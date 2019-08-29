package us.nonda.ai.controler

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.yaoxiaowen.download.DownloadHelper
import com.yaoxiaowen.download.DownloadHelper.onDownloadListener
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import us.nonda.ai.app.ui.VideoRecordActivity
import us.nonda.commonibrary.location.LocationUtils
import us.nonda.ai.utils.SysProp
import us.nonda.cameralibrary.status.CameraStatus
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.event.ServiceEvent
import us.nonda.commonibrary.http.NetModule
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.facelibrary.manager.FaceSDKManager
import us.nonda.commonibrary.utils.StringUtils
import us.nonda.mqttlibrary.model.GPSBean
import us.nonda.mqttlibrary.model.StatusBean
import us.nonda.mqttlibrary.mqtt.MqttManager
import java.util.concurrent.TimeUnit

class CarBoxControler private constructor() : onDownloadListener {

    private val TAG = "CarBoxControler"
    private var timerDisposable: Disposable? = null

    private var ONING = false
    private var OFFING = false

    companion object {
        val instance: CarBoxControler by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            CarBoxControler()
        }
    }


    /**
     * 唤醒应用
     * 打开摄像头页面
     * 开启gps和sensor
     * check face
     *
     */
    fun wakeUp(context: Context, msg: String) {
        MyLog.d(TAG, "wakeUp 唤醒应用    $msg")
        initConfig()
        startCamera(context)
//        initFace()
        startLocation()
        startSensor()
    }

    private fun startLocation() {
        EventBus.getDefault().post(ServiceEvent(ServiceEvent.ACTION_GPS, ServiceEvent.OPEN))
    }

    /**
     * 切换到ACC ON 模式
     *
     */
    fun accOnMode(context: Context, msg: String) {
        if (ONING) {
            MyLog.d(TAG, "正在accOnMode 重复了")
            return
        }
        ONING = true
        wakeUp(context, msg)
        ONING = false
    }

    /**
     * 切换到ACC OFF 模式
     * 延迟5秒进行任务
     */
    fun accOffMode() {
        if (OFFING) {
            MyLog.d(TAG, "正在accOffMode 重复了")
            return
        }
        OFFING = true
        cancelSleep()
        checkOTA()

        /*   if (timerDisposable != null && !timerDisposable!!.isDisposed) {
               timerDisposable!!.dispose()
           }
           timerDisposable = Observable.timer(5000, TimeUnit.MILLISECONDS)
               .subscribe({
                   closeCamera()
                   stopFace()
                   stopSensor()
                   OFFING = false
               }, {})
   */
    }

    /**
     * 检测OTA升级
     */
    fun checkOTA() {
         sleep()

        //取消休眠
        cancelIPO()

        MyLog.d(TAG, "开始OTA")

        NetModule.instance.provideAPIService()
            .getAppVersion("869455047237132")
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .retry(2)
            .subscribe({
                if (it.code == 200 && it.data != null && it.data!!.isUpdate) {
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

/*        var dis = Observable.interval(0, 1, TimeUnit.SECONDS)
            .subscribe {
                if (it == 10L) {
                    MyLog.d(TAG, "OTA结束")
                    //进行升级校验
                    if (getAccStatus() == 0) {
                        noticeIPO(AppUtils.context)
                    }
                }
            }*/


    }

    /**
     * 检测是否需要下载apk
     */
    private fun checkDownLoad(appVersion: String?, url: String?) {
        if (appVersion != AppUtils.getVersionName(AppUtils.context)) {
            DownloadHelper.getInstance().addTask(AppUtils.context, url)
            DownloadHelper.getInstance().setOnDownloadListener(this)
        } else {
            onNotDownLoad()
        }
    }

    private fun downloadApk(url: String) {
        MqttManager.getInstance().publishEventData(1018, "")
    }

    private fun onDownloadApkSucceed() {
        updateApp("")

    }


    private fun updateApp(path: String) {
        Observable.timer(10000, TimeUnit.MILLISECONDS, Schedulers.newThread())
            .subscribe {
                onUpdateSucceed()
            }

    }


    /**
     * 当更新成功时
     */
    private fun onUpdateSucceed() {
        MqttManager.getInstance().publishEventData(1019, "1")
        if (getAccStatus() == 0) {
            noticeIPO(AppUtils.context)
        }
    }


    /**
     * 不需要更新时
     */
    private fun onNotDownLoad() {
        if (getAccStatus() == 0) {
            noticeIPO(AppUtils.context)
        }
    }

    /**
     * 停止sensor
     */
    private fun stopSensor() {
        EventBus.getDefault().post(ServiceEvent(ServiceEvent.ACTION_GSENSOR, ServiceEvent.CLOSE))
        EventBus.getDefault().post(ServiceEvent(ServiceEvent.ACTION_GYRO, ServiceEvent.CLOSE))
    }

    /**
     * 停止人脸检测
     */
    private fun stopFace() {
        FaceSDKManager.instance.stop()
    }


    /**
     * 取消休眠
     */
    private fun cancelSleep() {


    }


    /**
     * 打开服务
     * 感应器上报
     * gps上报
     */
    private fun startSensor() {
        EventBus.getDefault().post(ServiceEvent(ServiceEvent.ACTION_GSENSOR, ServiceEvent.OPEN))
        EventBus.getDefault().post(ServiceEvent(ServiceEvent.ACTION_GYRO, ServiceEvent.OPEN))
    }


    /**
     * 初始化人脸
     */
    private fun initFace() {
        FaceSDKManager.instance.check()
    }


    /**
     * 初始化配置和状态
     */
    private fun initConfig() {


    }

    /**
     * 进入相机页面
     */
    fun startCamera(context: Context) {
        MyLog.d(TAG, "startCamera")
        if (getAccStatus() == 0) {
            MyLog.d(TAG, "startCamera acc off 不开启摄像头")

            return
        }
        VideoRecordActivity.starter(context)
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


    /**
     * 休眠应用
     */
    fun sleep() {
        closeCamera()
        stopSensor()
        stopLocation()
//        stopFace()
    }

    private fun stopLocation() {
        EventBus.getDefault().post(ServiceEvent(ServiceEvent.ACTION_GPS, ServiceEvent.CLOSE))
    }

    /**
     * 主动进入休眠广播
     */
    fun noticeIPO(context: Context) {
        MyLog.d(TAG, "主动进入休眠")
        val intent = Intent("com.reacheng.action.SYNC_NOTICE_IPO")
        context.sendBroadcast(intent)
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
    fun exitIpo() = CameraStatus.instance.exitIpo()


    private var time: Long = 0
    private var gpsDisposable: Disposable? = null
    private var batteryInfoDisposable: Disposable? = null


    /**
     * 休眠时 开始计时唤醒
     */
    @SuppressLint("CheckResult")
    fun onIpoON() {
        MyLog.d(TAG, "acc off下被唤醒了 开始上报GPS和电量")

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


        if (batteryInfoDisposable != null && !batteryInfoDisposable!!.isDisposed) {
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
            }


    }

    fun countDownNoticeIPO() {
        if (timerDisposable != null && !timerDisposable!!.isDisposed) {
            timerDisposable!!.dispose()
        }
        timerDisposable = Observable.interval(0, 1, TimeUnit.SECONDS)
            .observeOn(Schedulers.io())
            .subscribe {
                if (it == 120L) {
                    MyLog.d(TAG, "2分钟了开始唤醒设备")

                    exitIpo()
                    timerDisposable?.dispose()
                }
            }

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
        return simSerialNumber?:""

    }

    override fun onDownloadSuccess() {
        //TODO 下载成功
    }

    override fun onDownloadFailure() {
        //TODO 下载失败
    }

}