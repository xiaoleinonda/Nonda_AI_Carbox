//package us.nonda.mqttlibrary
//
//import android.os.Handler
//import android.os.Looper
//import android.util.Base64
//import us.nonda.mqttlibrary.mqtt.MqttManager
//
///**
// * Created by chenjun on 2019-06-04.
// */
//class                                                                                                                                                               MqttMessageHandler {
//    private val passMessageHandler = PassMessageHandler()
//    private val closeMotionHandler = CloseMotionHandler()
//
//    fun dispatchMessage(byteArray: ByteArray): String {
//        val parseFrom = CloudDriveMqttMessageCreator.CloudDriveMqttMessage.parseFrom(byteArray)
//        val cmd = parseFrom.cmd
//        MqttManager.getInstance().publicLogMessage(LogMessage(LogMessage.TYPE_CMD, "cmd:$cmd"))
//        return when (cmd) {
//            10001 -> {
//                val data = parseFrom.data.unpack(CloudDriveMqttMessageCreator.MqttFaceIssueData::class.java)
//                val faceImage = FaceImage(data.image, data.userId)
//                passMessageHandler.handleMessage(faceImage)
//            }
//            10031 -> {
//                val data = parseFrom.data.unpack(CloudDriveMqttMessageCreator.MqttEmotionStopData::class.java)
//                closeMotionHandler.handleMessage(data.userId)
//            }
//            else -> {
//                ""
//            }
//        }
//
//    }
//}
//
//
//class PassMessageHandler {
//
//    private val handler: Handler = Handler(Looper.getMainLooper())
//    private val faceRegister = FaceRegister()
//
//    fun handleMessage(faceImage: FaceImage): String {
////        val userType = object : TypeToken<NetResult<FaceImage>>() {}.type
////        val netResult = Gson().fromJson<NetResult<FaceImage>>(content, userType)
////        Log.i("图片：", netResult.data.image)
//
//        val imageArray = Base64.decode(faceImage.image, Base64.DEFAULT)
//        val userId = faceImage.userId
//
//        FinishPassActivityEvent().post()
//        ZCRApp.INSTANCE.toast("特征提取中...")
//        MqttManager.getInstance()
//            .publicLogMessage(LogMessage(LogMessage.TYPE_RECEIVE_FACE_IMAGE, "receive face image"))
//
//        handler.postDelayed({
//            faceRegister.registerFace(imageArray, userId)
//        }, 3_000)
//
//        return userId
//    }
//}
//
//class CloseMotionHandler {
//    fun handleMessage(content: String): String {
//        MqttManager.getInstance().publicLogMessage(LogMessage(LogMessage.TYPE_CLOSE_MOTION, "close motion"))
//        FinishPassActivityEvent().post()
////        val userType = object : TypeToken<NetResult<CloseMotion>>() {}.type
////        val netResult = Gson().fromJson<NetResult<CloseMotion>>(content, userType)
//        return content
//    }
//}
//
