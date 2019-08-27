package us.nonda.mqttlibrary.model

class Constant {
    companion object{
        /** CMD 定义  */
        //上报
        const val PUBLISH_STATUS = 10001
        const val PUBLISH_EVENT = 10002
        const val PUBLISH_GPS = 10003
        const val PUBLISH_GSENSOR = 10004
        const val PUBLISH_GYRO = 10005
        const val PUBLISH_FACE_RESULT = 10006
        const val PUBLISH_EMOTION = 10007
        //下发
        const val RESPONSE_STATUS = 20001
        const val RESPONSE_GPS = 20002
        const val RESPONSE_GSENSOR = 20003
        const val RESPONSE_GYRO = 20004
        const val RESPONSE_FACE_RESULT = 20005
        const val RESPONSE_EMOTION = 20006
    }
}
