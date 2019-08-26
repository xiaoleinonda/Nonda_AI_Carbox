package com.yaoxiaowen.download.utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.core.content.FileProvider;
import us.nonda.commonibrary.utils.AppUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class InstallUtils {
    public static void installApk(String appPath) {
        Intent intent = new Intent("android.intent.action.SILENCE_INSTALL");
        intent.putExtra("appPath", appPath);
        intent.putExtra("packageName", AppUtils.context.getPackageName());
        AppUtils.context.sendBroadcast(intent);

//        File file = (new File(appPath));
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        //如果没有设置SDCard写权限，或者没有sdcard,apk文件保存在内存中，需要授予权限才能安装
//        try {
//            String[] command = {"chmod", "777", file.toString()};
//            ProcessBuilder builder = new ProcessBuilder(command);
//            builder.start();
//        } catch (IOException ignored) {
//        }
//        intent.setDataAndType(Uri.fromFile(new File(appPath)),
//                "application/vnd.android.package-archive");
//        //为这个新apk开启一个新的activity栈
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        //开始安装
//        AppUtils.context.startActivity(intent);
//        //关闭旧版本的应用程序的进程
//        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
