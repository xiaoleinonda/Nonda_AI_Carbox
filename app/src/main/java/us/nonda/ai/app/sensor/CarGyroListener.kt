package us.nonda.ai.app.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.mqttlibrary.model.GyroBean
import us.nonda.mqttlibrary.mqtt.MqttManager

class CarGyroListener: SensorEventListener {

    private val data = arrayListOf<GyroBean>()

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        val x = sensorEvent.values[0]
        val y = sensorEvent.values[1]
        val z = sensorEvent.values[2]

        val currentTimeMillis = System.currentTimeMillis()
        if (data.size > 0) {
            val time = data[0].time
            if (currentTimeMillis - time >  CarboxConfigRepostory.instance.gyroReportFreq) {
                var reportData = arrayListOf<GyroBean>()
                reportData.addAll(data)
                MqttManager.getInstance().publishGyro(reportData)
                data.clear()
            }
        }
        data.add(GyroBean(x, y, z, currentTimeMillis))

    }

    override fun onAccuracyChanged(sensor: Sensor, i: Int) {

    }
}