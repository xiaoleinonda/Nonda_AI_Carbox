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
//        checkAccStatus(this)
//        FaceSDKManager2.instance.init()

//        SensorReportService.startService(this)
        checkAccStatus(this)

        btn_location.setOnClickListener {
            //            CarBoxControler.instance.accOnMode(this, "首页")
            VideoRecordActivity.starter(this@MainActivity)

        }
        btn_stop_location.setOnClickListener {
            //            CarBoxControler.instance.sleep()
//            FaceSDKManager.instance.isRegisted = false
//            VideoRecordActivityTest.starter(this@MainActivity)
//            test()
            //分片上传视频文件
//            UploadManager.getInstance().start()
            publishMessage()
        }

        val carBatteryInfo = DeviceUtils.getCarBatteryInfo()

        val simNumber = CarBoxControler.instance.getSimNumber(this)
        MyLog.d("设备信息", "电量=$carBatteryInfo   sim卡=$simNumber")

        tv_version.text = AppUtils.getVersionName(this)
    }

    private fun publishMessage() {
        count++
        val lists = ArrayList<EmotionBean>()
        val faceResultBean = EmotionBean("发送次数=$count", System.currentTimeMillis())
        lists.add(faceResultBean)
        MqttManager.getInstance().publishEmotion(lists)
        handler.postDelayed(runnable, 1000)
    }


    var handler = Handler()
    var runnable: Runnable = object : Runnable {
        override fun run() {
            count++
            val lists = ArrayList<EmotionBean>()
            val faceResultBean = EmotionBean("发送次数=$count", System.currentTimeMillis())
            lists.add(faceResultBean)
            MqttManager.getInstance().publishEmotion(lists)
            handler.postDelayed(this, 2000)
        }
    }

    private fun test() {
        checkAccStatus(this)
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
