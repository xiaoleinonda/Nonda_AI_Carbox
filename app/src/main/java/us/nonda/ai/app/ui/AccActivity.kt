package us.nonda.ai.app.ui

import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import us.nonda.ai.MqttTestManager
import us.nonda.ai.R
import us.nonda.ai.app.receiver.NetStateChangeReceiver
import us.nonda.ai.controler.CarBoxControler
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.status.CarboxCacheManager
import us.nonda.commonibrary.utils.FinishActivityManager
import us.nonda.facelibrary.db.DBManager
import us.nonda.facelibrary.manager.FaceSDKManager
import us.nonda.mqttlibrary.mqtt.MqttManager

class AccActivity : AppCompatActivity() {

    var netStateChangeReceiver: NetStateChangeReceiver? = null

    private val TAG = "AccActivity"
    private var carBoxControler: CarBoxControler? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acc)
//        registReceiver()
        MyLog.d(TAG, "onCreate")
        CarBoxControler.instance.accOnMode(this)
//        initCarbox()
    }

/*    private fun initCarbox() {
        if (CarboxCacheManager.instance.isAccOn()) {
            initConfig()
            checkFace()
            initCamera()
        } else {
            stopFace()
            stopCamera()
            checkOTA()
        }

    }*/

    override fun onResume() {
        super.onResume()
        MyLog.d(TAG, "onResume")

    }

    private fun initConfig() {


    }

    private fun checkOTA() {


    }

    private fun stopCamera() {
        FinishActivityManager.getManager().finishActivity(VideoRecordActivity::class.java)
    }

    private fun stopFace() {
        FaceSDKManager.instance.stop()
    }

    private fun initCamera() {
        VideoRecordActivity.starter(this)

    }

    private fun checkFace() {
        FaceSDKManager.instance.check()
    }

    private fun registReceiver() {
        netStateChangeReceiver = NetStateChangeReceiver()
        registerReceiver(netStateChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

    }

    fun onOpen(view: View) {
//        carBoxControler?.mode(CarBoxControler.MODE_ACC_ON)
//        MqttTestManager.getInstance().onStart()


    }

    fun onClose(view: View) {
//        carBoxControler?.mode(CarBoxControler.MODE_ACC_OFF)
//        MqttTestManager.getInstance().onStop()

    }

    fun onPublish(view: View) {
        MqttManager.getInstance().publish("测试publish")
    }


    override fun onDestroy() {
        MyLog.d(TAG, "onDestroy")

        super.onDestroy()
//        DBManager.getInstance().release()
        unregisterReceiver(netStateChangeReceiver)
    }

}
