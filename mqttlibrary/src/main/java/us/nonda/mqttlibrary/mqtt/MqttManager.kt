package us.nonda.mqttlibrary.mqtt

import android.annotation.SuppressLint
import android.util.Log
import com.google.protobuf.Any
import io.nonda.onedata.proto.contract.CloudDriveMqttMessageCreator
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.location.LocationUtils
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.DeviceUtils
import us.nonda.commonibrary.utils.FileUtils
import us.nonda.commonibrary.utils.PathUtils
import us.nonda.mqttlibrary.model.*
import us.nonda.mqttlibrary.model.Constant.Companion.PUBLISH_EMOTION
import us.nonda.mqttlibrary.model.Constant.Companion.PUBLISH_EVENT
import us.nonda.mqttlibrary.model.Constant.Companion.PUBLISH_FACE_RESULT
import us.nonda.mqttlibrary.model.Constant.Companion.PUBLISH_GPS
import us.nonda.mqttlibrary.model.Constant.Companion.PUBLISH_GYRO
import us.nonda.mqttlibrary.model.Constant.Companion.PUBLISH_STATUS
import java.util.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import us.nonda.mqttlibrary.BuildConfig


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
class MqttManager : MqttCallback, IMqttActionListener, MqttCallbackExtended {

    private val TAG = MqttManager::class.java.simpleName
    private var SERVER_HOST = BuildConfig.MQTT_URL
    //    private var CLIENT_ID = "Android${DeviceUtils.getIMEICode(AppUtils.context)}"
    private var CLIENT_ID = "nonda-vehiclebox-${DeviceUtils.getIMEICode(AppUtils.context)}"
    var mqttState = 0
    private val MQTTSTATE_CONNECTIONLOST = 0
    private val MQTTSTATE_MESSAGEARRIVED = 1
    private val MQTTSTATE_DELIVERYCOMPLETE = 2
    private val mqttConnectOptions = MqttConnectOptions()

    public var isConnected = false
    public var connectSuccessed = false
    private val messageQueue = LinkedList<MqttMessage>()
    private var isPublishLocalMessage = false

