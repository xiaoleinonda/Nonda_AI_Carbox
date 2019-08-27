package us.nonda.ai.app.base

import android.app.Application
import android.content.Context
import us.nonda.ai.app.crash.CrashHandler
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.utils.AppUtils


class NondaApp : Application() {

    private val TAG ="NondaApp"
    companion object {
        lateinit var instance: Context
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        MyLog.d(TAG,"onCreate")
//       Thread.setDefaultUncaughtExceptionHandler(CrashHandler.instance)
        AppUtils.init(this)

    }
}