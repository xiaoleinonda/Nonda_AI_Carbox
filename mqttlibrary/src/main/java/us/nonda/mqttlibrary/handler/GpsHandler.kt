package us.nonda.mqttlibrary.handler

import io.nonda.onedata.proto.contract.CloudDriveMqttMessageCreator
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.commonibrary.config.GpsConfig
import us.nonda.mqttlibrary.mqtt.IMqttMessageHandler

class GpsHandler : IMqttMessageHandler {
    override fun handleMqttMessage(cloudDriveMqttMessage: CloudDriveMqttMessageCreator.CloudDriveMqttMessage) {
        val cloudDriveMqttFreqData =
            cloudDriveMqttMessage.data.unpack(CloudDriveMqttMessageCreator.CloudDriveMqttFreqData::class.java)
        val gpsConfig = GpsConfig(
            (cloudDriveMqttFreqData!!.collectFreq).toLong(),
            (cloudDriveMqttFreqData.reportFreq).toLong()
        )
        CarboxConfigRepostory.instance.putGpsConfig(gpsConfig)
    }
}