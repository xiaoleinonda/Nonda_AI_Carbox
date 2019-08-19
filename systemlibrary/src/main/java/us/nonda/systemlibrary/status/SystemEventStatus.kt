package us.nonda.systemlibrary.status

import android.content.Context
import com.google.gson.Gson
import us.nonda.commonibrary.utils.SPUtils

class SystemEventStatus private constructor(private var context: Context) {

    companion object {
        private val SP_KEY = "SystemEventStatus"

        private var instance: SystemEventStatus? = null
        fun get(context: Context): SystemEventStatus {
            if (instance == null) {
                val get = SPUtils.get(context, SP_KEY, "") as String
                var status = Gson().fromJson(get, SystemEventStatus::class.java)
                if (status == null) {
                    instance = SystemEventStatus(context)
                } else {
                    instance = status
                }
            }

            return instance!!
        }
    }


    /**
     * gps上报状态
     * 0正在上报
     */
    var gpsStatus: Int = -1
        private set(value) {
            field = value
        }

    /**
     * gSensor上报状态
     * 0正在上报
     */
    var gSensorStatus: Int = -1
        private set(value) {
            field = value
        }

    /**
     * gyro上报状态
     * 0正在上报
     */
    var gyroSensorStatus: Int = -1
        private set(value) {
            field = value
        }

    fun setGPSStatus(status: Int, cache: Boolean) {
        gpsStatus = status
        if (cache) {
            updateCache()
        }
    }

    fun setGSensorStatus(status: Int, cache: Boolean) {
        gSensorStatus = status
        if (cache) {
            updateCache()
        }
    }

    fun setGyroStatus(status: Int, cache: Boolean) {
        gyroSensorStatus = status
        if (cache) {
            updateCache()
        }
    }


    fun resetStatus() {
        SPUtils.remove(context, SP_KEY)
        gpsStatus = -1
        gSensorStatus = -1
        gyroSensorStatus = -1
    }

    fun updateCache() {
        val toJson = Gson().toJson(this)
        SPUtils.put(context, SP_KEY, toJson)
    }
}