package us.nonda.ai.app.sensor

import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import io.reactivex.schedulers.Schedulers
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.mqttlibrary.model.GPSBean
import us.nonda.mqttlibrary.mqtt.MqttManager
import java.lang.Exception
import java.util.*
import java.util.concurrent.Executors

class CarLocationListener : LocationListener {
    @Volatile
    private var data = arrayListOf<GPSBean>()
    val newSingleThreadExecutor = Executors.newSingleThreadExecutor()

    override fun onLocationChanged(p0: Location?) {
            newSingleThreadExecutor.execute {
                p0?.run {
                        val currentTimeMillis = System.currentTimeMillis()
                        if (data.size > 0) {
                            val time = data[0].time
                            if (currentTimeMillis - time > CarboxConfigRepostory.instance.gpsReportFreq) {
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

    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
    }

    override fun onProviderEnabled(p0: String?) {
    }

    override fun onProviderDisabled(p0: String?) {
    }

    fun stop(){
        newSingleThreadExecutor.shutdownNow()
    }

}

