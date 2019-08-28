package us.nonda.commonibrary.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.TelephonyManager;
import androidx.appcompat.app.AppCompatActivity;

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
        return "869455047237132";
//        return "Androidzhimengchentest";
    }


    /**
     * 获取设备序列号
     * @return
     */
    public static String getDeviceSN(){
        String serialNumber = android.os.Build.SERIAL;
        return serialNumber;
    }



    /**
     * 获取电压
     */
    public static String getCarBatteryInfo() {
        String path = "/sys/bus/platform/devices/device_info/CARBATTERYINFO";
        return StringUtils.getString(path);
    }


    /**
     * 获取sim卡iccid
     */
    @SuppressLint("MissingPermission")
   public static String getSimNumber( Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(AppCompatActivity.TELEPHONY_SERVICE);
        String simSerialNumber = telephonyManager.getSimSerialNumber();
        return simSerialNumber;

    }


}
