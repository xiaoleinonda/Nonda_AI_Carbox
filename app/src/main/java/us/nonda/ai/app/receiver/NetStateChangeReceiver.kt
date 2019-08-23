package us.nonda.ai.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.utils.NetworkUtil
import us.nonda.facelibrary.manager.FaceSDKManager
import us.nonda.mqttlibrary.mqtt.MqttManager
import us.nonda.mqttlibrary.mqtt.MqttService


class NetStateChangeReceiver : BroadcastReceiver() {

    private val TAG = "NetStateChangeReceiver"
    override fun onReceive(context: Context, intent: Intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
            val connectivityStatus = NetworkUtil.getConnectivityStatus(context)
            if (connectivityStatus) {
//                initFace(context)
                MyLog.d(TAG, "有网")

//                MqttManager.getInstance().doConnect(context)
            } else {
                MyLog.d(TAG, "断网")

//                MqttManager.getInstance().stopConnect(context)
            }
        }
    }


}
