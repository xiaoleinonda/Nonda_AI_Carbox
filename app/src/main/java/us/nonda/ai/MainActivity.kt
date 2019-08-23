package us.nonda.ai

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import us.nonda.ai.app.service.SensorReportService
import us.nonda.ai.controler.CarBoxControler
import us.nonda.commonibrary.MyLog

/**
 * 首页
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    /**
     * 只会执行一次
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SensorReportService.startService(this)
        MyLog.d(TAG, "onCreate")
        checkAccStatus(this)
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

    }


}
