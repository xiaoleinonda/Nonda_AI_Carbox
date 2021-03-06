package us.nonda.ai.app.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.greenrobot.eventbus.EventBus
import us.nonda.ai.MainActivity
import us.nonda.ai.R
import us.nonda.commonibrary.location.LocationUtils
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.Subscribe
import us.nonda.ai.app.sensor.CarGsensorListener
import us.nonda.ai.app.sensor.CarGyroListener
import us.nonda.ai.app.sensor.CarLocationListener
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.event.ServiceEvent


/**
 * Created by chenjun on 2019-05-27.
 */
class SensorReportService : Service() {

    companion object {
        fun bindService(context: Context, serviceConnection: ServiceConnection) {
            context.startService(Intent(context, SensorReportService::class.java))
            context.bindService(Intent(context, SensorReportService::class.java), serviceConnection, BIND_AUTO_CREATE)
        }

        fun unbindService(context: Context, serviceConnection: ServiceConnection) {
            context.stopService(Intent(context, SensorReportService::class.java))
            context.unbindService(serviceConnection)
        }
    }


    private val TAG = "SensorReportService"

    private var sensorManager: SensorManager? = null

    private var locationListener: CarLocationListener? = null

    private var gSensorListener: CarGsensorListener? = null
    private var gyroListener: CarGyroListener? = null

    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }

    override fun onCreate() {
        super.onCreate()
        EventBus.getDefault().register(this)
        /*     val builder = NotificationCompat.Builder(this, "Zus")
                 .apply {
                     setContentTitle("ZusContentTitle")
                     setContentText("ZusContentText")
                     setSubText("ZusSubText")
                     setOngoing(false)
                     val notificationIntent = Intent(this@SensorReportService, MainActivity::class.java)
                     val pendingIntent = PendingIntent.getActivity(this@SensorReportService, 0, notificationIntent, 0)
                     setContentIntent(pendingIntent)
                     setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                     setSmallIcon(R.mipmap.ic_launcher)
                 }
             startForeground(1, builder.build())*/
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

    }


    override fun onDestroy() {
        MyLog.d(TAG, "服务开启关闭")

        stopLocation()
        actionGyro(false)
        actionGSensor(false)
        super.onDestroy()

//        stopForeground(true);
        EventBus.getDefault().unregister(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MyLog.d(TAG, "服务开启成功")
        startLocation()
        actionGSensor(true)
        actionGyro(true)
        return START_NOT_STICKY
    }


    fun startLocation() {
        if (locationListener == null) {
            locationListener = CarLocationListener()
        } else {
            LocationUtils.stopLocation(this, locationListener!!)
        }
        LocationUtils.getBestLocation(this, locationListener!!)
    }

    fun stopLocation() {
        if (locationListener != null) {
            LocationUtils.stopLocation(this, locationListener!!)
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: ServiceEvent) {
        when (event.action) {
            ServiceEvent.ACTION_GPS -> {
                actionLocation(event.open)
            }
            ServiceEvent.ACTION_GSENSOR -> {
                actionGSensor(event.open)
            }
            ServiceEvent.ACTION_GYRO -> {
                actionGyro(event.open)
            }
            else -> {
            }
        }
    }

    private fun actionGyro(open: Boolean) {
        val gyro = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (open) {
            if (gyroListener == null) {
                gyroListener = CarGyroListener()
            } else {
                sensorManager?.unregisterListener(gyroListener)
            }
            sensorManager?.registerListener(gyroListener, gyro, SensorManager.SENSOR_DELAY_UI)

        } else {
            if (gyroListener != null) {
                sensorManager?.unregisterListener(gyroListener)
            }
        }
    }

    private fun actionGSensor(open: Boolean) {
        val gSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (open) {
            if (gSensorListener == null) {
                gSensorListener = CarGsensorListener()
            } else {
                sensorManager?.unregisterListener(gSensorListener)
            }
            sensorManager?.registerListener(gSensorListener, gSensor, SensorManager.SENSOR_DELAY_UI)

        } else {
            if (gSensorListener != null) {
                sensorManager?.unregisterListener(gSensorListener)
            }
        }

    }


    private fun actionLocation(open: Boolean) {
        if (open) {
            startLocation()
        } else {
            stopLocation()
        }

    }


}