package us.nonda.mqttlibrary.mqtt

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.annotation.Nullable

class MqttService : Service() {
    companion object {

        @Volatile
        private var instance: MqttService? = null

        fun getInstance(): MqttService =
            instance ?: synchronized(this) {
                instance ?: MqttService().also { instance = it }
            }
    }

    /**
     * 开启服务
     */
    fun startService(mContext: Context) {
        mContext.startService(Intent(mContext, MqttService::class.java))
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        MqttManager.getInstance().init()
        return super.onStartCommand(intent, flags, startId)
    }

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        MqttManager.getInstance().onDestroy()
        super.onDestroy()
    }
}