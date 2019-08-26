package us.nonda.ai.app.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import io.reactivex.schedulers.Schedulers
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.commonibrary.config.GSensorConfig
import us.nonda.mqttlibrary.model.GSensorBean
import us.nonda.mqttlibrary.mqtt.MqttManager
import java.lang.Exception
import java.util.concurrent.Executors

class CarGsensorListener() : SensorEventListener {
    private val data = arrayListOf<GSensorBean>()
    private val newSingleThreadExecutor = Executors.newSingleThreadExecutor()


    private val TAG = "CarGsensorListener"
    override fun onSensorChanged(sensorEvent: SensorEvent) {
            newSingleThreadExecutor.execute {
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

    }

    override fun onAccuracyChanged(sensor: Sensor, i: Int) {

    }

    fun stop() {
        newSingleThreadExecutor.shutdownNow()
    }
}