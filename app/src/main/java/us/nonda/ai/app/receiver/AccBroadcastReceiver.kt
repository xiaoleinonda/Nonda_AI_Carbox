package us.nonda.ai.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import us.nonda.ai.app.ui.VideoRecordActivity
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
            accOn()
        } else if (action_acc_off == intent?.action) {
            accOff()
        }
    }

    /**
     * 初始化
     * 开启摄像头
     */
    private fun accOn() {
        MyLog.d(TAG,"accOn")
        CarboxCacheManager.instance.putACCStatus(true)
    }


    private fun accOff() {
        MyLog.d(TAG,"accOff")
        CarboxCacheManager.instance.putACCStatus(false)

        closeCamera()
        closeFace()
        updateReportFreq()
    }

    /**
     * 关闭摄像头页面
     */
    private fun closeCamera() {
        FinishActivityManager.getManager().finishActivity(VideoRecordActivity::class.java)
    }

    /**
     * 关闭识别相关
     */
    private fun closeFace() {


    }

    /**
     * 修改上报频率
     */
    private fun updateReportFreq() {


    }
}