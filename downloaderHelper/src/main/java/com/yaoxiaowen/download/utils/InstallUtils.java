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
    }
}
