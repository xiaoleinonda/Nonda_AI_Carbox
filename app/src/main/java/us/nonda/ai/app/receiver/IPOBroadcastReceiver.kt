package us.nonda.ai.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import us.nonda.ai.app.service.WakeUpService
import us.nonda.ai.controler.CarBoxControler
import us.nonda.commonibrary.MyLog

/**
 * IPO广播
 */
class IPOBroadcastReceiver : BroadcastReceiver() {
    private val action_ipo_on = "android.intent.action.ACTION_BOOT_IPO"
    private val action_ipo_off = "android.intent.action.ACTION_SHUTDOWN_IPO"
    private val TAG = "IPO"

    override fun onReceive(context: Context?, intent: Intent?) {

        if (action_ipo_on == intent?.action) {
            MyLog.d(TAG, "唤醒")
            onIpoON(context)
            Log.d("NondaApp", "唤醒")
        } else if (action_ipo_off == intent?.action) {
            MyLog.d(TAG, "休眠")
            Log.d("NondaApp", "休眠")

            onIpoOff(context)
        }
    }

    /**
     * 休眠
     */
    private fun onIpoOff(context: Context?) {
        CarBoxControler.instance.onIpoOff(context)
    }

    /**
     * 唤醒
     */
    private fun onIpoON(context: Context?) {
        val accOff = CarBoxControler.instance.isAccOff()
        WakeUpService.stopService(context!!)
        if (!accOff) {//acc on
//            CarBoxControler.instance.wakeUp(context!!)
        } else {
            CarBoxControler.instance.onIpoONGetGps()
        }

    }


}
