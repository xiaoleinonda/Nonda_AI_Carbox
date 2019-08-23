package us.nonda.mqttlibrary.mqtt

import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import com.google.protobuf.Any
import io.nonda.onedata.proto.contract.CloudDriveMqttMessageCreator
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.DeviceUtils
import us.nonda.mqttlibrary.model.*

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
class MqttManager : MqttCallback, IMqttActionListener {

    private val TAG = MqttManager::class.java.simpleName
    private var SERVER_HOST = "tcp://mqtt-qa.zus.ai:1883"
    private var CLIENT_ID = "Android${DeviceUtils.getIMEICode(AppUtils.context)}"
    var mqttState = 0
    private val MQTTSTATE_CONNECTIONLOST = 0
    private val MQTTSTATE_MESSAGEARRIVED = 1
    private val MQTTSTATE_DELIVERYCOMPLETE = 2
    private val mqttConnectOptions = MqttConnectOptions()

    private val PUBLISH_STATUS = 10001
    private val PUBLISH_GPS = 10003
    private val PUBLISH_GSENSOR = 10004
    private val PUBLISH_GYRO = 10005
    private val PUBLISH_FACE_RESULT = 10006
    private val PUBLISH_EMOTION = 10007

    public var isConnected = false

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var imei = DeviceUtils.getIMEICode(AppUtils.context)
        var PUBLISH_TOPIC = "nonda/drive/$imei/report"//车机上报云端：
        var RESPONSE_TOPIC = "nonda/drive/$imei/issue"//云端下发车机:
        var LAST_WILL_TOPIC = "nonda/drive/$imei/will"//车机Last Wii：
//        var mqttAndroidClient: MqttAndroidClient? = null
//        private var mMqttConnectOptions: MqttConnectOptions? = null

        @Volatile
        private var instance: MqttManager? = null

