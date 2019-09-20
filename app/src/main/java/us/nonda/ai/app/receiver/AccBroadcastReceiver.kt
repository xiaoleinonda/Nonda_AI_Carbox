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
            MyLog.d("广播", "action_acc_on")
            NondaApp.accStatus = true

            accOn(context!!)
            EventBus.getDefault().post(AccEvent(1))
        } else if (action_acc_off == intent?.action) {
            MyLog.d("广播", "action_acc_off ")
            NondaApp.accStatus = false

            accOff()
            EventBus.getDefault().post(AccEvent(2))
        }
    }


    /**
     * 初始化
     * 开启摄像头
     */
    private fun accOn(context: Context) {
        MyLog.d(TAG, "广播 ACC ON   accStatus=${NondaApp.accStatus} ipoStatus=${NondaApp.ipoStatus}")
        MqttManager.getInstance().publishEventData(1001, "1")
        if (NondaApp.accStatus && NondaApp.ipoStatus) {
            MyLog.d(TAG, "accOn openCamera")
            CarBoxControler.instance.openCamera(context)
        }

    }


    /**
     * acc off之后就直接关闭业务
     */
    private fun accOff() {
        MyLog.d(TAG, "广播 ACC OFF   accStatus=${NondaApp.accStatus} ipoStatus=${NondaApp.ipoStatus}  ")
        MqttManager.getInstance().publishEventData(1001, "2")
        FaceSDKManager2.instance.isRegisted = false
    }


}