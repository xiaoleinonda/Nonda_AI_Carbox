package us.nonda.ai.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import us.nonda.ai.controler.CarBoxControler
import us.nonda.commonibrary.MyLog
import us.nonda.facelibrary.manager.FaceSDKManager2
import us.nonda.mqttlibrary.mqtt.MqttManager
import us.nonda.videopushlibrary.uploadTask.UploadManager

/**
 * Created by chenjun on 2019-06-12.
 */
class AccBroadcastReceiver : BroadcastReceiver() {

    private val TAG = "AccBroadcastReceiver"

    private val action_acc_off = "android.intent.action.ACTION_POWER_DISCONNECTED"//acc off
    private val action_acc_on = "android.intent.action.ACTION_POWER_CONNECTED"//acc on
    override fun onReceive(context: Context?, intent: Intent?) {

        if (action_acc_on == intent?.action) {
            MyLog.d(TAG,"action_acc_on")
            MqttManager.getInstance().publishEventData(1001, "1")
            accOn(context)
        } else if (action_acc_off == intent?.action) {
            MyLog.d(TAG,"action_acc_off")
            MqttManager.getInstance().publishEventData(1001, "2")
            accOff()
        }
    }

    /**
     * 初始化
     * 开启摄像头
     */
    private fun accOn(context: Context?) {
        MyLog.d(TAG, "accOn")
        CarBoxControler.instance.openCamera(context!!)
        UploadManager.getInstance().stopUpload()
    }


    private fun accOff() {
        MyLog.d(TAG, "accOff")
        FaceSDKManager2.instance.isRegisted = false
        CarBoxControler.instance.onAccOff()
    }


}