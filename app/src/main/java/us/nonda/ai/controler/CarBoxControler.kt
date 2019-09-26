package us.nonda.ai.controler

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import com.yaoxiaowen.download.DownloadHelper
import com.yaoxiaowen.download.DownloadHelper.onDownloadListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import us.nonda.ai.app.base.NondaApp
import us.nonda.ai.app.service.WakeUpService
import us.nonda.ai.app.service.WakeUpService.Companion.isWakeUp
import us.nonda.ai.app.ui.VideoRecordActivity
import us.nonda.commonibrary.location.LocationUtils
import us.nonda.cameralibrary.status.CameraStatus
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.SysProp
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.commonibrary.http.NetModule
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.DeviceLightUtils
import us.nonda.commonibrary.utils.DeviceUtils
import us.nonda.commonibrary.utils.StringUtils
import us.nonda.mqttlibrary.model.StatusBean
import us.nonda.mqttlibrary.mqtt.MqttManager
import us.nonda.videopushlibrary.uploadTask.UploadManager
import java.util.concurrent.TimeUnit

class CarBoxControler private constructor() : onDownloadListener, UploadManager.onVideoUploadListener {

    private val TAG = "CarBoxControler"


    private var checkVersionDisposable: Disposable? = null
//    private var batteryInfoDisposable: Disposable? = null

    /**
     * 自动唤醒的时间
     */
//    private val TIME_IPO_ON:Long = 1*1000*60*60*4
    private val TIME_IPO_ON: Long = 1 * 1000 * 60 * 30

    companion object {
        val instance: CarBoxControler by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            CarBoxControler()
        }
    }

    private var cameraDisposable: Disposable? = null

    /**
     * 进入相机页面
     */
    fun openCamera(context: Context) {
        MyLog.d(TAG, "openCamera")
        /**
         * 1是模式开启
         */
        val mode = SysProp.get("persist.calibration.mode", "-1")//自己的调试模式
        val oqcMode = SysProp.get("persist.installation.test.mode", "-1")//锐承自己的OQC模式
        MyLog.d(TAG, "openCamera  mode=$mode  oqcMode=$oqcMode")

        if (TextUtils.equals(mode, "1") || TextUtils.equals(oqcMode, "1")) {
            return
        }
        /*  MyLog.d(TAG, "openCamera  NondaApp.accStatus=${NondaApp.accStatus}  NondaApp.ipoStatus=${NondaApp.ipoStatus}")

          if (!NondaApp.accStatus || !NondaApp.ipoStatus) {
              return
          }*/

        if (cameraDisposable != null && !cameraDisposable!!.isDisposed) {
            MyLog.d(TAG, "dispose")
            cameraDisposable!!.dispose()
        }
        cameraDisposable = Observable.timer(2000, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                if (!isAccOff()) {
                    MyLog.d(TAG, "startCamera")
                    VideoRecordActivity.starter(context)
                }
            }
            .subscribe({

            }, {

            })

    }


    /**
     * 休眠时做的工作
     * 1、检测是否需有新版本 然后做下载并安装， 安装完成后重启
     * 2、无新版本时， 检测本地视频文件 有就上传视频 ， 无就进入真正的休眠 然后定时任务
     *
     */
    fun accOffModeWork() {
        cancelIPO()
        checkVersion()
    }


    /**
     * 当ACC切成OFF时 触发
     * 关闭视频录制和数据上报服务
     * 先取消休眠
     * 做休眠时工作
     */
    fun onAccOff() {
        closeCamera()
        //取消休眠
        accOffModeWork()
    }


    /**
     * 检测是否需要下载apk
     */
    private fun checkDownLoad(appVersion: String?, url: String?) {
        //如果版本号不相等，并且服务端版本号大于客户端版本号更新
        MyLog.d("是否需要下载", "当前版本号" + AppUtils.getVersionName(AppUtils.context))
        if (appVersion != AppUtils.getVersionName(AppUtils.context)) {
            MyLog.d("需要下载", "当前版本号" + AppUtils.getVersionName(AppUtils.context))

            MqttManager.getInstance().publishEventData(1018, "")
//下载
            DeviceLightUtils.putLightStatus()
            DeviceLightUtils.flashPink()
            DownloadHelper.getInstance().setOnDownloadListener(this)
            DownloadHelper.getInstance().addTask(AppUtils.context, url, appVersion)
        } else {
            onNotDownLoad()
        }
    }


