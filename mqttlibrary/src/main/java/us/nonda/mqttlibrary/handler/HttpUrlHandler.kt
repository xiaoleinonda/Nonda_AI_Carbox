package us.nonda.mqttlibrary.handler

import android.os.Build
import io.nonda.onedata.proto.contract.CloudDriveMqttMessageCreator
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.commonibrary.config.EmotionConfig
import us.nonda.mqttlibrary.BuildConfig
import us.nonda.mqttlibrary.mqtt.IMqttMessageHandler

class HttpUrlHandler : IMqttMessageHandler {
    override fun handleMqttMessage(cloudDriveMqttMessage: CloudDriveMqttMessageCreator.CloudDriveMqttMessage) {
        val cloudDriveConfigChangeData =
            cloudDriveMqttMessage.data.unpack(CloudDriveMqttMessageCreator.CloudDriveConfigChangeData::class.java)

        val content = cloudDriveConfigChangeData.content

        if (!BuildConfig.DEBUG) {
            CarboxConfigRepostory.instance.putHttpUrl(content)
            MyLog.d("收到消息", "下发http url content=$content")
        }
    }
}