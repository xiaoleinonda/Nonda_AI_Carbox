package us.nonda.ai.app.sensor

import android.location.Location
import android.location.LocationListener
import android.os.Bundle

class CarLocationListener : LocationListener {
    override fun onLocationChanged(p0: Location?) {
//            Log.d(TAG, "location")
    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
    }

    override fun onProviderEnabled(p0: String?) {
    }

    override fun onProviderDisabled(p0: String?) {
    }

}

