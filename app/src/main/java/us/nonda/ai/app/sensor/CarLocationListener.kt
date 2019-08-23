package us.nonda.ai.app.sensor

import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.mqttlibrary.model.GPSBean
import us.nonda.mqttlibrary.mqtt.MqttManager

class CarLocationListener : LocationListener {
    private val data = arrayListOf<GPSBean>()

    override fun onLocationChanged(p0: Location?) {
        p0?.run {
            val currentTimeMillis = System.currentTimeMillis()
            if (data.size > 0) {
                val time = data[0].time
                if (currentTimeMillis - time >  CarboxConfigRepostory.instance.gpsReportFreq) {
                    var reportData = arrayListOf<GPSBean>()
                    reportData.addAll(data)
                    MqttManager.getInstance().publishGPS(reportData)
                    data.clear()
                }
            }
            data.add(
                GPSBean(
                    latitude,
                    longitude,
                    speed,
                    accuracy,
                    bearing,
                    currentTimeMillis
                )
            )
        }

    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
    }

    override fun onProviderEnabled(p0: String?) {
    }

    override fun onProviderDisabled(p0: String?) {
    }

}

