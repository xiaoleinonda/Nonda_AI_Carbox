package us.nonda.ai

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import us.nonda.ai.app.service.SensorReportService
import us.nonda.ai.controler.CarBoxControler

/**
 * 首页
 */
class MainActivity : AppCompatActivity() {

    /**
     * 只会执行一次
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SensorReportService.startService(this)

        checkAccStatus(this)
        CarBoxControler.instance.wakeUp(this)

    }

    private fun checkAccStatus(context:Context) {
        val accStatus = CarBoxControler.instance.getAccStatus()
        if (accStatus != 0) {//acc on
            CarBoxControler.instance.wakeUp(context!!)
        } else {
            CarBoxControler.instance.checkOTA()
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        SensorReportService.stopService(this)

    }


}
