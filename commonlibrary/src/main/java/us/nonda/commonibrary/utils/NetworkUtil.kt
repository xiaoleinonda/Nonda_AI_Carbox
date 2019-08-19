package us.nonda.commonibrary.utils

import android.content.Context
import android.net.ConnectivityManager


/**
 * Created by chenjun on 2019-06-12.
 */
object NetworkUtil {

    fun getConnectivityStatus(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        if (activeNetwork != null) {
            return activeNetwork.isConnected
        }
        return false
       /* return if (activeNetwork != null) {
            when {
                activeNetwork.type == ConnectivityManager.TYPE_WIFI -> true
                activeNetwork.type == ConnectivityManager.TYPE_MOBILE -> true
                else -> false
            }
        } else {
            false
        }*/
    }
}