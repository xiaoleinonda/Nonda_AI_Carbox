package us.nonda.systemlibrary.wifi

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.net.ConnectivityManager
import android.os.ResultReceiver
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import android.net.wifi.WifiConfiguration


object CarboxWifiManager {

    const val PASSWORD = "nondawifi"

    @SuppressLint("MissingPermission")
    fun setWifiApEnabled(context: Context, ssid: String, enabled: Boolean): Boolean {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (enabled) {
            wifiManager.isWifiEnabled = false
        }

        if (Build.VERSION.SDK_INT >= 26) {
            setWifiApEnabledForAndroidO(context, enabled)
            return true
        }


        try {
            // 热点的配置类
            val apConfig = WifiConfiguration()
            // 配置热点的名称(可以在名字后面加点随机数什么的)
            apConfig.SSID = ssid
            apConfig.preSharedKey = PASSWORD
            apConfig.allowedKeyManagement.set(4)//设置加密类型，这里4是wpa加密

            val method = wifiManager.javaClass.getMethod(
                "setWifiApEnabled",
                WifiConfiguration::class.java,
                java.lang.Boolean.TYPE
            )
            // 返回热点打开状态
            return method.invoke(wifiManager, apConfig, enabled) as Boolean
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return false

    }

    /**
     * 8.0 开启热点方法
     * 注意：这个方法开启的热点名称和密码是手机系统里面默认的那个
     * @param context
     */
    private fun setWifiApEnabledForAndroidO(context: Context, isEnable: Boolean) {
        val connManager =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        var iConnMgrField: Field? = null
        try {
            iConnMgrField = connManager.javaClass.getDeclaredField("mService")
            iConnMgrField!!.setAccessible(true)
            val iConnMgr = iConnMgrField!!.get(connManager)
            val iConnMgrClass = Class.forName(iConnMgr.javaClass.getName())

            if (isEnable) {
                val startTethering = iConnMgrClass.getMethod(
                    "startTethering",
                    Int::class.javaPrimitiveType,
                    ResultReceiver::class.java,
                    Boolean::class.javaPrimitiveType
                )
                startTethering.invoke(iConnMgr, 0, null, true)
            } else {
                val startTethering = iConnMgrClass.getMethod("stopTethering", Int::class.javaPrimitiveType)
                startTethering.invoke(iConnMgr, 0)
            }

        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }

    }


}