package us.nonda.ai.app.base

import android.app.Application
import android.content.Context
import us.nonda.ai.app.crash.CrashHandler
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.SPUtils
import us.nonda.facelibrary.db.DBManager
import java.io.File


class NondaApp : Application() {

    private val TAG = "NondaApp"

    companion object {
        lateinit var instance: Context
        private val SP_KEY_APP_VERSION = "sp_key_app_version"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppUtils.init(this)
        DBManager.getInstance().init(this)
        MyLog.d(TAG, "onCreate")
//       Thread.setDefaultUncaughtExceptionHandler(CrashHandler.instance)
        checkVersion()

    }

    /**e
     * 检查版本号，判断是否更新
     */
    private fun checkVersion() {
        val appVersion = SPUtils.get(this, SP_KEY_APP_VERSION, "")
        //如果不是第一次安装并且版本号不相等说明更新成功
        if (appVersion != null && AppUtils.getVersionName(this) != appVersion) {
            //更新成功删除安装包
            val dirName = getExternalFilesDir(null)?.path + "/DownLoad/"
            //下载后的文件名
            val fileName = dirName + "ZUS_AI.apk"
            val downloadFile = File(fileName)
            if (downloadFile.exists()) {
                downloadFile.delete()
            }
            //TODO 更新成功后的其他操作
        }
        //记录当前的版本号
        SPUtils.put(this, SP_KEY_APP_VERSION, AppUtils.getVersionName(this))
    }
}