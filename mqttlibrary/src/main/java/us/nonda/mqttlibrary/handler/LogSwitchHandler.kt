package us.nonda.mqttlibrary.handler

import io.nonda.onedata.proto.contract.CloudDriveMqttMessageCreator
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.commonibrary.config.EmotionConfig
import us.nonda.mqttlibrary.mqtt.IMqttMessageHandler

class LogSwitchHandler : IMqttMessageHandler {
    override fun handleMqttMessage(cloudDriveMqttMessage: CloudDriveMqttMessageCreator.CloudDriveMqttMessage) {
        val cloudDriveConfigChangeData = cloudDriveMqttMessage.data.unpack(CloudDriveMqttMessageCreator.CloudDriveConfigChangeData::class.java)

        val content = cloudDriveConfigChangeData.content

        CarboxConfigRepostory.instance.putLogSwitch(content)
        MyLog.d("收到消息","下发log开关 content=$content")
    }
}