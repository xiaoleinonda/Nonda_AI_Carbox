package us.nonda.commonibrary.config

import com.google.gson.Gson
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.SPUtils

class CarboxConfigRepostory private constructor() {

    private val context = AppUtils.context

    /**
     * 采集时间
     */
    var gpsCollectFreq: Long = 1 * 1000

    /**
     * 上报时间
     */
    var gpsReportFreq: Long = 1 * 20 * 1000

    /**
     * 采集时间
     */
    var gSensorCollectFreq: Long = 100

    /**
     * 上报时间
     */
    var gSensorReportFreq: Long = 1 * 20 * 1000

    /**
     * 采集时间
     */
    var gyroCollectFreq: Long = 100

    /**
     * 上报时间
     */
    var gyroReportFreq: Long = 1 * 20 * 1000


    var emotionReportFreq: Long = 1 * 60 * 1000
    var faceResultReportFreq: Long = 1 * 60 * 1000


    companion object {
        private val SP_KEY_GSENSOR_CONFIG = "sp_key_gsensor_config"
        private val SP_KEY_GYRO_CONFIG = "sp_key_gyro_config"
        private val SP_KEY_GPS_CONFIG = "sp_key_gps_config"
        val instance: CarboxConfigRepostory by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            CarboxConfigRepostory()
        }

    }

    init {
        getGpsConfig()?.run {
            gpsCollectFreq = collectFreq
            gpsReportFreq = reportFreq
        }

        getGSensorConfig()?.run {
            gSensorCollectFreq = collectFreq
            gSensorReportFreq = reportFreq
        }

        getGyroConfig()?.run {
            gyroCollectFreq = collectFreq
            gyroReportFreq = reportFreq
        }
    }

    private fun getGSensorConfig(): GSensorConfig? {
        val json = SPUtils.get(context, SP_KEY_GSENSOR_CONFIG, "") as String
        return Gson().fromJson(json, GSensorConfig::class.java)
    }

    fun putGSensorConfig(gSensorConfig: GSensorConfig) {
        gSensorConfig.run {
            gSensorCollectFreq = collectFreq
            gSensorReportFreq = reportFreq
        }
        SPUtils.put(context, SP_KEY_GSENSOR_CONFIG, gSensorConfig)
    }

    private fun getGyroConfig(): GyroConfig? {
        val json = SPUtils.get(context, SP_KEY_GYRO_CONFIG, "") as String
        return Gson().fromJson(json, GyroConfig::class.java)
    }

    fun putGyroConfig(gyroConfig: GyroConfig) {
        gyroConfig.run {
            gyroCollectFreq = collectFreq
            gyroReportFreq = reportFreq
        }
        SPUtils.put(context, SP_KEY_GYRO_CONFIG, gyroConfig)
    }

    private fun getGpsConfig(): GpsConfig? {
        val json = SPUtils.get(context, SP_KEY_GPS_CONFIG, "") as String
        return Gson().fromJson(json, GpsConfig::class.java)
    }

    fun putGpsConfig(gpsConfig: GpsConfig) {
        gpsConfig.run {
            gpsCollectFreq = collectFreq
            gpsReportFreq = reportFreq
        }
        SPUtils.put(context, SP_KEY_GPS_CONFIG, gpsConfig)
    }
}