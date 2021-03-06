package com.yaoxiaowen.download;

import android.content.Context;
import android.content.Intent;

import com.yaoxiaowen.download.config.InnerConstant;
import com.yaoxiaowen.download.bean.DownloadInfo;
import com.yaoxiaowen.download.bean.RequestInfo;
import com.yaoxiaowen.download.service.DownloadService;
import com.yaoxiaowen.download.utils.LogUtils;
import us.nonda.commonibrary.utils.AppUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * @author www.yaoxiaowen.com
 * time:  2017/12/20 18:10
 * @since 1.0.0
 */
public class DownloadHelper {

    public static final String TAG = "DownloadHelper";

    private volatile static DownloadHelper SINGLETANCE;

    private static ArrayList<RequestInfo> requests = new ArrayList<>();

    private DownloadHelper() {
    }

    public static DownloadHelper getInstance() {
        if (SINGLETANCE == null) {
            synchronized (DownloadHelper.class) {
                if (SINGLETANCE == null) {
                    SINGLETANCE = new DownloadHelper();
                }
            }
        }

        return SINGLETANCE;
    }

    /**
     * 提交  下载/暂停  等任务.(提交就意味着开始执行生效)
     *
     * @param context
     */
    public synchronized void submit(Context context) {
        if (requests.isEmpty()) {
            LogUtils.w("没有下载任务可供执行");
            return;
        }
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra(InnerConstant.Inner.SERVICE_INTENT_EXTRA, requests);
        context.startService(intent);
        requests.clear();
    }// end of "submit(..."


    /**
     * 添加 新的下载任务
     *
     * @param url 下载的url
     * @return DownloadHelper自身 (方便链式调用)
     */
//    public DownloadHelper addTask(String url, File file, String action) {
//        RequestInfo requestInfo = createRequest(url, file, action, InnerConstant.Request.loading);
//        LogUtils.i(TAG, "addTask() requestInfo=" + requestInfo);
//
//        requests.add(requestInfo);
//        return this;
//    }
    public void addTask(Context context, String url, String appVersion) {
        //创建文件夹DownLoad，在存储卡下
        String dirName = AppUtils.context.getExternalFilesDir(null).getPath() + "/DownLoad/";
        File file = new File(dirName);
        //不存在创建
        if (!file.exists()) {
            file.mkdir();
        }
        //下载后的文件名
        String fileName = dirName + "ZUS_AI" + appVersion + ".apk";
        File downloadFile = new File(fileName);
        RequestInfo requestInfo = createRequest(url, downloadFile, "", InnerConstant.Request.loading);
        LogUtils.i(TAG, "addTask() requestInfo=" + requestInfo);

        requests.add(requestInfo);
        this.submit(context);
    }


    public void addCarBoxTask(Context context) {
        //创建文件夹DownLoad，在存储卡下
        String dirName = AppUtils.context.getExternalFilesDir(null).getPath() + "/DownLoad/";
        File file = new File(dirName);
        //不存在创建
        if (!file.exists()) {
            file.mkdir();
        }
        //下载后的文件名
        String fileName = dirName + "ZUS_AI.apk";
        File downloadFile = new File(fileName);
        String url = "https://download.zus.ai/clouddrive/vehiclebox/app/app_v1.apk";
        RequestInfo requestInfo = createRequest(url, downloadFile, "", InnerConstant.Request.loading);
        LogUtils.i(TAG, "addTask() requestInfo=" + requestInfo);

        requests.add(requestInfo);
        this.submit(context);
    }

    /**
     * 暂停某个下载任务
     *
     * @param url    下载的url
     * @param file   存储在某个位置上的文件
     * @param action 下载过程会发出广播信息.该参数是广播的action
     * @return DownloadHelper自身 (方便链式调用)
     */
    public DownloadHelper pauseTask(String url, File file, String action) {
        RequestInfo requestInfo = createRequest(url, file, action, InnerConstant.Request.pause);
        LogUtils.i(TAG, "pauseTask() -> requestInfo=" + requestInfo);
        requests.add(requestInfo);
        return this;
    }

    /**
     * 设定该模块是否输出 debug信息
     * Todo 要重构log模块, 对于我们的静态内部类，目前还不生效
     */
    private DownloadHelper setDebug(boolean isDebug) {
        LogUtils.setDebug(isDebug);
        return this;
    }


    private RequestInfo createRequest(String url, File file, String action, int dictate) {
        RequestInfo request = new RequestInfo();
        request.setDictate(dictate);
        request.setDownloadInfo(new DownloadInfo(url, file, action));
        return request;
    }

    public interface onDownloadListener {
        void onDownloadSuccess();

        void onDownloadFailure();
    }

    public onDownloadListener onDownloadListener;

    public void setOnDownloadListener(onDownloadListener onDownloadListener) {
        this.onDownloadListener = onDownloadListener;
    }
}
