//package us.nonda.mqttlibrary
//
//import android.util.Log
//import org.eclipse.paho.android.service.MqttAndroidClient
//import org.eclipse.paho.client.mqttv3.*
//
//
///**
// * Created by chenjun on 2019-05-27.
// */
//class PublishMqttManager : MqttCallback, IMqttActionListener {
//    var userId: String? = null
//
//    companion object {
//        private const val TAG = "PublishMqttManager"
//        private const val SERVER_HOST = "tcp://mqtt-qa.zus.ai:1883"
//        private val CLIENT_ID = "Android${System.currentTimeMillis()}"
//        private const val SERVER_TOPIC = "nonda/drive/device/issue/demodevice"
//        private const val CLIENT_TOPIC = "nonda/drive/device/report/demodevice"
//        //private const val USER_NAME = ""
//        //private const val PASS_WORD = ""
//
//        @Volatile
//        private var instance: PublishMqttManager? = null
//
//        fun getInstance(): PublishMqttManager =
//            instance ?: synchronized(this) {
//                instance
//                    ?: PublishMqttManager().also { instance = it }
//            }
//    }
//
//    private val mqttPublishAndroidClient = MqttAndroidClient(
//        ZCRApp.INSTANCE,
//        SERVER_HOST,
//        CLIENT_ID
//    )
//    private val mqttConnectOptions = MqttConnectOptions()
//
//    init {
//        mqttPublishAndroidClient.setCallback(this)
//        mqttConnectOptions.isCleanSession = true
//        // 设置超时时间，单位：秒
//        mqttConnectOptions.connectionTimeout = 15
//        // 心跳包发送间隔，单位：秒
//        mqttConnectOptions.keepAliveInterval = 30
//        mqttConnectOptions.isAutomaticReconnect = true
//        // 用户名
//        //mqttConnectOptions.userName = USER_NAME
//        // 密码
//        //mqttConnectOptions.password = PASS_WORD.toCharArray()
//    }
//
//    fun onStart() {
//        if (!mqttPublishAndroidClient.isConnected) {
//            try {
//                mqttPublishAndroidClient.connect(mqttConnectOptions, null, this)
//            } catch (e: MqttException) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//    fun onStop() {
//        if (mqttPublishAndroidClient.isConnected) {
//            mqttPublishAndroidClient.disconnect()
//        }
//    }
//
//    fun publicMotion(emotion: String) {
//        val emotionResult = EmotionResult(userId ?: "", emotion)
//        val netResult = NetResult(10021, System.currentTimeMillis(), emotionResult)
//        val json = GsonUtil.COMMON_GSON.gson.toJson(netResult)
//        publish(json)
//    }
//
//    fun publishPassResult(result: Boolean) {
//        val passResult = PassResult(userId ?: "", result)
//        val netResult = NetResult(10011, System.currentTimeMillis(), passResult)
//        val json = GsonUtil.COMMON_GSON.gson.toJson(netResult)
//        publish(json)
//    }
//
//    private fun publish(msg: String) {
//        val mqttMessage = MqttMessage()
//        mqttMessage.payload = msg.toByteArray()
//        mqttPublishAndroidClient.publish(CLIENT_TOPIC, mqttMessage)
//    }
//
//    override fun messageArrived(topic: String?, message: MqttMessage?) {
//
//    }
//
//    override fun connectionLost(cause: Throwable?) {
//        Log.d(TAG, "connectionLost")
//    }
//
//    override fun deliveryComplete(token: IMqttDeliveryToken?) {
//    }
//
//    override fun onSuccess(asyncActionToken: IMqttToken?) {
//        Log.d(TAG, "连接成功")
//    }
//
//    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
//        Log.d(TAG, "连接失败：${exception?.message}")
//    }
//}
//
//
//
