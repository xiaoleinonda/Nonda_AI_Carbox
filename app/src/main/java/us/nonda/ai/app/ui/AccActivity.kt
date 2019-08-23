package us.nonda.ai.app.ui

import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import org.eclipse.paho.client.mqttv3.MqttMessage
import us.nonda.ai.R
import us.nonda.ai.app.receiver.NetStateChangeReceiver
import us.nonda.ai.controler.CarBoxControler
import us.nonda.facelibrary.db.DBManager
import us.nonda.facelibrary.manager.FaceSDKManager
import us.nonda.mqttlibrary.mqtt.MqttManager

class AccActivity : AppCompatActivity() {

    var netStateChangeReceiver: NetStateChangeReceiver? = null

    private var carBoxControler: CarBoxControler? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acc)
        registReceiver()

        FaceSDKManager.instance.checkLicenceStatus()
        MqttManager.getInstance().init()
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
//        MqttManager.getInstance().pulishGPS()
    }

    fun onOTA(view: View) {
//        this.startService(Intent(applicationContext, DownloadApkService::class.java))
//        val mIntent = Intent(applicationContext, DownloadApkService::class.java)
        //TODO
//        mIntent.putExtra(APK_DOWNLOAD_URL, "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1566454623095&di=f0558b9e8e1da0aeebcc992baf1de3ba&imgtype=0&src=http%3A%2F%2Fb-ssl.duitang.com%2Fuploads%2Fitem%2F201704%2F28%2F20170428194714_3haNw.jpeg")
//        mIntent.putExtra(APK_DOWNLOAD_URL, "https://fir.im/zusaibuild")
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
