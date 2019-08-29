package us.nonda.mqttlibrary.handler

import io.nonda.onedata.proto.contract.CloudDriveMqttMessageCreator
import us.nonda.mqttlibrary.mqtt.IMqttMessageHandler
import us.nonda.mqttlibrary.mqtt.MqttManager

class ReportStatusHandler : IMqttMessageHandler {
    override fun handleMqttMessage(cloudDriveMqttMessage: CloudDriveMqttMessageCreator.CloudDriveMqttMessage) {
        val cloudDriveMqttFreqData =
            cloudDriveMqttMessage.data.unpack(CloudDriveMqttMessageCreator.CloudDriveMqttReportStatusData::class.java)
        if (cloudDriveMqttFreqData.type == 1) {
            MqttManager.getInstance().publishStatus()
        }
    }
}