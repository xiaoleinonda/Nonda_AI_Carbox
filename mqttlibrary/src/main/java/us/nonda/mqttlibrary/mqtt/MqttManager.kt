//package us.nonda.mqttlibrary.manager
//
//import android.util.Log
//import org.eclipse.paho.android.service.MqttAndroidClient
//import org.eclipse.paho.client.mqttv3.*
//import us.nonda.commonibrary.utils.AppUtils
//import java.lang.Exception
//
//class MqttManager : MqttCallback, IMqttActionListener {
//    private var userId: String? = null
//    public var isConnected = false
//    private val mqttConnectOptions = MqttConnectOptions()
//
//    companion object {
//        private const val TAG = "MqttManager"
//        private const val SERVER_HOST = "tcp://mqtt-qa.zus.ai:1883"
//        private val CLIENT_ID = "Android${System.currentTimeMillis()}"
////        private val CLIENT_ID_1 = "Android_1${System.currentTimeMillis()}"
//        //车机上报云端：
//        private const val SERVER_TOPIC = "nonda/drive/<imei>/report"
//        //云端下发车机:
//        private const val CLIENT_TOPIC = "nonda/drive/<imei>/issue"
////        //车机Last Wii：
////        private const val CLIENT_TOPIC = "nonda/drive/<imei>/will"
//        //private const val USER_NAME = ""
//        //private const val PASS_WORD = ""
//
//        @Volatile
//        private var instance: MqttManager? = null
//
//        fun getInstance(): MqttManager =
//            instance ?: synchronized(this) {
//                instance
//                    ?: MqttManager().also { instance = it }
//            }
//    }
//
//    private val mqttAndroidClient = MqttAndroidClient(
//        AppUtils.context,
//        SERVER_HOST,
//        CLIENT_ID
//    )
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
////        if (!mqttAndroidClientReceiver.isConnected) {
////            try {
////                mqttAndroidClientReceiver.connect(mqttConnectOptions, null, object : IMqttActionListener {
////                    override fun onSuccess(asyncActionToken: IMqttToken?) {
////                        mqttAndroidClientReceiver.subscribe(SERVER_TOPIC, 1)
////                        isConnected = true
//////                        EventBus.getDefault().post(MqttStatusEvent(true, "Receiver onSuccess"))
////
////                    }
////
////                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
////                        isConnected = false
////                    }
////
////                })
////                Log.d(TAG, "mqtt建立连接")
////            } catch (e: MqttException) {
////                e.printStackTrace()
////            }
////        }
//    }
//
//    fun onStop() {
//        isConnected = false
//
//        if (mqttAndroidClient.isConnected) {
//            mqttAndroidClient.disconnect()
//            Log.d(TAG, "mqtt断开连接")
//        }
//    }
//
//
//
//    override fun messageArrived(topic: String?, message: MqttMessage?) {
//        Log.d(TAG, "mqtt messageArrived message=${message.toString()}")
////        publicLogMessage(LogMessage(TYPE_MESSAGE_ARRIVED, "message arrived"))
//        if (message == null) return
//
//        try {
//            if (mqttAndroidClient != null) {
////                userId = mqttMessageHandler.dispatchMessage(message.payload)
//            }
//        } catch (e: Exception) {
//
//        }
//    }
//
//    override fun connectionLost(cause: Throwable?) {
//        Log.d(TAG, "connectionLost")
//        isConnected = false
//    }
//
//    override fun deliveryComplete(token: IMqttDeliveryToken?) {
//    }
//
//    override fun onSuccess(asyncActionToken: IMqttToken?) {
//        Log.d(TAG, "连接成功")
//        isConnected = true
//    }
//
//    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
//        isConnected = false
//        Log.d(TAG, "连接失败：${exception?.message}")
//    }
//}