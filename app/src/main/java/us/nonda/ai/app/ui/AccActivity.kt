package us.nonda.ai.app.ui

import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import us.nonda.ai.R
import us.nonda.ai.app.receiver.NetStateChangeReceiver
import us.nonda.ai.controler.CarBoxControler
import us.nonda.facelibrary.db.DBManager

class AccActivity : AppCompatActivity() {

    var netStateChangeReceiver: NetStateChangeReceiver? = null

    private var carBoxControler:CarBoxControler ?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acc)
        registReceiver()
        carBoxControler =   CarBoxControler(this)
    }

    private fun registReceiver() {
        netStateChangeReceiver = NetStateChangeReceiver()
        registerReceiver(netStateChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

    }

    fun onOpen(view: View) {
        carBoxControler?.mode(CarBoxControler.MODE_ACC_ON)

    }


    override fun onDestroy() {
        super.onDestroy()
        DBManager.getInstance().release()
        unregisterReceiver(netStateChangeReceiver)
    }

}