        fun getInstance(): MqttManager =
            instance ?: synchronized(this) {
                instance
                    ?: MqttManager().also { instance = it }
            }
    }


    private val mqttAndroidClient = MqttAndroidClient(
        AppUtils.context,
        SERVER_HOST,
        CLIENT_ID
    )


    fun init() {
        mqttAndroidClient.setCallback(this)
        mqttConnectOptions.isCleanSession = false
        // 设置超时时间，单位：秒
        mqttConnectOptions.connectionTimeout = 15
        // 心跳包发送间隔，单位：秒
        mqttConnectOptions.keepAliveInterval = 180
        mqttConnectOptions.isAutomaticReconnect = true

        // last will message
        val message = imei
        val topic = LAST_WILL_TOPIC
        val qos = 1
        val retained = false
        if (message != "" || topic != "") {
            // 最后的遗嘱
            try {
                mqttConnectOptions.setWill(topic, message.toByteArray(), qos, retained)
            } catch (e: Exception) {
                Log.i(TAG, "Exception Occured", e)
                onFailure(null, e)
            }
        }
    }

    @Synchronized
    fun onStart() {
        if (!mqttAndroidClient.isConnected) {
            try {
                mqttAndroidClient.connect(mqttConnectOptions, null, this)
                Log.d(TAG, "mqtt建立连接")
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }

    fun onStop() {
        isConnected = false
        if (mqttAndroidClient.isConnected) {
            mqttAndroidClient.disconnect()
        }

    }

    /**
     * 发布 （模拟其他客户端发布消息）
     *
     * @param message 消息
     */
//    fun publish(message: String) {
//        val topic = PUBLISH_TOPIC
//        val qos = 1
//        val retained = false
//        try {
//            //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
//            mqttAndroidClient.publish(topic, message.toByteArray(), qos, retained)
//        } catch (e: MqttException) {
//            e.printStackTrace()
//        }
//    }
    private fun publish(builderMessage: CloudDriveMqttMessageCreator.CloudDriveMqttMessage.Builder, cmd: Int) {
        builderMessage.time = System.currentTimeMillis()
        builderMessage.cmd = cmd

        val byteArray = builderMessage.build().toByteArray()
        val mqttMessage = MqttMessage()
        mqttMessage.payload = byteArray

        mqttAndroidClient.publish(PUBLISH_TOPIC, mqttMessage)
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
            mqttAndroidClient.publish(topic, message.toByteArray(), qos, retained)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    override fun messageArrived(topic: String?, message: MqttMessage?) {
        Log.d(TAG, "messageArrived")
        mqttState = MQTTSTATE_MESSAGEARRIVED
    }


    override fun connectionLost(cause: Throwable?) {
        Log.d(TAG, "connectionLost")
        isConnected = false
        mqttState = MQTTSTATE_CONNECTIONLOST
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        Log.d(TAG, "deliveryComplete")
        mqttState = MQTTSTATE_DELIVERYCOMPLETE
    }

    override fun onSuccess(asyncActionToken: IMqttToken?) {
        Log.d(TAG, "onSuccess")
        isConnected = true
        try {
            mqttAndroidClient.subscribe(PUBLISH_TOPIC, 1)//订阅主题，参数：主题、服务质量
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
        isConnected = false
        Log.d(TAG, "onFailure：${exception?.message}")
    }


    /**
     * 上报状态
     */
    fun publishStatus(statusBean: StatusBean) {
        val builderData = CloudDriveMqttMessageCreator.CloudDriveMqttBoxStatusData.newBuilder()

        builderData.fw = statusBean.fw
        builderData.app = statusBean.app
        builderData.lat = statusBean.lat
        builderData.lng = statusBean.lng
        builderData.acc = statusBean.acc!!
        builderData.vol = statusBean.vol!!

        val builderMessage = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.newBuilder()
        builderMessage.data = Any.pack(builderData.build())

        publish(builderMessage, PUBLISH_STATUS)
    }

    /**
     * 上报GPS
     */
    fun publishGPS(GPSBeans: List<GPSBean>) {
        val builderItem = CloudDriveMqttMessageCreator.CloudDriveMqttGpsDataItem.newBuilder()
        for (i in 0..GPSBeans.size) {
            val GPSBean = GPSBeans[i]
            GPSBean.run {
                builderItem.lat = this.lat!!
                builderItem.lng = this.lng!!
                builderItem.spd = this.spd!!
                builderItem.acc = this.acc!!
                builderItem.brg = this.brg!!
                builderItem.time = System.currentTimeMillis()
            }
        }
        val builderData = CloudDriveMqttMessageCreator.CloudDriveMqttGpsData.newBuilder()
        builderData.addItems(builderItem)
        val builderMessage = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.newBuilder()
        builderMessage.data = Any.pack(builderData.build())

        publish(builderMessage, PUBLISH_GPS)
    }

    /**
     * 上报G-Sensor
     */
    fun publishGSensor(gSensorBeans: List<GSensorBean>) {
        val builderItem = CloudDriveMqttMessageCreator.CloudDriveMqttGSensorDataItem.newBuilder()
        for (i in 0..gSensorBeans.size) {
            val gSensorBean = gSensorBeans[i]
            gSensorBean.run {
                builderItem.x = this.x!!
                builderItem.y = this.y!!
                builderItem.z = this.z!!
                builderItem.time = this.time!!
            }
        }
        val builderData = CloudDriveMqttMessageCreator.CloudDriveMqttGSensorData.newBuilder()
        builderData.addItems(builderItem)
        val builderMessage = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.newBuilder()
        builderMessage.data = Any.pack(builderData.build())

        publish(builderMessage, PUBLISH_GSENSOR)
    }

    /**
     * 上报Gyro
     */
    fun publishGyro(gyroBeans: List<GyroBean>) {
        val builderItem = CloudDriveMqttMessageCreator.CloudDriveMqttGyroDataItem.newBuilder()
        for (i in 0..gyroBeans.size) {
            val gyroBean = gyroBeans[i]
            gyroBean.run {
                builderItem.x = this.x!!
                builderItem.y = this.y!!
                builderItem.z = this.z!!
                builderItem.time = this.time!!
            }
        }
        val builderData = CloudDriveMqttMessageCreator.CloudDriveMqttGyroData.newBuilder()
        builderData.addItems(builderItem)
        val builderMessage = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.newBuilder()
        builderMessage.data = Any.pack(builderData.build())

        publish(builderMessage, PUBLISH_GYRO)
    }

    /**
     * 上报人脸比对结果
     */
    fun publishFaceResult(faceResultBeans: List<FaceResultBean>) {
        val builderItem = CloudDriveMqttMessageCreator.CloudDriveMqttFaceDataItem.newBuilder()
        for (i in 0..faceResultBeans.size) {
            val gyroBean = faceResultBeans[i]
            gyroBean.run {
                builderItem.face = this.face!!
                builderItem.time = this.time!!
            }
        }
        val builderData = CloudDriveMqttMessageCreator.CloudDriveMqttFaceData.newBuilder()
        builderData.addItems(builderItem)
        val builderMessage = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.newBuilder()
        builderMessage.data = Any.pack(builderData.build())

        publish(builderMessage, PUBLISH_FACE_RESULT)
    }

    /**
     * 上报情绪识别结果
     */
    fun publishEmotion(emotionBeans: List<EmotionBean>) {
        val builderItem = CloudDriveMqttMessageCreator.CloudDriveMqttEmotionDataItem.newBuilder()
        for (i in 0..emotionBeans.size) {
            val emotionBean = emotionBeans[i]
            emotionBean.run {
                builderItem.emotion = this.emotion!!
                builderItem.time = this.time!!
            }
        }
        val builderData = CloudDriveMqttMessageCreator.CloudDriveMqttEmotionData.newBuilder()
        builderData.addItems(builderItem)
        val builderMessage = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.newBuilder()
        builderMessage.data = Any.pack(builderData.build())

        publish(builderMessage, PUBLISH_EMOTION)
    }

}


