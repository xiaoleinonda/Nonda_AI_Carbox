package us.nonda.ai.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.greenrobot.eventbus.EventBus
import us.nonda.ai.app.base.NondaApp
import us.nonda.ai.controler.CarBoxControler
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.event.AccEvent
import us.nonda.commonibrary.utils.DeviceUtils
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
            NondaApp.accStatus = true
            EventBus.getDefault().post(AccEvent(1))
            MyLog.d("广播","action_acc_on IPO=${DeviceUtils.getIpoStatus()}")
        } else if (action_acc_off == intent?.action) {
            NondaApp.accStatus = false
            EventBus.getDefault().post(AccEvent(2))
            MyLog.d("广播","action_acc_off  IPO=${DeviceUtils.getIpoStatus()}")
        }
    }




}