package us.nonda.mqttlibrary.handler

import io.nonda.onedata.proto.contract.CloudDriveMqttMessageCreator
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.commonibrary.config.GyroConfig
import us.nonda.mqttlibrary.mqtt.IMqttMessageHandler

class GyroHandler : IMqttMessageHandler {
    override fun handleMqttMessage(cloudDriveMqttMessage: CloudDriveMqttMessageCreator.CloudDriveMqttMessage) {
        val cloudDriveMqttFreqData = cloudDriveMqttMessage.data.unpack(CloudDriveMqttMessageCreator.CloudDriveMqttFreqData::class.java)
        val gyroConfig = GyroConfig(
            (cloudDriveMqttFreqData!!.collectFreq).toLong(),
            (cloudDriveMqttFreqData.reportFreq).toLong()
        )
        CarboxConfigRepostory.instance.putGyroConfig(gyroConfig)
        MyLog.d("收到消息","gyroConfig")
    }
}