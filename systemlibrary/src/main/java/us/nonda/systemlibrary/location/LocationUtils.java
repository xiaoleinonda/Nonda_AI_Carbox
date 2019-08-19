package us.nonda.systemlibrary.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.*;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class LocationUtils {

    private static final long REFRESH_TIME = 1000L;
    private static final float METER_POSITION = 0.0f;
    private static ILocationListener mLocationListener;
    private static INmeaListener mNmeaListener;

    private static MyLocationListener gpsLocationListener;

    private static class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {//定位改变监听
            Log.d("定位", "onLocationChanged: location=" +location);
            if (mLocationListener != null) {
                mLocationListener.onSuccessLocation(location);
            }

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {//定位状态监听

        }

        @Override
        public void onProviderEnabled(String provider) {//定位状态可用监听

        }

        @Override
        public void onProviderDisabled(String provider) {//定位状态不可用监听

        }
    }

    /**
     * GPS获取定位方式
     */
    @SuppressLint("MissingPermission")
    public static Location getGPSLocation( Context context) {
        Location location = null;
        LocationManager manager = getLocationManager(context);

        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {//是否支持GPS定位
            //获取最后的GPS定位信息，如果是第一次打开，一般会拿不到定位信息，一般可以请求监听，在有效的时间范围可以获取定位信息
            location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        return location;
    }

    /**
     * network获取定位方式
     */
    @SuppressLint("MissingPermission")
    public static Location getNetWorkLocation(Context context) {
        Location location = null;
        LocationManager manager = getLocationManager(context);

        if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {//是否支持Network定位
            //获取最后的network定位信息
            location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        return location;
    }

    /**
     * 获取最好的定位方式
     */
    @SuppressLint("MissingPermission")
    public static Location getBestLocation(final Context context, ILocationListener locationListener, final INmeaListener nmeaListener) {
        mLocationListener = locationListener;
        Location location;
        final LocationManager manager = getLocationManager(context);
        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_LOW);//设置低耗电
        criteria.setAltitudeRequired(true);//设置需要海拔
        criteria.setBearingAccuracy(Criteria.ACCURACY_COARSE);//设置COARSE精度标准
        criteria.setAccuracy(Criteria.ACCURACY_LOW);//设置低精度

        String provider = "gps";
        provider = manager.getBestProvider(criteria, true);



        if (TextUtils.isEmpty(provider)) {
            //如果找不到最适合的定位，使用network定位
            location = getGPSLocation(context);
            provider = LocationManager.GPS_PROVIDER;
            if (location == null) {
                provider = LocationManager.NETWORK_PROVIDER;
                location = getNetWorkLocation(context);
            }
        } else {
            //获取最适合的定位方式的最后的定位权限
            location = manager.getLastKnownLocation(provider);
        }

        Log.d("定位", "getBestLocation:location= " +location);

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
                            newLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            currentTime = timeMillis;
                        }
                    }

                    Log.d("定位", "Scheduler= " +newLocation);

*//*
                    addLocationListener(context, LocationManager.GPS_PROVIDER, locationListener);

                    addNmeaListener(manager, nmeaListener);*//*
                }
            });
        } else {

        }*/

        addLocationListener(context, provider);

        addNmeaListener(manager, nmeaListener);
        return location;
    }

    @SuppressLint("MissingPermission")
    private static void addNmeaListener(LocationManager manager, final INmeaListener nmeaListener) {
        if (manager == null) return;
        if (nmeaListener != null) {
            mNmeaListener = nmeaListener;
        }
//        manager.addNmeaListener(gpsNmeaListener);
    }

    private static GpsStatus.NmeaListener gpsNmeaListener = new GpsStatus.NmeaListener() {
        @Override
        public void onNmeaReceived(long timestamp, String nmea) {
            Log.d("gps速度", "onNmeaMessage: message= " + nmea);
            if (!TextUtils.isEmpty(nmea) && nmea.contains("GPRMC")) {
                String info[] = nmea.split(",");
                if (info != null && info.length > 7) {
                    String speed = info[8];
                    if (mNmeaListener != null) {
                        mNmeaListener.onNmeaReceived(speed);
                    }
                }
            }

        }
    };

    /**
     * 定位监听
     */
    public static void addLocationListener(Context context, String provider) {

        addLocationListener(context, provider, REFRESH_TIME, METER_POSITION);
    }

    /**
     * 定位监听
     */
    @SuppressLint("MissingPermission")
    public static void addLocationListener(Context context, String provider, long time, float meter) {
        if (gpsLocationListener == null) {
            gpsLocationListener = new MyLocationListener();
        }
        LocationManager manager = getLocationManager(context);

        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, time, meter, gpsLocationListener);
        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, time, meter, gpsLocationListener);
    }

    /**
     * 取消定位监听
     */
    public static void unRegisterListener(Context context) {
        LocationManager manager = getLocationManager(context);
        if (gpsLocationListener != null) {
            //移除定位监听
            manager.removeUpdates(gpsLocationListener);
        }

        if (gpsNmeaListener != null) {
//            manager.removeNmeaListener(gpsNmeaListener);
        }
    }

    private static LocationManager getLocationManager( Context context) {
        return (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * 自定义接口
     */
    public interface ILocationListener {
        void onSuccessLocation(Location location);
    }

    public interface INmeaListener {
        void onNmeaReceived(String nmea);
    }
}
