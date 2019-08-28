package us.nonda.ai.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import us.nonda.ai.controler.CarBoxControler
import com.yaoxiaowen.download.DownloadHelper
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.NetworkUtil
import us.nonda.facelibrary.manager.FaceSDKManager
import us.nonda.mqttlibrary.mqtt.MqttManager


class NetStateChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
            val connectivityStatus = NetworkUtil.getConnectivityStatus(context)
            if (connectivityStatus) {
                initDevice()
                MqttManager.getInstance().onStart()
//                val mDownloadHelper = DownloadHelper.getInstance()
//                mDownloadHelper.addCarBoxTask(AppUtils.context)
            }else{
                MqttManager.getInstance().onStop()
            }
        }
    }

    private fun initDevice() {
      FaceSDKManager.instance.checkLicenceStatus()
      FaceSDKManager.instance.checkRegistFaceStatus()

    }

    private fun initFace(context: Context) {
//        FaceSDKManager.instance.init(context, "")
        FaceSDKManager.instance.checkLicenceStatus()

    }
}
