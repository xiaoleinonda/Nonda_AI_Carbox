package us.nonda.commonibrary.utils

import android.content.Context

object AppUtils {

    lateinit var context:Context

    fun init(context: Context) {
        AppUtils.context = context.applicationContext
    }
}