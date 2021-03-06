package us.nonda.ai.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import us.nonda.ai.MainActivity
import us.nonda.ai.app.ui.AccActivity
import us.nonda.commonibrary.MyLog
import us.nonda.facelibrary.manager.FaceSDKManager2

/**
 * Created by chenjun on 2019-06-12.
 */
class BootBroadcastReceiver : BroadcastReceiver() {
    private val action = "android.intent.action.BOOT_COMPLETED"
    private val actionShutSown = "android.intent.action.ACTION_SHUTDOWN"

    private val TAG="BootBroadcastReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        FaceSDKManager2.instance.clearFace()

        if (action == intent?.action) {
            MyLog.d("广播", "开机")
/*
            Intent(context, MainActivity::class.java).run {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context?.startActivity(this)
            }*/
        }else if (actionShutSown == intent?.action) {

            MyLog.d("广播", "关机")
        }
    }
}
