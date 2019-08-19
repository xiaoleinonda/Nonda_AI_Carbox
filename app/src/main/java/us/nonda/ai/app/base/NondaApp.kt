package us.nonda.ai.app.base

import android.app.Application
import android.content.Context
import us.nonda.commonibrary.utils.AppUtils


class NondaApp : Application() {

    companion object {
        lateinit var instance: Context
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        AppUtils.init(this)

    }
}