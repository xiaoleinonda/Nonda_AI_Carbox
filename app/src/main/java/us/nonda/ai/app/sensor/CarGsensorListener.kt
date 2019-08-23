package us.nonda.ai.app.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.commonibrary.config.GSensorConfig
import us.nonda.mqttlibrary.model.GSensorBean
import us.nonda.mqttlibrary.mqtt.MqttManager

class CarGsensorListener() : SensorEventListener {
    private val data = arrayListOf<GSensorBean>()

    private val TAG = "CarGsensorListener"
    override fun onSensorChanged(sensorEvent: SensorEvent) {
        val x = sensorEvent.values[0]
        val y = sensorEvent.values[1]
        val z = sensorEvent.values[2]

        val currentTimeMillis = System.currentTimeMillis()
        if (data.size > 0) {
            val time = data[0].time
            if (currentTimeMillis - time > CarboxConfigRepostory.instance.gSensorReportFreq) {
                var reportData = arrayListOf<GSensorBean>()
                reportData.addAll(data)
                MqttManager.getInstance().publishGSensor(reportData)
                data.clear()

            }
        }
        data.add(GSensorBean(x, y, z, currentTimeMillis))

    }

    override fun onAccuracyChanged(sensor: Sensor, i: Int) {

    }
}