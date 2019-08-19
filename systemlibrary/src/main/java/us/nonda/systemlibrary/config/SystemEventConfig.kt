package us.nonda.systemlibrary.config

import android.content.Context
import com.google.gson.Gson
import us.nonda.commonibrary.utils.SPUtils

class SystemEventConfig private constructor(private var context: Context) {

    companion object {
        val SP_KEY = "SystemEventConfig"
        private var instance: SystemEventConfig? = null
        fun get(context: Context): SystemEventConfig {
            if (instance == null) {
                val json = SPUtils.get(context, SP_KEY, "") as String
                var config = Gson().fromJson(json, SystemEventConfig::class.java)
                if (config == null) {
                    instance = SystemEventConfig(context)
                } else {
                    instance = config
                }
            }

            return instance!!
        }

    }

    /**
     * gps上报频率
     */
    var gpsReportFreq: Long = 1000
        private set(value) {
            field = value
        }


    /**
     * gSensor上报频率
     */
    var gSensorReportFreq: Long = 1000
        private set(value) {
            field = value
        }

    /**
     * gyro上报频率
     */
    var gyroReportFreq: Long = 1000
        private set(value) {
            field = value
        }

    fun setGPSReportFreq(freq: Long, cache: Boolean) {
        gpsReportFreq = freq
        if (cache) {
            updateCache()
        }
    }


    fun setGSensorReportFreq(freq: Long, cache: Boolean) {
        gSensorReportFreq = freq
        if (cache) {
            updateCache()
        }
    }


    fun setGyroReportFreqFreq(freq: Long, cache: Boolean) {
        gyroReportFreq = freq
        if (cache) {
            updateCache()
        }
    }

    fun updateCache() {
        val toJson = Gson().toJson(this)
        SPUtils.put(context, SP_KEY, toJson)
    }


    fun resetConfig() {
        SPUtils.remove(context, SP_KEY)
        gpsReportFreq = 1000
        gSensorReportFreq = 1000
        gyroReportFreq = 1000
    }

}