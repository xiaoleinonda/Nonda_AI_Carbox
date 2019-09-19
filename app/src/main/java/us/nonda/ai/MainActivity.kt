package us.nonda.ai

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.yaoxiaowen.download.utils.ToastUtils
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import us.nonda.ai.app.base.NondaApp
import us.nonda.ai.app.receiver.NetStateChangeReceiver
import us.nonda.ai.app.service.WakeUpService
import us.nonda.ai.app.ui.VideoRecordActivity
import us.nonda.ai.controler.CarBoxControler
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.event.AccEvent
import us.nonda.commonibrary.event.IpoEvent
import us.nonda.commonibrary.event.ServiceEvent
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.DeviceLightUtils
import us.nonda.commonibrary.utils.DeviceUtils
import us.nonda.commonibrary.utils.StringUtils
import us.nonda.facelibrary.manager.FaceSDKManager2
import us.nonda.mqttlibrary.model.EmotionBean
import us.nonda.mqttlibrary.model.FaceResultBean
import us.nonda.mqttlibrary.mqtt.MqttManager
import us.nonda.videopushlibrary.uploadTask.UploadManager
import java.util.ArrayList

/**
 * 首页
 */
class MainActivity : AppCompatActivity() {
    var netStateChangeReceiver: NetStateChangeReceiver? = null

    private val TAG = "MainActivity"
    private var count = 0
    /**
     * 只会执行一次
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MyLog.d(TAG, "onCreate")

        EventBus.getDefault().register(this)
        MqttManager.getInstance().onStart()
        checkAccStatus()
        registReceiver()
    }



    private fun checkAccStatus() {
//        val ipoStatus = DeviceUtils.getIpoStatus()
        val accOf = CarBoxControler.instance.isAccOff()
        if (!accOf) {//acc on
//            if (!ipoStatus) {
                //未休眠时开启业务
                CarBoxControler.instance.openCamera(this)
                UploadManager.getInstance().stopUpload()
//            }
        } else {
            CarBoxControler.instance.accOffModeWork()
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    override fun onDestroy() {
        MyLog.d(TAG, "onDestroy")
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        CarBoxControler.instance.onDestroy()
    }

    private fun registReceiver() {
        if (netStateChangeReceiver == null) {
            netStateChangeReceiver = NetStateChangeReceiver()
        } else {
            unregisterReceiver(netStateChangeReceiver)
        }
        registerReceiver(netStateChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: AccEvent) {
        when (event.status) {
            1 -> {
                accOn()
            }
            2 -> {
                accOff()
            }

            else -> {
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: IpoEvent) {
        when (event.status) {
            1 -> {
                ipoOn()
            }
            2 -> {
                ipoOff()
            }

            else -> {
            }
        }
    }

    /**
     * 休眠
     */
    private fun ipoOff() {
        val ipoStatus = DeviceUtils.getIpoStatus()

        val accOff = CarBoxControler.instance.isAccOff()

        MyLog.d(TAG, "休眠 isAccOff=$accOff  accStatus=${NondaApp.accStatus} ipoStatus=${NondaApp.ipoStatus} DeviceIPO=$ipoStatus")

        WakeUpService.isWakeUp = false
        CarBoxControler.instance.onIpoOff(this)
        unregisterReceiver(netStateChangeReceiver)
    }

    /**
     * 唤醒
     * 唤醒状态下 如果acc 是on的时候就打开业务
     */
    private fun ipoOn() {
        val ipoStatus = DeviceUtils.getIpoStatus()

        val accOff = CarBoxControler.instance.isAccOff()
        MyLog.d(TAG, "唤醒 isAccOff=$accOff  accStatus=${NondaApp.accStatus} ipoStatus=${NondaApp.ipoStatus} DeviceIPO=$ipoStatus")
        if (!NondaApp.accStatus) {//acc off
            CarBoxControler.instance.onIpoONGetGps()
        }


        WakeUpService.stopService(this)
        registReceiver()

        CarBoxControler.instance.openCamera(this)
        UploadManager.getInstance().stopUpload()


    }


    /**
     * 初始化
     * 开启摄像头
     */
    private fun accOn() {
        val accOff = CarBoxControler.instance.isAccOff()
        val ipoStatus = DeviceUtils.getIpoStatus()

        MyLog.d(TAG, "ACC ON isAccOff=$accOff  accStatus=${NondaApp.accStatus} ipoStatus=${NondaApp.ipoStatus} DeviceIPO=$ipoStatus")

        MqttManager.getInstance().publishEventData(1001, "1")
        CarBoxControler.instance.openCamera(this)

//        CarBoxControler.instance.openCamera(this)
//        UploadManager.getInstance().stopUpload()
    }


    /**
     * acc off之后就直接关闭业务
     */
    private fun accOff() {
        val ipoStatus = DeviceUtils.getIpoStatus()
        val accOff = CarBoxControler.instance.isAccOff()
        MyLog.d(TAG, "ACC OFF isAccOff=$accOff  accStatus=${NondaApp.accStatus} ipoStatus=${NondaApp.ipoStatus} DeviceIPO=$ipoStatus")

        MqttManager.getInstance().publishEventData(1001, "2")
        FaceSDKManager2.instance.isRegisted = false
        CarBoxControler.instance.onAccOff()
    }

}
