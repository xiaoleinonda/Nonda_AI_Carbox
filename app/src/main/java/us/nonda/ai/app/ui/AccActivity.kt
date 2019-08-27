package us.nonda.ai.app.ui

import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import kotlinx.android.synthetic.main.activity_acc.*
import com.yaoxiaowen.download.DownloadHelper
import us.nonda.ai.R
import us.nonda.ai.controler.CarBoxControler
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.facelibrary.manager.FaceSDKManager
import us.nonda.mqttlibrary.model.FaceResultBean
import us.nonda.mqttlibrary.mqtt.MqttManager
import java.util.ArrayList
import androidx.core.os.HandlerCompat.postDelayed
import androidx.core.os.postDelayed
import androidx.core.os.HandlerCompat.postDelayed
import us.nonda.mqttlibrary.model.GPSBean


class AccActivity : AppCompatActivity() {


    private var carBoxControler: CarBoxControler? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acc)

        FaceSDKManager.instance.checkLicenceStatus()
        btn_ota.text = AppUtils.getVersionName(this)
    }


    fun onOpen(view: View) {


    }

    fun onClose(view: View) {
    }

    fun onPublish(view: View) {
        val lists = ArrayList<GPSBean>()
        for (i in 0..3000) {
            val gps = GPSBean(120.111,120.121311,3213.12f,3123.213f,121231f,System.currentTimeMillis())
            lists.add(gps)
        }
        MqttManager.getInstance().publishGPS(lists)
    }

    fun onOTA(view: View) {
        val mDownloadHelper = DownloadHelper.getInstance()
//        mDownloadHelper.addTask("https://download.zus.ai/clouddrive/vehiclebox/app/app_v1.apk", downloadFile, "")
//        mDownloadHelper.addTask("https://ali-fir-pro-binary.fir.im/b31cff6b333debb38ab49b511a43d4c5250de990.apk?auth_key=1566477346-0-0-35a369b6a5d864394b38dc6774896828", downloadFile, "")
//        mDownloadHelper.addTask("http://www.meituan.com/mobile/download/meituan/android/meituan?from=new", downloadFile, "")
//            .submit(this)
        mDownloadHelper.addCarBoxTask(this)
    }

    fun onInfinitePuiblish(view: View) {
        val lists = ArrayList<FaceResultBean>()
        val faceResultBean = FaceResultBean(1, System.currentTimeMillis())
        lists.add(faceResultBean)
        MqttManager.getInstance().publishFaceResult(lists)
//        handler.postDelayed(runnable, 1000)
    }


    var handler = Handler()
    var runnable: Runnable = object : Runnable {
        override fun run() {
            val lists = ArrayList<FaceResultBean>()
            val faceResultBean = FaceResultBean(1, System.currentTimeMillis())
            lists.add(faceResultBean)
            MqttManager.getInstance().publishFaceResult(lists)
            handler.postDelayed(this, 1000)
        }
    }

}
