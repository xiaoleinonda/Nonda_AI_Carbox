package us.nonda.ai

import android.content.Intent
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import us.nonda.ai.app.ui.VideoRecordActivity
import us.nonda.systemlibrary.location.LocationUtils

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_location.setOnClickListener {
            startActivity(Intent(this@MainActivity, VideoRecordActivity::class.java))
        }

        btn_stop_location.setOnClickListener {
            stopLocation()
        }
    }


    private fun startLocation() {
        LocationUtils.getBestLocation(this, object : LocationUtils.ILocationListener {
            override fun onSuccessLocation(location: Location?) {
                location?.run {
                    /**
                     * 可以在这个回调里 检测mqtt是否已连接， 如果未连接就 重新连接
                     */

                    Log.d("定位", "latitude=$latitude   longitude=$longitude")


                }
            }

        }, object : LocationUtils.INmeaListener {
            override fun onNmeaReceived(nmea: String?) {
                Log.d("定位", "速度=$nmea")
            }

        })

    }

    private fun stopLocation() {
        LocationUtils.unRegisterListener(this)
    }

}
