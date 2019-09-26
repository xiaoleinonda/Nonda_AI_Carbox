package us.nonda.ai.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import us.nonda.ai.controler.CarBoxControler
import us.nonda.commonibrary.MyLog
import java.util.concurrent.TimeUnit

class WakeUpService : Service() {

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private var timerDisposable: Disposable? = null
    private val TIME_IPO_ON: Long = 4

    companion object {
        var isWakeUp = false

        fun startService(context: Context) {
//            context.startService(Intent(context, WakeUpService::class.java))
        }

        fun stopService(context: Context) {
//            context.stopService(Intent(context, WakeUpService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()

    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        countDownNoticeIPO()
        return START_STICKY
    }

    private fun countDownNoticeIPO() {
        MyLog.d("服务", "开始计时休眠唤醒")
        if (timerDisposable != null && !timerDisposable!!.isDisposed) {
            timerDisposable!!.dispose()
        }
        isWakeUp = false
        timerDisposable = Observable.timer(30, TimeUnit.SECONDS)
            .observeOn(Schedulers.io())
            .subscribe {
                MyLog.d("服务", "唤醒")
                isWakeUp = true
                CarBoxControler.instance.exitIpo()
                stopSelf()
            }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (timerDisposable != null && !timerDisposable!!.isDisposed) {
            timerDisposable!!.dispose()
        }
    }

}
