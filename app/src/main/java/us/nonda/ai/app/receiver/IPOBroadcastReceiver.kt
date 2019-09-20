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
import us.nonda.videopushlibrary.uploadTask.UploadManager

/**
 * IPO广播
 */
class IPOBroadcastReceiver : BroadcastReceiver() {
    private val action_ipo_on = "android.intent.action.ACTION_BOOT_IPO"
    private val action_ipo_off = "android.intent.action.ACTION_SHUTDOWN_IPO"
    private val TAG = "IPO"

    override fun onReceive(context: Context?, intent: Intent?) {

        if (action_ipo_on == intent?.action) {
            MyLog.d("广播", "唤醒  ")
            NondaApp.ipoStatus = true

            ipoOn(context!!)

            EventBus.getDefault().post(IpoEvent(1))
        } else if (action_ipo_off == intent?.action) {
            MyLog.d("广播", "休眠   ")
            NondaApp.ipoStatus = false

            EventBus.getDefault().post(IpoEvent(2))
        }
    }

    /**
     * 休眠
     */
    private fun ipoOff(context: Context) {

    }

    /**
     * 唤醒
     * 唤醒状态下 如果acc 是on的时候就打开业务
     */
    private fun ipoOn(context: Context) {
        MyLog.d(TAG, "广播 唤醒   accStatus=${NondaApp.accStatus} ipoStatus=${NondaApp.ipoStatus}  ")

        if (NondaApp.accStatus && NondaApp.ipoStatus) {
            CarBoxControler.instance.openCamera(context)
        }


    }


}
