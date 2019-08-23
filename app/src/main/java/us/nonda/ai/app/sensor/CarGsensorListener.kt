package us.nonda.ai.app.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener

class CarGsensorListener  : SensorEventListener {
    private val data = arrayListOf<String>()

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        val x = sensorEvent.values[0]
        val y = sensorEvent.values[1]
        val z = sensorEvent.values[2]


    }

    override fun onAccuracyChanged(sensor: Sensor, i: Int) {

    }
}