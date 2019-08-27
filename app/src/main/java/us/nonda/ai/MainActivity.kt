package us.nonda.ai

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import us.nonda.ai.app.receiver.NetStateChangeReceiver
import us.nonda.ai.app.service.SensorReportService
import us.nonda.ai.app.ui.AccActivity
import us.nonda.ai.controler.CarBoxControler
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.http.NetModule
import us.nonda.facelibrary.db.DBManager

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
        SensorReportService.startService(this)
        MyLog.d(TAG, "onCreate")
        checkAccStatus(this)
        registReceiver()
        btn_location.setOnClickListener {
            Log.d("VideoRecordActivity", "点击")
            CarBoxControler.instance.startCamera(this)
            test()
        }
        btn_stop_location.setOnClickListener { view ->
            val intent=Intent(this,AccActivity::class.java)
            startActivity(intent)
        }

        val carBatteryInfo = CarBoxControler
            .instance.getCarBatteryInfo()

        val simNumber = CarBoxControler.instance.getSimNumber(this)
        MyLog.d("设备信息", "电量=$carBatteryInfo   sim卡=$simNumber")
    }
    private fun registReceiver() {
        netStateChangeReceiver = NetStateChangeReceiver()
        registerReceiver(netStateChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

    }
    private fun test() {
        NetModule.instance.provideAPIService()
            .postLicenceSucceed("869455047237132", "111", "123")
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe({ println("成功$it")}, { println("失败${it.message}")})
    }

    private fun checkAccStatus(context: Context) {
        val accStatus = CarBoxControler.instance.getAccStatus()
        if (accStatus != 0) {//acc on
            CarBoxControler.instance.accOnMode(this, "MainActivity页面初始化")
        } else {
            CarBoxControler.instance.checkOTA()
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    override fun onDestroy() {
        MyLog.d(TAG, "onDestroy")

        super.onDestroy()
        SensorReportService.stopService(this)
        DBManager.getInstance().release()
        unregisterReceiver(netStateChangeReceiver)

    }

}
