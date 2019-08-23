package us.nonda.commonibrary.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager

object AppUtils {

    lateinit var context: Context

    fun init(context: Context) {
        AppUtils.context = context.applicationContext
    }

    @Synchronized
    fun getVersionName(context: Context): String? {
        try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(
                context.packageName, 0
            )
            return packageInfo.versionName
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

}