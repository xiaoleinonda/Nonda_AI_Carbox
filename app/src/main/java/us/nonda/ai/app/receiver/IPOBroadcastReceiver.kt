package us.nonda.ai.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
        } else if (action_ipo_off == intent?.action) {
            MyLog.d(TAG, "休眠")
            onIpoOff()
        }
    }

    /**
     * 休眠
     */
    private fun onIpoOff() {
        CarBoxControler.instance.sleep()

        CarBoxControler.instance.countDownNoticeIPO()
    }

    /**
     * 唤醒
     */
    private fun onIpoON(context: Context?) {
        val accStatus = CarBoxControler.instance.getAccStatus()
        if (accStatus != 0) {//acc on
//            CarBoxControler.instance.wakeUp(context!!)
        } else {
            CarBoxControler.instance.onIpoON()
        }

    }


}
