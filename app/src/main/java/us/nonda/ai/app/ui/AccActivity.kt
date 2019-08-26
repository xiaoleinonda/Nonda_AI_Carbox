package us.nonda.ai.app.ui

import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_acc.*
import us.nonda.ai.R
import us.nonda.ai.app.receiver.NetStateChangeReceiver
import us.nonda.ai.controler.CarBoxControler
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.facelibrary.db.DBManager
import us.nonda.facelibrary.manager.FaceSDKManager
import us.nonda.mqttlibrary.model.FaceResultBean
import us.nonda.mqttlibrary.mqtt.MqttManager
import java.util.ArrayList

class AccActivity : AppCompatActivity() {

    var netStateChangeReceiver: NetStateChangeReceiver? = null

    private var carBoxControler: CarBoxControler? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acc)
        registReceiver()

        FaceSDKManager.instance.checkLicenceStatus()
        MqttManager.getInstance().init()
        btn_ota.text = AppUtils.getVersionName(this)
    }

    private fun registReceiver() {
        netStateChangeReceiver = NetStateChangeReceiver()
        registerReceiver(netStateChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

    }

    fun onOpen(view: View) {


    }

    fun onClose(view: View) {
    }

    fun onPublish(view: View) {
        val lists = ArrayList<FaceResultBean>()
        val faceResultBean = FaceResultBean(1, System.currentTimeMillis())
//        faceResultBean.face=1
//        faceResultBean.time=System.currentTimeMillis()
        lists.add(faceResultBean)
        MqttManager.getInstance().publishFaceResult(lists)
    }

    fun onOTA(view: View) {
//        val mIntent = Intent(applicationContext, DownloadApkService::class.java)
        //TODO
     /*   mIntent.putExtra(
            APK_DOWNLOAD_URL,
//            "https://ali-fir-pro-binary.fir.im/b31cff6b333debb38ab49b511a43d4c5250de990.apk?auth_key=1566477346-0-0-35a369b6a5d864394b38dc6774896828"
        "https://download.zus.ai/clouddrive/vehiclebox/app/app_v1.apk"
        )*/
//        mIntent.putExtra(Constants.APK_MD5, mUpdateInfonfo.getMD5())
//        mIntent.putExtra(Constants.APK_DIFF_UPDATE, mUpdateInfonfo.isDiffUpdate())
//        applicationContext.startService(mIntent)
    }


    override fun onDestroy() {
        super.onDestroy()
        DBManager.getInstance().release()
        unregisterReceiver(netStateChangeReceiver)
    }

}
