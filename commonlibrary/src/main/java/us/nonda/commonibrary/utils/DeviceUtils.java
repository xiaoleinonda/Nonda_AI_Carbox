package us.nonda.commonibrary.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import androidx.appcompat.app.AppCompatActivity;
import us.nonda.commonibrary.MyLog;
import us.nonda.commonibrary.SysProp;

public class DeviceUtils {

    /**
     * 获取IMEI号
     *
     * @param context
     * @return
     */
    @SuppressLint("MissingPermission")
    public static String getIMEICode(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getDeviceId();
    }


    /**
     * 获取设备序列号
     *
     * @return
     */
    public static String getDeviceSN() {
        String serialNumber = android.os.Build.SERIAL;
        return serialNumber;
    }


    /**
     * 获取电压
     */
    public static String getCarBatteryInfo() {
        String path = "/sys/bus/platform/devices/device_info/CARBATTERYINFO";
        if (TextUtils.isEmpty(path)) {
            return "0";
        }
        try {
            return StringUtils.getString(path);
        } catch (Exception e) {
            return "0";
        }
    }


    /**
     * 获取sim卡iccid
     */
    @SuppressLint("MissingPermission")
    public static String getSimNumber(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(AppCompatActivity.TELEPHONY_SERVICE);
        String simSerialNumber = telephonyManager.getSimSerialNumber();
        if (TextUtils.isEmpty(simSerialNumber)) {
            return "";
        }
        return simSerialNumber;

    }

    /**
     * 获取sim卡状态
     * @param context
     * @return
     */
    public static int getSimState(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(AppCompatActivity.TELEPHONY_SERVICE);
        return telephonyManager.getSimState();
    }

    /**
     * 熄火时不进休眠控制属性值
     */
    public static void cancelIPO() {
        SysProp.set("sys.need.update", "true");
        MyLog.d("上传视频取消休眠", "取消自动休眠");
    }


    /**
     * true是休眠
     * false是未休眠
     *
     * @return
     */
    public static boolean getIpoStatus() {
        String ipo = SysProp.get("sys.ipo.shutdown", "0");
        return TextUtils.equals("1", ipo);
    }


}
