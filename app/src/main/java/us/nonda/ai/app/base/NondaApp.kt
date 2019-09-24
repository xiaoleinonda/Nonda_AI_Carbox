package us.nonda.ai.app.base

import android.app.Application
import android.content.Context
import android.text.TextUtils
import us.nonda.ai.app.crash.CrashHandler
import us.nonda.ai.app.ui.ANRWatchDog
import us.nonda.ai.controler.CarBoxControler
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.DeviceUtils
import us.nonda.commonibrary.utils.SPUtils
import us.nonda.facelibrary.db.DBManager
import us.nonda.mqttlibrary.model.FaceResultBean
import us.nonda.mqttlibrary.mqtt.MqttManager
import java.io.File


class NondaApp : Application() {

    private val TAG = "NondaApp"

    companion object {
        lateinit var instance: Context
        private val SP_KEY_APP_VERSION = "sp_key_app_version"

        var accStatus: Boolean = true
        var ipoStatus: Boolean = true
    }

    override fun onCreate() {
//        ANRWatchDog().starter()
        super.onCreate()
        instance = this
        AppUtils.init(this)
        MyLog.initFile()
        MyLog.d(TAG, "onCreate")
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler.instance)
        DBManager.getInstance().init(this)
        checkVersion()
    }

    /**
     * 检查版本号，判断是否更新
     */
    private fun checkVersion() {
        val appVersion = SPUtils.get(this, SP_KEY_APP_VERSION, "")
        MyLog.d(TAG, "checkVersion appVersion=$appVersion")
        //如果不是第一次安装并且版本号不相等说明更新成功
        if (appVersion != null && AppUtils.getVersionName(this) != appVersion) {
            MyLog.d(TAG, "更新成功")

            //更新成功删除安装包
            val dirName = getExternalFilesDir(null)?.path + "/DownLoad/"
            //下载后的文件名
            val fileName = dirName + "ZUS_AI" + AppUtils.getVersionName(this) + ".apk"
            val downloadFile = File(fileName)
            if (downloadFile.exists()) {
                downloadFile.delete()
            }
            MqttManager.getInstance().publishEventData(1019, "1")

            //TODO 更新成功后的其他操作
            MyLog.d(TAG, "更新成功了versionName=${AppUtils.getVersionName(instance)}")
//            CarBoxControler.instance.onNotDownLoad()
        } else {
            MyLog.d(TAG, "没有更新")
        }
        //记录当前的版本号
        SPUtils.put(this, SP_KEY_APP_VERSION, AppUtils.getVersionName(this))
    }
}