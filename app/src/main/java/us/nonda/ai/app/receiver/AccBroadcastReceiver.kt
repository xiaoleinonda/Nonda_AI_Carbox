package us.nonda.ai.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import us.nonda.ai.app.ui.VideoRecordActivity
import us.nonda.ai.controler.CarBoxControler
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.status.CarboxCacheManager
import us.nonda.commonibrary.utils.FinishActivityManager

/**
 * Created by chenjun on 2019-06-12.
 */
class AccBroadcastReceiver : BroadcastReceiver() {

    private val TAG = "AccBroadcastReceiver"

    private val action_acc_off = "android.intent.action.ACTION_POWER_DISCONNECTED"//acc off
    private val action_acc_on = "android.intent.action.ACTION_POWER_CONNECTED"//acc on
    override fun onReceive(context: Context?, intent: Intent?) {

        if (action_acc_on == intent?.action) {
            accOn(context)
        } else if (action_acc_off == intent?.action) {
            accOff()
        }
    }

    /**
     * 初始化
     * 开启摄像头
     */
    private fun accOn(context: Context?) {
        MyLog.d(TAG, "accOn")
        CarBoxControler.instance.accOnMode(context!!)
    }


    private fun accOff() {
        MyLog.d(TAG, "accOff")
        CarBoxControler.instance.checkOTA()

    }


}