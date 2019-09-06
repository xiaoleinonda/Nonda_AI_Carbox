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
import us.nonda.ai.app.receiver.NetStateChangeReceiver
import us.nonda.ai.app.ui.VideoRecordActivity
import us.nonda.ai.controler.CarBoxControler
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.DeviceUtils
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

        registReceiver()
        checkAccStatus(this)

        btn_location.setOnClickListener {
            //            CarBoxControler.instance.accOnMode(this, "首页")
            VideoRecordActivity.starter(this@MainActivity)

        }

//        val baiduDeviceId = FaceSDKManager2.instance.getBaiduDeviceId()
//        MyLog.d(TAG, "当前百度设备指纹：deviceId=$baiduDeviceId")
    }


    private fun checkAccStatus(context: Context) {
        val accStatus = CarBoxControler.instance.getAccStatus()
        if (accStatus != 0) {//acc on
            CarBoxControler.instance.openCamera(this)
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
        unregisterReceiver(netStateChangeReceiver)
        CarBoxControler.instance.onDestroy()
    }

    private fun registReceiver() {
        netStateChangeReceiver = NetStateChangeReceiver()
        registerReceiver(netStateChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }


}
