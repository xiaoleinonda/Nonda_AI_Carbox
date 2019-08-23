package us.nonda.mqttlibrary.mqtt

import io.nonda.onedata.proto.contract.CloudDriveMqttMessageCreator
import us.nonda.mqttlibrary.model.Constant

class MqttHandlerFactory {
    companion object {

        fun getHandlerByCMD(cmd: Int): IMqttMessageHandler {
//            when (cmd) {
//                Constant.RESPONSE_STATUS -> {
//                    return IMqttMessageHandler { }
//                }
//                Constant.RESPONSE_GPS -> {
//                }
//                Constant.RESPONSE_GSENSOR -> {
//                }
//                Constant.RESPONSE_GYRO -> {
//                }
//                Constant.RESPONSE_FACE_RESULT -> {
//                }
//                Constant.RESPONSE_EMOTION -> {
//                }
//            }
            return IMqttMessageHandler { }

        }
    }
}