/*
    */
    /**
     * 当更新成功时
     *//*

    private fun onUpdateSucceed() {
        MqttManager.getInstance().publishEventData(1019, "1")
        if (getAccStatus() == 0) {
            noticeIPO(AppUtils.context)
        }
    }
*/


    /**
     * 不需要更新时
     */
    public fun onNotDownLoad() {
        if (!isAccOff()) {
            return
        }

        checkUploadVideoFile()
    }

    /**
     * 检查是否有需要上传的视频
     */
    fun checkUploadVideoFile() {
        UploadManager.getInstance().setOnVideoUploadListener(this)
        UploadManager.getInstance().start()
    }


    /**
     * 当上传视频成功时 进入休眠
     */
    private fun onUploadVideoSucceed() {
        if (isAccOff()) {
            noticeIPO(AppUtils.context)
        } else {
            DeviceLightUtils.restoreLastLightStatus()
        }
    }

    /**
     * 当上传视频失败时
     */
    private fun onUploadVideoFailed() {

    }


    /**
     * 初始化配置和状态
     */
    private fun initConfig() {


    }


    /**
     * 关闭摄像头页面
     */
    private fun closeCamera() {
        VideoRecordActivity.finish()
    }

    /**
     * 返回acc状态
     * 1 ACC ON， 0 ACC OFF， -1 UNKNOW
     */
    fun getAccStatus() = CameraStatus.instance.getAccStatus()

    fun isAccOff(): Boolean {
        val accStatus = getAccStatus()
        MyLog.d("acc状态", "accStatus=$accStatus")
        return accStatus == 0
    }


    /**
     * 当设备休眠时会触发
     */
    fun onIpoOff(context: Context?) {
        /*if (batteryInfoDisposable != null && !batteryInfoDisposable!!.isDisposed) {
            batteryInfoDisposable!!.dispose()
        }*/
        WakeUpService.startService(context!!)
    }

    /**
     * 主动进入休眠广播
     */
    fun noticeIPO(context: Context) {
        if (CameraStatus.instance.getAccStatus() == 0) {
            MyLog.d(TAG, "主动进入休眠")
            SysProp.set("sys.need.update", "false")
            val intent = Intent("com.reacheng.action.SYNC_NOTICE_IPO")
            context.sendBroadcast(intent)
        }

    }

    /**
     * 熄火时不进休眠控制属性值
     */
    fun cancelIPO() {
        SysProp.set("sys.need.update", "true")
        MyLog.d(TAG, "取消自动休眠")
    }

    /**
     * 主动唤醒休眠
     */
    fun exitIpo() {
        if (isAccOff()) {
            CameraStatus.instance.exitIpo()
        }
    }

    /**
     * 是否打开碰撞检测
     */
    fun setSuspendCollision(boolean: Boolean){
        CameraStatus.instance.setSuspendCollision(boolean)
    }


    /**
     * 被唤醒时 获取gps
     */
    @SuppressLint("CheckResult")
    fun onIpoONGetGps() {
        if (!isWakeUp) {
            return
        }
        isWakeUp = false
        MyLog.d(TAG, "acc off下被唤醒了 开始上报GPS和电量")

        val carBatteryInfo = getCarBatteryInfo()
        val vol = java.lang.Float.valueOf(carBatteryInfo ?: "0.0")
        val bestLocation = LocationUtils.getBestLocation(AppUtils.context, null)
        val versionName = AppUtils.getVersionName(AppUtils.context)
        val latitude = (bestLocation?.latitude) ?: 0.0
        val longitude = (bestLocation?.longitude) ?: 0.0
        val accuracy = bestLocation?.accuracy
        MqttManager.getInstance()
            .publishSleepStatus(StatusBean("", versionName, latitude, longitude, accuracy, vol))
        noticeIPO(AppUtils.context)
/*
        if (gpsDisposable != null && !gpsDisposable!!.isDisposed) {
            gpsDisposable!!.dispose()
        }
        gpsDisposable = Observable.interval(0, 1, TimeUnit.SECONDS)
            .observeOn(Schedulers.io())
            .subscribe {
                val bestLocation = LocationUtils.getBestLocation(AppUtils.context, null)
                bestLocation?.run {
                    MyLog.d(TAG, "休眠状态下上报location")
                    MqttManager.getInstance().publishGPS(
                        arrayListOf(
                            GPSBean(
                                latitude,
                                longitude,
                                speed,
                                accuracy,
                                bearing,
                                System.currentTimeMillis()
                            )
                        )
                    )

                    gpsDisposable?.dispose()
                }

            }
*/


        /* if (batteryInfoDisposable != null && !batteryInfoDisposable!!.isDisposed) {
             batteryInfoDisposable!!.dispose()
         }
         batteryInfoDisposable = Observable.interval(0, 1, TimeUnit.SECONDS)
             .observeOn(Schedulers.io())
             .subscribe {
                 val carBatteryInfo = getCarBatteryInfo()
                 val vol = java.lang.Float.valueOf(carBatteryInfo ?: "0.0")
                 val bestLocation = LocationUtils.getBestLocation(AppUtils.context, null)
                 val versionName = AppUtils.getVersionName(AppUtils.context)
                 val latitude = (bestLocation?.latitude)?.toDouble() ?: -1.0
                 val longitude = (bestLocation?.longitude)?.toDouble() ?: -1.0
                 val accuracy = bestLocation?.accuracy
                 MqttManager.getInstance()
                     .publishSleepStatus(StatusBean("fw", versionName, latitude, longitude, accuracy, vol))
                 batteryInfoDisposable?.dispose()
                 noticeIPO(AppUtils.context)
             }*/


    }


    /**
     * 获取电压
     */
    fun getCarBatteryInfo(): String? {
        val path = "/sys/bus/platform/devices/device_info/CARBATTERYINFO"
        return StringUtils.getString(path)
    }


    /**
     * 获取sim卡iccid
     */
    @SuppressLint("MissingPermission")
    fun getSimNumber(context: Context): String {
        val telephonyManager = context.getSystemService(AppCompatActivity.TELEPHONY_SERVICE) as TelephonyManager
        val simSerialNumber = telephonyManager.simSerialNumber
        MyLog.d("SIM卡", "ICCID=$simSerialNumber")
        return simSerialNumber ?: ""

    }

    override fun onDownloadSuccess() {
        MyLog.d(TAG, "下载apk成功")
        DeviceLightUtils.restoreLastLightStatus()
    }

    override fun onDownloadFailure() {
        MyLog.d(TAG, "下载apk失败")
        DeviceLightUtils.restoreLastLightStatus()

//        CarBoxControler.instance.checkOTA()
//        DownloadHelper.getInstance().addCarBoxTask(AppUtils.context)
    }


    private fun checkVersion() {
        val imeiCode = DeviceUtils.getIMEICode(AppUtils.context)

        if (checkVersionDisposable != null && !checkVersionDisposable!!.isDisposed) {
            checkVersionDisposable!!.dispose()
        }

        val url = CarboxConfigRepostory.instance.getHttpUrl() + CarboxConfigRepostory.URL_APP_VERSION
        checkVersionDisposable = AppUtils.getVersionName(AppUtils.context)?.let {
            NetModule.instance.provideAPIService()
                .getAppVersion(url, imeiCode, it)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .retry(2)
                .subscribe({
                    //                    it.code = 400
                    if (it.code == 200 && it.data != null && it.data!!.updateStatus) {
                        val data = it.data
                        val appVersion = data?.appVersion
                        val url = data?.downUrl
                        checkDownLoad(appVersion, url)
                    } else {
                        onNotDownLoad()
                    }
                }, {
                    it.message?.let { it1 ->
                        onNotDownLoad()
                    }
                })
        }


    }


    override fun onVideoUploadSuccess() {
//        DeviceLightUtils.restoreLastLightStatus()
        onUploadVideoSucceed()
    }

    override fun onLowBattery() {
//        DeviceLightUtils.restoreLastLightStatus()
        noticeIPO(AppUtils.context)
        MyLog.d("分片上传", "电压过低")
    }

    override fun onVideoUploadFail() {
//        DeviceLightUtils.restoreLastLightStatus()
        noticeIPO(AppUtils.context)
        MyLog.d("分片上传", "上传失败过多")
    }


    fun onDestroy() {
        if (cameraDisposable != null && !cameraDisposable!!.isDisposed) {
            cameraDisposable!!.dispose()
        }
    }
}