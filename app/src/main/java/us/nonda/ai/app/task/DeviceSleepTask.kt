package us.nonda.ai.app.task

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import us.nonda.ai.controler.CarBoxControler
import us.nonda.commonibrary.location.LocationUtils
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.mqttlibrary.model.StatusBean
import us.nonda.mqttlibrary.mqtt.MqttManager
import java.util.concurrent.TimeUnit

class DeviceSleepTask {

    var isWakeUp = false
    val TIME: Long = 1//单位 分钟

    private var wakeUpDisposable: Disposable? = null
    private var sleepDisposable: Disposable? = null
    fun onSleep() {
        stopWakeupTask()
        wakeUpDisposable = Observable.timer(TIME, TimeUnit.MINUTES)
            .doOnNext {
                isWakeUp = true
            }.subscribe {
                stopWakeupTask()
                wakeUpDevice()
            }
    }


    fun onWakeUp() {
        if (!isWakeUp) {
            return
        }
        stopSleepTask()
        sleepDisposable = Observable.interval(10, 10, TimeUnit.SECONDS)
            .doOnNext {
                reportStatus()
            }.subscribe {
                if (it == 5L) {
                    sleepDevice()
                    stopSleepTask()
                }
            }
    }

    private fun reportStatus() {
        val carBatteryInfo = CarBoxControler.instance.getCarBatteryInfo()
        val vol = java.lang.Float.valueOf(carBatteryInfo ?: "0.0")
        val bestLocation = LocationUtils.getBestLocation(AppUtils.context, null)
        val versionName = AppUtils.getVersionName(AppUtils.context)
        val latitude = (bestLocation?.latitude) ?: 0.0
        val longitude = (bestLocation?.longitude) ?: 0.0
        val accuracy = bestLocation?.accuracy
        MqttManager.getInstance()
            .publishSleepStatus(StatusBean("", versionName, latitude, longitude, accuracy, vol))

    }


    /**
     * 主动唤醒
     */
    private fun wakeUpDevice() {
        CarBoxControler.instance.exitIpo()
    }

    /**
     * 主动休眠
     */
    private fun sleepDevice() {
        if (isWakeUp) {
            CarBoxControler.instance.noticeIPO(AppUtils.context)
            isWakeUp = false
        }

    }



    private fun stopWakeupTask() {
        wakeUpDisposable?.run {
            if (!isDisposed) {
                dispose()
            }
        }
    }


    private fun stopSleepTask() {
        sleepDisposable?.run {
            if (!isDisposed) {
                dispose()
            }
        }
    }


    fun onDestroy() {
        stopWakeupTask()
        stopSleepTask()
    }
}