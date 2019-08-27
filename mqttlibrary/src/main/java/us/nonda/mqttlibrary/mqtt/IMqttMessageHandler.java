package us.nonda.mqttlibrary.mqtt;

import io.nonda.onedata.proto.contract.CloudDriveMqttMessageCreator;
import org.jetbrains.annotations.Nullable;

public interface IMqttMessageHandler {
    void handleMqttMessage(@Nullable CloudDriveMqttMessageCreator.CloudDriveMqttFreqData cloudDriveMqttFreqData);

}
