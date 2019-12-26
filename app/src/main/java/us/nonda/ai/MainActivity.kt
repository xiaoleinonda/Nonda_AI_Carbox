package us.nonda.ai

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import com.yaoxiaowen.download.utils.ToastUtils
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import us.nonda.ai.app.base.NondaApp
import us.nonda.ai.app.receiver.NetStateChangeReceiver
import us.nonda.ai.app.service.WakeUpService
import us.nonda.ai.app.ui.VideoRecordActivity
import us.nonda.ai.app.ui.debug.DebugActivity
import us.nonda.ai.controler.CarBoxControler
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.commonibrary.event.AccEvent
import us.nonda.commonibrary.event.IpoEvent
import us.nonda.commonibrary.event.ServiceEvent
import us.nonda.commonibrary.location.LocationUtils
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.DeviceLightUtils
import us.nonda.commonibrary.utils.DeviceUtils
import us.nonda.commonibrary.utils.StringUtils
import us.nonda.facelibrary.manager.FaceSDKManager2
import us.nonda.mqttlibrary.model.EmotionBean
import us.nonda.mqttlibrary.model.FaceResultBean
import us.nonda.mqttlibrary.model.GPSBean
import us.nonda.mqttlibrary.mqtt.MqttManager
import us.nonda.videopushlibrary.uploadTask.UploadManager
import java.util.ArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * 首页
 */
class MainActivity : AppCompatActivity() {
    var netStateChangeReceiver: NetStateChangeReceiver? = null

    private val TAG = "MainActivity"
    /**
     * 只会执行一次
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MyLog.d(TAG, "onCreate")
        //关闭碰撞检测
        CarBoxControler.instance.setSuspendCollision(false)

        EventBus.getDefault().register(this)
        MqttManager.getInstance().onStart()
        checkAccStatus()
        registReceiver()

        main_container.setOnLongClickListener {
            DebugActivity.starter(this)
            return@setOnLongClickListener true
        }
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

        unregisterReceiver(netStateChangeReceiver)
        EventBus.getDefault().unregister(this)
        CarBoxControler.instance.onDestroy()
        super.onDestroy()

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
        WakeUpService.isWakeUp = false
        CarBoxControler.instance.onIpoOff(this)
        unregisterReceiver(netStateChangeReceiver)
    }

    /**
     * 唤醒
     * 唤醒状态下 如果acc 是on的时候就打开业务
     */
    private fun ipoOn() {
        WakeUpService.stopService(this)
        registReceiver()
        UploadManager.getInstance().stopUpload()
        if (!NondaApp.accStatus) {//acc off
//            CarBoxControler.instance.onIpoONGetGps()
        }
    }


    /**
     * 初始化
     * 开启摄像头
     */
    private fun accOn() {
        MqttManager.getInstance().onStart()
        UploadManager.getInstance().stopUpload()
        MyLog.initSwitch()//初始化日志开关
        /*  if (NondaApp.accStatus && NondaApp.ipoStatus) {
              MyLog.d(TAG, "accOn openCamera")
              CarBoxControler.instance.openCamera(this)
          }*/
    }


    /**
     * acc off之后就直接关闭业务
     */
    private fun accOff() {
        CarBoxControler.instance.onAccOff()
    }

}
