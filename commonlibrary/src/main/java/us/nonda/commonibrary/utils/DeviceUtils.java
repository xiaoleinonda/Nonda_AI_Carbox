package us.nonda.commonibrary.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.TelephonyManager;

public class DeviceUtils {

    /**
     * 获取IMEI号
     * @param context
     * @return
     */
    @SuppressLint("MissingPermission")
    public static String getIMEICode(Context context){
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//        return tm.getDeviceId();
        return String.valueOf(System.currentTimeMillis());
    }


    /**
     * 获取设备序列号
     * @return
     */
    public static String getDeviceSN(){
        String serialNumber = android.os.Build.SERIAL;
        return serialNumber;
    }
}
