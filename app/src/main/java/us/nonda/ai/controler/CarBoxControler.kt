package us.nonda.ai.controler

import android.content.Context
import android.content.Intent
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.greenrobot.eventbus.EventBus
import us.nonda.ai.app.service.SensorReportService
import us.nonda.ai.app.ui.VideoRecordActivity
import us.nonda.ai.location.LocationUtils
import us.nonda.ai.utils.SysProp
import us.nonda.cameralibrary.status.CameraStatus
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.event.ServiceEvent
import us.nonda.facelibrary.manager.FaceSDKManager
import java.util.concurrent.TimeUnit

class CarBoxControler private constructor() {

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
     */
    fun wakeUp(context: Context) {
        initConfig()
        //        startCamera(context)
        initFace()
        startLocation()
        startSensor()
    }

    private fun startLocation() {
        EventBus.getDefault().post(ServiceEvent(ServiceEvent.ACTION_GPS, ServiceEvent.OPEN))
    }

    /**
     * 切换到ACC ON 模式
     */
    fun accOnMode(context: Context) {
        if (ONING) {
            MyLog.d(TAG, "正在accOnMode 重复了")
            return
        }
        ONING = true
        wakeUp(context)
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

        if (timerDisposable != null && !timerDisposable!!.isDisposed) {
            timerDisposable!!.dispose()
        }
        timerDisposable = Observable.timer(5000, TimeUnit.MILLISECONDS)
            .subscribe({
                closeCamera()
                stopFace()
                stopSensor()
                OFFING = false
            }, {})

    }

    /**
     * 检测OTA升级
     */
    fun checkOTA() {
        //取消休眠
        cancelIPO()

        //进行升级校验


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
    private fun startCamera(context: Context) {
        if (getAccStatus() == 0) {
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
        stopFace()
    }

    private fun stopLocation() {
        EventBus.getDefault().post(ServiceEvent(ServiceEvent.ACTION_GPS, ServiceEvent.CLOSE))
    }

    /**
     * 主动进入休眠广播
     */
    fun noticeIPO(context: Context) {
        val intent = Intent("com.reacheng.action.SYNC_NOTICE_IPO")
        context.sendBroadcast(intent)
    }

    /**
     * 熄火时不进休眠控制属性值
     */
    fun cancelIPO() {
        SysProp.set("sys.need.update", "true")
    }

    /**
     * 主动唤醒休眠
     */
    fun exitIpo() = CameraStatus.instance.exitIpo()


}