    private var failcount = 0

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var imei = DeviceUtils.getIMEICode(AppUtils.context)
        var PUBLISH_TOPIC = "nonda/drive/$imei/report"//车机上报云端：
        var RESPONSE_TOPIC = "nonda/drive/$imei/issue"//云端下发车机:
        var LAST_WILL_TOPIC = "nonda/drive/$imei/will"//车机Last Wii：
        //        var mqttAndroidClient: MqttAndroidClient? = null
//        private var mMqttConnectOptions: MqttConnectOptions? = null
        var persistence = MemoryPersistence()
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
        CLIENT_ID,
        persistence
    )


    init {
        mqttAndroidClient.setCallback(this)
        mqttConnectOptions.isCleanSession = false
        // 设置超时时间，单位：秒
        mqttConnectOptions.connectionTimeout = 15
        // 心跳包发送间隔，单位：秒
        mqttConnectOptions.keepAliveInterval = 60
        mqttConnectOptions.isAutomaticReconnect = true
        mqttConnectOptions.userName = BuildConfig.MQTT_NAME
        mqttConnectOptions.password = BuildConfig.MQTT_PASSWORD.toCharArray()


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
        } else {
            Log.d(TAG, "网络重新连接" + mqttAndroidClient.isConnected)
        }
    }

    fun onStop() {
        isConnected = false
        if (mqttAndroidClient.isConnected) {
            mqttAndroidClient.disconnect()
        }
        Log.d(TAG, "网络断开" + mqttAndroidClient.isConnected)

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
            mqttAndroidClient.publish(topic, message.toByteArray(), qos, retained)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun publish(builderMessage: CloudDriveMqttMessageCreator.CloudDriveMqttMessage.Builder, cmd: Int) {
        builderMessage.time = System.currentTimeMillis()
        builderMessage.cmd = cmd

        val byteArray = builderMessage.build().toByteArray()
        val mqttMessage = MqttMessage()
        mqttMessage.payload = byteArray

        //如果还没有初始化，存在本地，连接成功之后上报
        if (!connectSuccessed) {
            failcount++
            MyLog.d(TAG, "还没初始化存到本地" + failcount + "条消息")
            messageQueue.offer(mqttMessage)
        }
        publish(mqttMessage)
    }

    private fun publish(mqttMessage: MqttMessage) {
        try {
            if (mqttAndroidClient.isConnected) {
                mqttAndroidClient.publish(PUBLISH_TOPIC, mqttMessage)
            } else {
                messageQueue.offer(mqttMessage)
                failcount++
                MyLog.d(TAG, "存到本地" + failcount + "条消息")
            }
            MyLog.d(TAG, "mqttAndroidClient=${mqttAndroidClient.isConnected}")
        } catch (e: Exception) {
            MyLog.d(TAG, "发送失败" + mqttAndroidClient.isConnected)
        }
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

        var cloudDriveMqttMessage: CloudDriveMqttMessageCreator.CloudDriveMqttMessage? = null

        Log.d(TAG, "messageArrived")
        mqttState = MQTTSTATE_MESSAGEARRIVED

        //过滤掉topic为publish的回调
        if (PUBLISH_TOPIC == topic) return

        MyLog.d(TAG, "messageArrived" + message)

        if (message != null) {
            cloudDriveMqttMessage = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.parseFrom(message.payload)
        }

        if (cloudDriveMqttMessage == null) {
            return
        }
        MqttManager.getInstance().publishEventData(1015, cloudDriveMqttMessage.cmd.toString())

        val mqttMessageHandler = MqttHandlerFactory.getHandlerByCMD(cloudDriveMqttMessage.cmd)
        mqttMessageHandler.handleMqttMessage(cloudDriveMqttMessage)
    }


    override fun connectionLost(cause: Throwable?) {
        //停止上传本地数据
        isPublishLocalMessage = false
        Log.d(TAG, "connectionLost")
        isConnected = false
        mqttState = MQTTSTATE_CONNECTIONLOST
    }

    /**
     * 消息发送成功接收到的回调
     */
    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        MyLog.d(TAG, "deliveryComplete")
        mqttState = MQTTSTATE_DELIVERYCOMPLETE
    }

    /**
     * mqtt初始化成功
     */
    override fun onSuccess(asyncActionToken: IMqttToken?) {
        isConnected = true
        try {
            mqttAndroidClient.subscribe(RESPONSE_TOPIC, 1)//订阅主题，参数：主题、服务质量
            Log.d(TAG, "onSuccess发送成功")
            connectSuccessed = true

            MqttManager.getInstance().publishEventData(1014, "1")

        } catch (e: MqttException) {
            MqttManager.getInstance().publishEventData(1014, "2")

            e.printStackTrace()
            Log.d(TAG, "onSuccess：${e?.message}" + mqttAndroidClient.isConnected)
        }
    }

    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
        isConnected = false
        Log.d(TAG, "onFailure：${exception?.message}+mqttAndroidClient.isConnected")
    }

    /**
     * 链接和重连时都会走到的回调，初始化时晚于onSuccess
     */
    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
        Log.d(TAG, "connectComplete:重连成功")
        if (!isPublishLocalMessage) {
            publishLocalMessage()
        }
    }

    private fun publishLocalMessage() {
        //开始发送本地消息，改变布尔值防止再次网络状态改变进入循环
        isPublishLocalMessage = true
        //如果初始化连接成功,发送初始化之前缓存的消息
        if (messageQueue.size > 0) {
            val runnable = Runnable {
                var mqttMessage = messageQueue.poll()
                while (mqttMessage != null) {
                    try {
                        publish(mqttMessage)
                        failcount--
                        MyLog.d(TAG, "本地剩余" + failcount + "条消息")
                        MyLog.d(TAG, "缓存数据发送成功=${mqttAndroidClient.isConnected}")
                        mqttMessage = messageQueue.poll()

                        //相隔一定时间发送一次，防止短时间发送过多收不到消息的回调
                        Thread.sleep(500)
                    } catch (e: Exception) {
                        MyLog.d(TAG, "发送失败" + mqttAndroidClient.isConnected)
                    }
                }
                MyLog.d(TAG, "补发结束" + mqttAndroidClient.isConnected)
                //发送结束
                isPublishLocalMessage = false
            }
            Thread(runnable).start()
        }
    }

    /**
     * 上报状态
     */
    fun publishSleepStatus(statusBean: StatusBean) {
        val builderData = CloudDriveMqttMessageCreator.CloudDriveMqttBoxStatusData.newBuilder()

        builderData.fw = statusBean.fw
        builderData.app = statusBean.app
        builderData.lat = statusBean.lat
        builderData.lng = statusBean.lng
        builderData.acc = statusBean.acc ?: 0f
        builderData.vol = statusBean.vol ?: 0f
        builderData.sim = DeviceUtils.getSimNumber(AppUtils.context)

        val builderMessage = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.newBuilder()
        builderMessage.data = Any.pack(builderData.build())

        publish(builderMessage, PUBLISH_STATUS)
        MyLog.i(TAG, "上报状态")
    }

    /**
     * 上报状态
     */
    fun publishStatus() {
        val builderData = CloudDriveMqttMessageCreator.CloudDriveMqttBoxStatusData.newBuilder()
        val bestLocation = LocationUtils.getBestLocation(AppUtils.context, null)
        val latitude = bestLocation?.latitude
        val longitude = bestLocation?.longitude
        val accuracy = bestLocation?.accuracy

        builderData.fw = "1.2"
        builderData.app = AppUtils.getVersionName(AppUtils.context)
        builderData.sim = DeviceUtils.getSimNumber(AppUtils.context)
        builderData.lat = latitude ?: 0.0
        builderData.lng = longitude ?: 0.0
        builderData.acc = accuracy ?: 0.0f
        builderData.vol = DeviceUtils.getCarBatteryInfo().toFloat()

        val builderMessage = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.newBuilder()
        builderMessage.data = Any.pack(builderData.build())

        publish(builderMessage, PUBLISH_STATUS)
        MyLog.i(TAG, "立即上报状态")
    }

    /**
     * 上报事件
     */
    fun publishEventData(type: Int, content: String) {
        val builderData = CloudDriveMqttMessageCreator.CloudDriveMqttEventData.newBuilder()

        val bestLocation = LocationUtils.getBestLocation(AppUtils.context, null)
        val latitude = bestLocation?.latitude
        val longitude = bestLocation?.longitude
        val accuracy = bestLocation?.accuracy

        builderData.fw = "1.2"
        builderData.app = AppUtils.getVersionName(AppUtils.context)
        builderData.sim = DeviceUtils.getSimNumber(AppUtils.context)
        builderData.lat = latitude ?: 0.0
        builderData.lng = longitude ?: 0.0
        builderData.acc = accuracy ?: 0.0f
        builderData.vol = DeviceUtils.getCarBatteryInfo().toFloat()
        builderData.type = type
        builderData.content = content

        val builderMessage = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.newBuilder()
        builderMessage.data = Any.pack(builderData.build())

        publish(builderMessage, PUBLISH_EVENT)
        MyLog.i(TAG, "上报事件")
    }

    /**
     * 上报GPS
     */
    fun publishGPS(GPSBeans: List<GPSBean>) {
        val builderItem = CloudDriveMqttMessageCreator.CloudDriveMqttGpsDataItem.newBuilder()
        val builderData = CloudDriveMqttMessageCreator.CloudDriveMqttGpsData.newBuilder()

        for (gpsBean in GPSBeans) {
            gpsBean?.run {
                builderItem.lat = this.lat!!
                builderItem.lng = this.lng!!
                builderItem.spd = this.spd!!
                builderItem.acc = this.acc!!
                builderItem.brg = this.brg!!
                builderItem.time = System.currentTimeMillis()
                builderData.addItems(builderItem)
            }
        }

        val builderMessage = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.newBuilder()
        builderMessage.data = Any.pack(builderData.build())

        publish(builderMessage, PUBLISH_GPS)
        MyLog.i(TAG, "上报GPS=${GPSBeans.size}")

    }

    /**
     * 上报G-Sensor
     */
    fun publishGSensor(gSensorBeans: List<GSensorBean>) {
        val builderItem = CloudDriveMqttMessageCreator.CloudDriveMqttGSensorDataItem.newBuilder()
        val builderData = CloudDriveMqttMessageCreator.CloudDriveMqttGSensorData.newBuilder()

        for (gSensorBean in gSensorBeans) {
            gSensorBean?.run {
                builderItem.x = this.x!!
                builderItem.y = this.y!!
                builderItem.z = this.z!!
                builderItem.time = this.time!!
                builderData.addItems(builderItem)
            }
        }

        val builderMessage = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.newBuilder()
        builderMessage.data = Any.pack(builderData.build())

        publish(builderMessage, Constant.PUBLISH_GSENSOR)
        MyLog.i(TAG, "上报G-Sensor=${gSensorBeans.size}")
    }

    /**
     * 上报Gyro
     */
    fun publishGyro(gyroBeans: List<GyroBean>) {
        val builderItem = CloudDriveMqttMessageCreator.CloudDriveMqttGyroDataItem.newBuilder()
        val builderData = CloudDriveMqttMessageCreator.CloudDriveMqttGyroData.newBuilder()

        for (gyroBean in gyroBeans) {
            gyroBean?.run {
                builderItem.x = this.x!!
                builderItem.y = this.y!!
                builderItem.z = this.z!!
                builderItem.time = this.time!!
                builderData.addItems(builderItem)
            }
        }

        val builderMessage = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.newBuilder()
        builderMessage.data = Any.pack(builderData.build())

        publish(builderMessage, PUBLISH_GYRO)
        MyLog.i(TAG, "上报Gyro=${gyroBeans.size}")

    }

    /**
     * 上报人脸比对结果
     */
    fun publishFaceResult(faceResultBeans: List<FaceResultBean>) {
        val builderItem = CloudDriveMqttMessageCreator.CloudDriveMqttFaceDataItem.newBuilder()
        val builderData = CloudDriveMqttMessageCreator.CloudDriveMqttFaceData.newBuilder()

        for (faceResultBean in faceResultBeans) {
            faceResultBean?.run {
                builderItem.face = this.face!!
                builderItem.time = this.time!!
                builderData.addItems(builderItem)
            }
        }

        val builderMessage = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.newBuilder()
        builderMessage.data = Any.pack(builderData.build())

        publish(builderMessage, PUBLISH_FACE_RESULT)
        MyLog.i(TAG, "上报人脸比对结果=${faceResultBeans.size}")

    }

    /**
     * 上报情绪识别结果
     */
    fun publishEmotion(emotionBeans: List<EmotionBean>) {
        MyLog.d(TAG, "开始上报情绪${emotionBeans.size}")
        val builderItem = CloudDriveMqttMessageCreator.CloudDriveMqttEmotionDataItem.newBuilder()
        val builderData = CloudDriveMqttMessageCreator.CloudDriveMqttEmotionData.newBuilder()

        for (emotionBean in emotionBeans) {
            emotionBean?.run {
                builderItem.emotion = this.emotion!!
                builderItem.time = this.time!!
                builderData.addItems(builderItem)
            }
        }

        val builderMessage = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.newBuilder()
        builderMessage.data = Any.pack(builderData.build())

        publish(builderMessage, PUBLISH_EMOTION)
        MyLog.i(TAG, "上报情绪识别结果=${emotionBeans.size}")

    }
}


