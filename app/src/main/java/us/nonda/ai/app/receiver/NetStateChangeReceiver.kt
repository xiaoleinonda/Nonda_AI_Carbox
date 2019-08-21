package us.nonda.ai.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import us.nonda.commonibrary.utils.NetworkUtil
import us.nonda.facelibrary.manager.FaceSDKManager
import us.nonda.mqttlibrary.mqtt.MqttManager
import us.nonda.mqttlibrary.mqtt.MqttService


class NetStateChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
            val connectivityStatus = NetworkUtil.getConnectivityStatus(context)
            if (connectivityStatus) {
                initFace(context)
                MqttManager.getInstance().doConnect(context)
            }else{
                MqttManager.getInstance().stopConnect(context)
            }
        }
    }

    private fun initFace(context: Context) {
//        FaceSDKManager.instance.init(context, "")
        FaceSDKManager.instance.checkLicenceStatus()

    }
}
