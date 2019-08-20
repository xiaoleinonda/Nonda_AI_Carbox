//package us.nonda.mqttlibrary
//
//import android.util.Log
//import io.reactivex.schedulers.Schedulers
//import org.eclipse.paho.android.service.MqttAndroidClient
//import org.eclipse.paho.client.mqttv3.*
//import us.nonda.mqttlibrary.manager.MqttManager
//import java.lang.Exception
//
//
///**
// * Created by chenjun on 2019-05-27.
// */
//class MqttManager1 : MqttCallback, IMqttActionListener {
//    private var userId: String? = null
//    private val mqttMessageHandler = MqttMessageHandler()
//
//    public var isConnected = false
//
//    companion object {
//        private const val TAG = "MqttManager"
//        private const val SERVER_HOST = "tcp://mqtt-qa.zus.ai:1883"
//        private val CLIENT_ID = "Android${System.currentTimeMillis()}"
//        private val CLIENT_ID_1 = "Android_1${System.currentTimeMillis()}"
//        private const val SERVER_TOPIC = "nonda/drive/device/issue/demodevice"
//
//        private const val CLIENT_TOPIC = "nonda/drive/device/report/demodevice"
//        //private const val USER_NAME = ""
//        //private const val PASS_WORD = ""
//
//        @Volatile
//        private var instance: MqttManager? = null
//
//        fun getInstance(): MqttManager =
//            instance ?: synchronized(this) {
//                instance ?: MqttManager().also { instance = it }
//            }
//    }
//
//    private val createWorker = Schedulers.computation().createWorker()
//
//
//    private val mqttAndroidClient = MqttAndroidClient(
//        ZCRApp.INSTANCE,
//        SERVER_HOST,
//        CLIENT_ID
//    )
//    private val mqttAndroidClientReceiver = MqttAndroidClient(
//        ZCRApp.INSTANCE,
//        SERVER_HOST,
//        CLIENT_ID_1
//    )
//    private val mqttConnectOptions = MqttConnectOptions()
//
//    init {
//        mqttAndroidClient.setCallback(this)
//        mqttConnectOptions.isCleanSession = false
//        // 设置超时时间，单位：秒
//        mqttConnectOptions.connectionTimeout = 15
//        // 心跳包发送间隔，单位：秒
//        mqttConnectOptions.keepAliveInterval = 300
//        mqttConnectOptions.isAutomaticReconnect = true
//        // 用户名
//        //mqttConnectOptions.userName = USER_NAME
//        // 密码
//        //mqttConnectOptions.password = PASS_WORD.toCharArray()
//
//        mqttAndroidClientReceiver.setCallback(object : MqttCallback {
//            override fun messageArrived(topic: String?, message: MqttMessage?) {
////                Log.d(MyLogTag.TAG_CLOUD, "mqtt messageArrived message=${message.toString()}")
//                publicLogMessage(LogMessage(TYPE_MESSAGE_ARRIVED, "message arrived"))
//                if (message == null) return
//                Log.d(TAG, message.toString())
//                /*  val content = String(message.payload)
//                  userId = mqttMessageHandler.dispatchMessage(content)*/
//
//                try {
//                    if (mqttAndroidClient != null) {
//                        userId = mqttMessageHandler.dispatchMessage(message.payload)
//                    }
//                } catch (e: Exception) {
//                }
//
//            }
//
//            override fun connectionLost(cause: Throwable?) {
//            }
//
//            override fun deliveryComplete(token: IMqttDeliveryToken?) {
//            }
//
//        })
//    }
//
//    @Synchronized
//    fun onStart() {
//        if (!mqttAndroidClient.isConnected) {
//            try {
//                mqttAndroidClient.connect(mqttConnectOptions, null, this)
////                Log.d(MyLogTag.TAG_CLOUD, "mqtt建立连接")
//            } catch (e: MqttException) {
//                e.printStackTrace()
//            }
//        }
//        if (!mqttAndroidClientReceiver.isConnected) {
//            try {
//                mqttAndroidClientReceiver.connect(mqttConnectOptions, null, object : IMqttActionListener {
//                    override fun onSuccess(asyncActionToken: IMqttToken?) {
//                        mqttAndroidClientReceiver.subscribe(SERVER_TOPIC, 1)
//                        isConnected = true
////                        EventBus.getDefault().post(MqttStatusEvent(true, "Receiver onSuccess"))
//
//                    }
//
//                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
//                        isConnected = false
////                        EventBus.getDefault().post(MqttStatusEvent(false, "Receiver onStop"))
//
//                    }
//
//                })
////                Log.d(MyLogTag.TAG_CLOUD, "mqtt建立连接")
//            } catch (e: MqttException) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//    fun onStop() {
//        isConnected = false
//
////        EventBus.getDefault().post(MqttStatusEvent(false, "onStop"))
//
//        if (mqttAndroidClient.isConnected) {
//            mqttAndroidClient.disconnect()
////            Log.d(MyLogTag.TAG_CLOUD, "mqtt断开连接")
//        }
//        if (mqttAndroidClientReceiver.isConnected) {
//            mqttAndroidClientReceiver.disconnect()
////            Log.d(MyLogTag.TAG_CLOUD, "mqtt断开连接")
//        }
//    }
//
//    fun publicLogMessage(logMessage: LogMessage) {
////        val netResult = NetResult(10041, System.currentTimeMillis(), logMessage)
////        val json = GsonUtil.COMMON_GSON.gson.toJson(netResult)
////        publish(json)
//        val newBuilderData = CloudDriveMqttMessageCreator.MqttCommonLogData.newBuilder()
//        newBuilderData.type = logMessage.type
//        newBuilderData.message = logMessage.message
//
//        val newBuilder = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.newBuilder()
//        newBuilder.cmd = 10041
//        newBuilder.time = System.currentTimeMillis()
//        newBuilder.data = Any.pack(newBuilderData.build())
//
//        val toByteArray = newBuilder.build().toByteArray()
//        val mqttMessage = MqttMessage()
//        mqttMessage.payload = toByteArray
//        try {
//            if (mqttAndroidClient != null) {
//                mqttAndroidClient.publish(CLIENT_TOPIC, mqttMessage)
////                Log.d(MyLogTag.TAG_CLOUD, "logMessageType=${logMessage.type}  publicLogMessage=${logMessage.message}")
//            }
//        } catch (e: Exception) {
//
//        }
//
//    }
//
//    fun publicMotion(emotion: String) {
////        val emotionResult = EmotionResult(userId ?: "", emotion)
////        val netResult = NetResult(10021, System.currentTimeMillis(), emotionResult)
////        val json = GsonUtil.COMMON_GSON.gson.toJson(netResult)
////        publish(json)
//        val newBuilderData = CloudDriveMqttMessageCreator.MqttEmotionReportData.newBuilder()
//        newBuilderData.emotion = emotion;
//        newBuilderData.userId = userId ?: "";
//
//        val newBuilder = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.newBuilder()
//        newBuilder.cmd = 10021
//        newBuilder.time = System.currentTimeMillis()
//        newBuilder.data = Any.pack(newBuilderData.build())
//
//        val toByteArray = newBuilder.build().toByteArray()
//        val mqttMessage = MqttMessage()
//        mqttMessage.payload = toByteArray
//        try {
//            if (mqttAndroidClient != null) {
//                mqttAndroidClient.publish(CLIENT_TOPIC, mqttMessage)
//                Log.d("mqtt", "情绪：${newBuilderData.emotion}")
//            }
//        } catch (e: Exception) {
//
//        }
//
//
//    }
//
//    fun publishPassResult(result: Boolean) {
////        val passResult = PassResult(userId ?: "", result)
////        val netResult = NetResult(10011, System.currentTimeMillis(), passResult)
////        val json = GsonUtil.COMMON_GSON.gson.toJson(netResult)
////        publish(json)
//
//        val newBuilderData = CloudDriveMqttMessageCreator.MqttFaceResultReportData.newBuilder();
//        newBuilderData.result = result
//        newBuilderData.userId = userId ?: "";
//
//        val newBuilder = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.newBuilder()
//        newBuilder.cmd = 10011
//        newBuilder.time = System.currentTimeMillis()
//        newBuilder.data = Any.pack(newBuilderData.build())
//
//        val toByteArray = newBuilder.build().toByteArray()
//        val mqttMessage = MqttMessage()
//        mqttMessage.payload = toByteArray
//        try {
////            Log.d(MyLogTag.TAG_CLOUD, "publishPassResult通过人脸验证")
//
//            mqttAndroidClient.publish(CLIENT_TOPIC, mqttMessage)
//        } catch (e: Exception) {
//
//        }
//    }
//
//    private fun publish(msg: String) {
////        Log.d(MyLogTag.TAG_CLOUD, "mqtt publish msg=$msg")
//        val mqttMessage = MqttMessage()
//        mqttMessage.payload = msg.toByteArray()
//        mqttAndroidClient.publish(CLIENT_TOPIC, mqttMessage)
//    }
//
//    override fun messageArrived(topic: String?, message: MqttMessage?) {
////        Log.d(MyLogTag.TAG_CLOUD, "mqtt messageArrived message=${message.toString()}")
//        publicLogMessage(LogMessage(TYPE_MESSAGE_ARRIVED, "message arrived"))
////        if (message == null) return
////        val d = Log.d(TAG, message.toString())
//        if (message == null) return
//
//        try {
//            if (mqttAndroidClient != null) {
//                userId = mqttMessageHandler.dispatchMessage(message.payload)
//            }
//        } catch (e: Exception) {
//
//        }
//    }
//
//    override fun connectionLost(cause: Throwable?) {
////        Log.d(MyLogTag.TAG_CLOUD, "mqtt connectionLost Throwable=${cause?.message}")
//        Log.d(TAG, "connectionLost")
//        isConnected = false
//
////        EventBus.getDefault().post(MqttStatusEvent(false, "connectionLost"))
//
//    }
//
//    override fun deliveryComplete(token: IMqttDeliveryToken?) {
//    }
//
//    override fun onSuccess(asyncActionToken: IMqttToken?) {
//        Log.d(TAG, "连接成功")
//        isConnected = true
////        EventBus.getDefault().post(MqttStatusEvent(true, "onSuccess"))
////        mqttAndroidClient.subscribe(SERVER_TOPIC, 1)
//    }
//
//    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
//        isConnected = false
////        EventBus.getDefault().post(MqttStatusEvent(false, "onFailure"))
//
//        Log.d(TAG, "连接失败：${exception?.message}")
//    }
//}
