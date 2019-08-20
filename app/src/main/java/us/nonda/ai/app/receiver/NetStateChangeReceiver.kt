package us.nonda.ai.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import us.nonda.commonibrary.utils.NetworkUtil
import us.nonda.facelibrary.manager.FaceSDKManager


class NetStateChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
            val connectivityStatus = NetworkUtil.getConnectivityStatus(context)
            if (connectivityStatus) {
//                MqttManager.getInstance().onStart()
                //PublishMqttManager.getInstance().onStart()
                initFace(context)
            }
        }
    }

    private fun initFace(context: Context) {
        FaceSDKManager.instance.init(context, "")
    }
}
