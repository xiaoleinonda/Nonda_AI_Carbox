package us.nonda.mqttlibrary.mqtt;

import io.nonda.onedata.proto.contract.CloudDriveMqttMessageCreator;
import org.jetbrains.annotations.NotNull;

public interface IMqttMessageHandler {

    void handleMqttMessage(@NotNull CloudDriveMqttMessageCreator.CloudDriveMqttFreqData cloudDriveMqttFreqData);
}
