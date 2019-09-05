package us.nonda.ai.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.nfc.Tag
import us.nonda.ai.controler.CarBoxControler
import com.yaoxiaowen.download.DownloadHelper
import us.nonda.cameralibrary.status.CameraStatus
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.NetworkUtil
import us.nonda.facelibrary.manager.FaceSDKManager
import us.nonda.facelibrary.manager.FaceSDKManager2
import us.nonda.mqttlibrary.mqtt.MqttManager


class NetStateChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
            val connectivityStatus = NetworkUtil.getConnectivityStatus(context)
            if (connectivityStatus) {
                MyLog.d("网络", "有网")
//                initDevice()
                MqttManager.getInstance().onStart()
                checkAccStatus()
            } else {
                MyLog.d("网络", "断网")

                MqttManager.getInstance().onStop()
            }
        }
    }


    /**
     *  acc off状态网络波动后重新监测下载新版本App
     */
    private fun checkAccStatus() {
        if (CarBoxControler.instance.isAccOff()) {//acc off
            MyLog.d("下载", "Acc off 状态下断网后重连成功")
            CarBoxControler.instance.accOffModeWork()
        } else {
            FaceSDKManager2.instance.init()
        }
    }
}
