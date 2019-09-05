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


    var emotionReportFreq: Long = 1 * 10 * 1000
    var emotionCollectFreq: Long = 1 * 10 * 1000
    var faceResultReportFreq: Long = 1 * 10 * 1000
    var faceResultCollectFreq: Long = 1 * 10 * 1000


    companion object {
        private val SP_KEY_GSENSOR_CONFIG = "sp_key_gsensor_config"
        private val SP_KEY_GYRO_CONFIG = "sp_key_gyro_config"
        private val SP_KEY_GPS_CONFIG = "sp_key_gps_config"
        private val SP_KEY_EMOTION_CONFIG = "sp_key_emotion_config"
        private val SP_KEY_FACE_CONFIG = "sp_key_face_config"
        val instance: CarboxConfigRepostory by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            CarboxConfigRepostory()
        }

    }

    init {
        getGpsConfig()?.run {

            if (collectFreq < 1000) {
                gpsCollectFreq = 1000
            } else {
                gpsCollectFreq = collectFreq
            }

            if (reportFreq < 1000) {
                gpsReportFreq = 1000
            } else {
                gpsReportFreq = reportFreq
            }

        }

        getGSensorConfig()?.run {

            if (collectFreq < 1000) {
                gSensorCollectFreq = 1000
            } else {
                gSensorCollectFreq = collectFreq
            }

            if (reportFreq < 1000) {
                gSensorReportFreq = 1000
            } else {
                gSensorReportFreq = reportFreq
            }
        }

        getGyroConfig()?.run {

            if (collectFreq < 1000) {
                gyroCollectFreq = 1000
            } else {
                gyroCollectFreq = collectFreq
            }

            if (reportFreq < 1000) {
                gyroReportFreq = 1000
            } else {
                gyroReportFreq = reportFreq
            }
        }
    }

    private fun getGSensorConfig(): GSensorConfig? {
        try {
            val json = SPUtils.get(context, SP_KEY_GSENSOR_CONFIG, "") as String
            return Gson().fromJson(json, GSensorConfig::class.java)
        } catch (e: Exception) {
            return null
        }

    }

    fun putGSensorConfig(gSensorConfig: GSensorConfig) {
        try {
            gSensorConfig.run {
                gSensorCollectFreq = collectFreq
                gSensorReportFreq = reportFreq
            }
            SPUtils.put(context, SP_KEY_GSENSOR_CONFIG, gSensorConfig)
        } catch (e: Exception) {
        }
    }

    private fun getGyroConfig(): GyroConfig? {
        try {
            val json = SPUtils.get(context, SP_KEY_GYRO_CONFIG, "") as String
            return Gson().fromJson(json, GyroConfig::class.java)
        } catch (e: Exception) {
            return null
        }
    }

    fun putGyroConfig(gyroConfig: GyroConfig) {
        try {
            gyroConfig.run {
                gyroCollectFreq = collectFreq
                gyroReportFreq = reportFreq
            }
            SPUtils.put(context, SP_KEY_GYRO_CONFIG, gyroConfig)
        } catch (e: Exception) {
        }
    }

    private fun getGpsConfig(): GpsConfig? {
        try {
            val json = SPUtils.get(context, SP_KEY_GPS_CONFIG, "") as String
            val bean = Gson().fromJson(json, GpsConfig::class.java)
            return bean
        } catch (e: Exception) {
            return null
        }
    }

    fun putGpsConfig(gpsConfig: GpsConfig) {
        try {
            gpsConfig.run {
                gpsCollectFreq = collectFreq
                gpsReportFreq = reportFreq
            }
            SPUtils.put(context, SP_KEY_GPS_CONFIG, gpsConfig)
        } catch (e: Exception) {
        }
    }

    private fun getEmotionConfig(): EmotionConfig? {
        val json = SPUtils.get(context, SP_KEY_EMOTION_CONFIG, "") as String
        return Gson().fromJson(json, EmotionConfig::class.java)
    }

    fun putEmotionConfig(gpsConfig: EmotionConfig) {
        try {
            gpsConfig.run {
                if (collectFreq < 1000) {
                    emotionCollectFreq = 1000
                } else {
                    emotionCollectFreq = collectFreq
                }

                if (reportFreq < 1000) {
                    emotionReportFreq = 1000
                } else {
                    emotionReportFreq = reportFreq
                }
            }
            SPUtils.put(context, SP_KEY_EMOTION_CONFIG, gpsConfig)
        } catch (e: Exception) {
            return
        }
    }


    private fun getFaceConfig(): FaceConfig? {
        val json = SPUtils.get(context, SP_KEY_FACE_CONFIG, "") as String
        return Gson().fromJson(json, FaceConfig::class.java)
    }

    fun putFaceConfig(gpsConfig: FaceConfig) {
        try {
            gpsConfig.run {

                if (collectFreq < 1000) {
                    faceResultCollectFreq = 1000
                } else {
                    faceResultCollectFreq = collectFreq
                }

                if (reportFreq < 1000) {
                    faceResultReportFreq = 1000
                } else {
                    faceResultReportFreq = reportFreq
                }
            }
            SPUtils.put(context, SP_KEY_FACE_CONFIG, gpsConfig)
        } catch (e: Exception) {
        }
    }
}