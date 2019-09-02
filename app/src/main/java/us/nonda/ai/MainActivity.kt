package us.nonda.ai

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import us.nonda.ai.app.receiver.NetStateChangeReceiver
import us.nonda.ai.app.ui.VideoRecord2Activity
import us.nonda.ai.app.ui.VideoRecordActivity
import us.nonda.ai.controler.CarBoxControler
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.videopushlibrary.uploadTask.UploadManager

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
        MyLog.d(TAG, "setContentView")
        setContentView(R.layout.activity_main)

        registReceiver()
//        checkAccStatus(this)
//        FaceSDKManager2.instance.init()

//        SensorReportService.startService(this)
        MyLog.d(TAG, "onCreate")
        checkAccStatus(this)

        btn_location.setOnClickListener {
//            CarBoxControler.instance.accOnMode(this, "首页")
            it.postDelayed({
                VideoRecord2Activity.starter(this@MainActivity)

            }, 5000)
        }
        btn_stop_location.setOnClickListener {
//            CarBoxControler.instance.sleep()
//            FaceSDKManager.instance.isRegisted = false
//            VideoRecordActivity.starter(this@MainActivity)
//            test()
            //分片上传视频文件
            UploadManager.getInstance().start()
        }

        val carBatteryInfo = CarBoxControler
            .instance.getCarBatteryInfo()

        val simNumber = CarBoxControler.instance.getSimNumber(this)
        MyLog.d("设备信息", "电量=$carBatteryInfo   sim卡=$simNumber")

        tv_version.text = AppUtils.getVersionName(this)
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

    }

    private fun registReceiver() {
        netStateChangeReceiver = NetStateChangeReceiver()
        registerReceiver(netStateChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }


}
