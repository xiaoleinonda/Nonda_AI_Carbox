package us.nonda.mqttlibrary.mqtt

import us.nonda.mqttlibrary.handler.*
import us.nonda.mqttlibrary.model.Constant

class MqttHandlerFactory {
    companion object {
        fun getHandlerByCMD(cmd: Int): IMqttMessageHandler {
            when (cmd) {
                Constant.RESPONSE_STATUS -> {
                    return ReportStatusHandler()
                }
                Constant.RESPONSE_GPS -> {
                    return GpsHandler()
                }
                Constant.RESPONSE_GSENSOR -> {
                    return GSensorHandler()
                }
                Constant.RESPONSE_GYRO -> {
                    return GyroHandler()
                }
                Constant.RESPONSE_FACE_RESULT -> {
                    return FaceHandler()
                }
                Constant.RESPONSE_EMOTION -> {
                    return EmotionHandler()
                }
                Constant.RESPONSE_LOG_SWITCH -> {
                    return LogSwitchHandler()
                }
                Constant.RESPONSE_MQTT_URL -> {
                    return MqttUrlHandler()
                }
                Constant.RESPONSE_HTTP_URL -> {
                    return HttpUrlHandler()
                }

            }
            return IMqttMessageHandler { }
        }
    }
}