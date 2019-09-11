package us.nonda.ai.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.greenrobot.eventbus.EventBus
import us.nonda.ai.app.base.NondaApp
import us.nonda.ai.app.service.WakeUpService
import us.nonda.ai.controler.CarBoxControler
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.event.IpoEvent
import us.nonda.commonibrary.utils.DeviceLightUtils
import us.nonda.commonibrary.utils.DeviceUtils

/**
 * IPO广播
 */
class IPOBroadcastReceiver : BroadcastReceiver() {
    private val action_ipo_on = "android.intent.action.ACTION_BOOT_IPO"
    private val action_ipo_off = "android.intent.action.ACTION_SHUTDOWN_IPO"
    private val TAG = "IPO"

    override fun onReceive(context: Context?, intent: Intent?) {

        if (action_ipo_on == intent?.action) {
            NondaApp.ipoStatus = true
            MyLog.d("广播", "唤醒  ")
            EventBus.getDefault().post(IpoEvent(1))
        } else if (action_ipo_off == intent?.action) {
            NondaApp.ipoStatus = false
            MyLog.d("广播", "休眠   ")
            EventBus.getDefault().post(IpoEvent(2))
        }
    }



}
