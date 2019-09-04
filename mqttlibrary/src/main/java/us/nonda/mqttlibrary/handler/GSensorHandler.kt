package us.nonda.mqttlibrary.handler

import io.nonda.onedata.proto.contract.CloudDriveMqttMessageCreator
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.commonibrary.config.GSensorConfig
import us.nonda.mqttlibrary.mqtt.IMqttMessageHandler

class GSensorHandler : IMqttMessageHandler {
    override fun handleMqttMessage(cloudDriveMqttMessage: CloudDriveMqttMessageCreator.CloudDriveMqttMessage) {
        val cloudDriveMqttFreqData =
            cloudDriveMqttMessage.data.unpack(CloudDriveMqttMessageCreator.CloudDriveMqttFreqData::class.java)
        val gSensor = GSensorConfig(
            (cloudDriveMqttFreqData!!.collectFreq).toLong(),
            (cloudDriveMqttFreqData.reportFreq).toLong()
        )
        CarboxConfigRepostory.instance.putGSensorConfig(gSensor)
        MyLog.d("收到消息","gSensor")
    }
}