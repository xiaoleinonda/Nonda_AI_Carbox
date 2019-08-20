package us.nonda.mqttlibrary.mqtt

import android.annotation.SuppressLint
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Context
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import android.content.Intent
import android.net.ConnectivityManager
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.android.service.MqttAndroidClient
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.Nullable
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.DeviceUtils

/**
 *MQTT客户端连接参数
QOS: 1
Retained: false
AutomaticReconnect: true
CleanSession: false
KeepAliveInterval: 3min
UserName: <username>
Password: <password>
Will: topic - nonda/drive/<imei>/will, payload - <imei>, qos - 1, retained - false
 */
class MqttService : Service() {
    private val TAG = MqttService::class.java.simpleName
    private var mMqttConnectOptions: MqttConnectOptions? = null
    private var SERVER_HOST = "tcp://mqtt-qa.zus.ai:1883"
    //    var USERNAME = "admin"//用户名
    //    var PASSWORD = "password"//密码
    private var CLIENTID = "Android${System.currentTimeMillis()}"
    public var mqttState = ""

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var mqttAndroidClient: MqttAndroidClient? = null
        private var imei = DeviceUtils.getIMEICode(AppUtils.context)
        var PUBLISH_TOPIC = "nonda/drive/$imei/report"//车机上报云端：
        var RESPONSE_TOPIC = "nonda/drive/$imei/issue"//云端下发车机:
        var LAST_WILL_TOPIC = "nonda/drive/$imei/will"//车机Last Wii：

        /**
         * 开启服务
         */
        fun startService(mContext: Context) {
            mContext.startService(Intent(mContext, MqttService::class.java))
            Log.i(TAG, "开启服务")
        }

        /**
         * 发布 （模拟其他客户端发布消息）
         *
         * @param message 消息
         */
        fun publish(message: String) {
            val topic = PUBLISH_TOPIC
            val qos = 1
            val retained = false
            try {
                //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
                mqttAndroidClient!!.publish(topic, message.toByteArray(), qos, retained)
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }

        @Volatile
        private var instance: MqttService? = null

        fun getInstance(): MqttService =
            instance ?: synchronized(this) {
                instance ?: MqttService().also { instance = it }
            }
    }

    /**
     * 判断网络是否连接
     */
    private/*没有可用网络的时候，延迟3秒再尝试重连*/ val isConnectIsNomarl: Boolean
        get() {
            val connectivityManager =
                AppUtils.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val info = connectivityManager.getActiveNetworkInfo()
            if (info != null && info.isAvailable) {
                val name = info.typeName
                Log.i(TAG, "当前网络名称：$name")
                return true
            } else {
                Log.i(TAG, "没有可用网络")
                Handler().postDelayed(Runnable { doClientConnection() }, 3000)
                return false
            }
        }

    //MQTT是否连接成功的监听
    private val iMqttActionListener = object : IMqttActionListener {
        override fun onSuccess(arg0: IMqttToken) {
            Log.i(TAG, "连接成功 ")
            try {
                mqttAndroidClient!!.subscribe(PUBLISH_TOPIC, 1)//订阅主题，参数：主题、服务质量
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }

        override fun onFailure(arg0: IMqttToken?, arg1: Throwable) {
            arg1.printStackTrace()
            Log.i(TAG, "连接失败 ")
            doClientConnection()//连接失败，重连（可关闭服务器进行模拟）
        }
    }

    //订阅主题的回调
    private val mqttCallback = object : MqttCallback {

        @Throws(Exception::class)
        override fun messageArrived(topic: String, message: MqttMessage) {
            Log.i(TAG, "收到消息： " + String(message.payload))
            //收到消息，这里弹出Toast表示。如果需要更新UI，可以使用广播或者EventBus进行发送
            Toast.makeText(AppUtils.context, "messageArrived: " + String(message.payload), Toast.LENGTH_LONG).show()
            //收到其他客户端的消息后，响应给对方告知消息已到达或者消息有问题等
            response("message arrived")
            mqttState = "messageArrived"
        }

        override fun deliveryComplete(arg0: IMqttDeliveryToken) {
            mqttState = "deliveryComplete"
            Toast.makeText(AppUtils.context, "deliveryComplete ", Toast.LENGTH_LONG).show()
        }

        override fun connectionLost(arg0: Throwable) {
            Log.i(TAG, "连接断开 ")
            doClientConnection()//连接断开，重连
            mqttState = "connectionLost"
            Toast.makeText(AppUtils.context, "connectionLost: ", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        init()
        return super.onStartCommand(intent, flags, startId)
    }

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /**
     * 响应 （收到其他客户端的消息后，响应给对方告知消息已到达或者消息有问题等）
     *
     * @param message 消息
     */
    fun response(message: String) {
        val topic = RESPONSE_TOPIC
        val qos = 1
        val retained = false
        try {
            //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
            mqttAndroidClient!!.publish(topic, message.toByteArray(), qos, retained)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    /**
     * 初始化
     */
    private fun init() {
        val serverURI = SERVER_HOST //服务器地址（协议+地址+端口号）
        mqttAndroidClient = MqttAndroidClient(this, serverURI, CLIENTID)
        mqttAndroidClient!!.setCallback(mqttCallback) //设置监听订阅消息的回调
        mMqttConnectOptions = MqttConnectOptions()
        mMqttConnectOptions!!.isCleanSession = false //设置是否清除缓存
        mMqttConnectOptions!!.connectionTimeout = 15 //设置超时时间，单位：秒
        mMqttConnectOptions!!.keepAliveInterval = 180 //设置心跳包发送间隔，单位：秒
        mMqttConnectOptions!!.isAutomaticReconnect = true
//        mMqttConnectOptions!!.userName = USERNAME //设置用户名
//        mMqttConnectOptions!!.password = PASSWORD.toCharArray() //设置密码

        // last will message
        var doConnect = true
        val message = imei
        val topic = LAST_WILL_TOPIC
        val qos = 1
        val retained = false
        if (message != "" || topic != "") {
            // 最后的遗嘱
            try {
                mMqttConnectOptions!!.setWill(topic, message.toByteArray(), qos, retained)
            } catch (e: Exception) {
                Log.i(TAG, "Exception Occured", e)
                doConnect = false
                iMqttActionListener.onFailure(null, e)
            }
        }
        if (doConnect) {
            doClientConnection()
        }
    }

    /**
     * 连接MQTT服务器
     */
    @Synchronized
    private fun doClientConnection() {
        if (!mqttAndroidClient!!.isConnected && isConnectIsNomarl) {
            try {
                mqttAndroidClient!!.connect(mMqttConnectOptions, null, iMqttActionListener)
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        try {
            mqttAndroidClient!!.disconnect() //断开连接
        } catch (e: MqttException) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
}