package us.nonda.ai.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.*
import android.location.LocationManager
import android.text.TextUtils
import android.util.Log
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.commonibrary.utils.AppUtils

object LocationUtils {

    private val METER_POSITION: Float = 0.0f

    private fun getLocationManager(context: Context): LocationManager {
        return context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    /**
     * GPS获取定位方式
     */
    @SuppressLint("MissingPermission")
    private fun getGPSLocation(context: Context): Location? {
        var location: Location? = null
        val manager = getLocationManager(context)

        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {//是否支持GPS定位
            //获取最后的GPS定位信息，如果是第一次打开，一般会拿不到定位信息，一般可以请求监听，在有效的时间范围可以获取定位信息
            location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }
        return location
    }

    /**
     * network获取定位方式
     */
    @SuppressLint("MissingPermission")
    private fun getNetWorkLocation(context: Context): Location? {
        var location: Location? = null
        val manager = getLocationManager(context)

        if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {//是否支持Network定位
            //获取最后的network定位信息
            location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }
        return location
    }

    /**
     * 获取最好的定位方式
     */
    @SuppressLint("MissingPermission")
    fun getBestLocation(context: Context, locationListener: LocationListener): Location {
        stopLocation(context, locationListener)

        var location: Location?
        val manager = getLocationManager(context)
        val criteria = Criteria()
        criteria.powerRequirement = Criteria.POWER_LOW//设置低耗电
        criteria.isAltitudeRequired = true//设置需要海拔
        criteria.bearingAccuracy = Criteria.ACCURACY_COARSE//设置COARSE精度标准
        criteria.accuracy = Criteria.ACCURACY_LOW//设置低精度

        var provider: String? = "gps"
        provider = manager.getBestProvider(criteria, true)



        if (TextUtils.isEmpty(provider)) {
            //如果找不到最适合的定位，使用network定位
            location = getGPSLocation(context)
            provider = LocationManager.GPS_PROVIDER
            if (location == null) {
                provider = LocationManager.NETWORK_PROVIDER
                location = getNetWorkLocation(context)
            }
        } else {
            //获取最适合的定位方式的最后的定位权限
            location = manager.getLastKnownLocation(provider!!)
        }

        Log.d("定位", "getBestLocation:location= " + location!!)

        /*        if (location == null) {
            Scheduler.Worker worker = Schedulers.io().createWorker();
            worker.schedule(new Runnable() {
                @Override
                public void run() {
                    long currentTime = 0;

                    Location newLocation = null;

                    while (newLocation == null) {
                        long timeMillis = System.currentTimeMillis();
                        if (timeMillis - currentTime > 5000) {
                            newLocation = manager.getLastKnownLocation(LocationUtils.GPS_PROVIDER);
                            currentTime = timeMillis;
                        }
                    }

                    Log.d("定位", "Scheduler= " +newLocation);

*//*
                    addLocationListener(context, LocationUtils.GPS_PROVIDER, locationListener);

                    addNmeaListener(manager, nmeaListener);*//*
                }
            });
        } else {

        }*/

        if (locationListener != null) {
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                CarboxConfigRepostory.instance.gpsCollectFreq,
                METER_POSITION,
                locationListener
            )
            manager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                CarboxConfigRepostory.instance.gpsCollectFreq,
                METER_POSITION,
                locationListener
            )
        }


        return location
    }

    fun stopLocation(context: Context, locationListener: LocationListener) {
        val manager = getLocationManager(context)
        if (locationListener != null) {
            //移除定位监听
            manager.removeUpdates(locationListener)
        }
    }

}