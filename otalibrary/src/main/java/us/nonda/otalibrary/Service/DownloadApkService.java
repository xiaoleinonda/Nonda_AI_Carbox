package us.nonda.otalibrary.Service;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import us.nonda.commonibrary.utils.AppUtils;
import us.nonda.otalibrary.Utils.Constants;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadApkService extends IntentService {
    private static final String TAG = "DownloadService";

    public DownloadApkService() {
        super("DownloadService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String urlStr = intent.getStringExtra(Constants.APK_DOWNLOAD_URL);
        String md5 = intent.getStringExtra(Constants.APK_MD5);
        InputStream in = null;
        FileOutputStream out = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(false);
            urlConnection.setConnectTimeout(10 * 1000);
            urlConnection.setReadTimeout(10 * 1000);
            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.setRequestProperty("Charset", "UTF-8");
            urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");

            urlConnection.connect();
            in = urlConnection.getInputStream();

            //创建文件夹DownLoad，在存储卡下
            String dirName = AppUtils.context.getExternalFilesDir(null).getPath() + "/DownLoad/";
            File file = new File(dirName);
            //不存在创建
            if (!file.exists()) {
                file.mkdir();
            }
            //下载后的文件名
            String fileName = dirName + "船新的版本" + ".apk";
            File downloadFile = new File(fileName);
            if (downloadFile.exists()) {
                downloadFile.delete();
            }
            //创建字节流
            byte[] bs = new byte[1024];
            int len;
            OutputStream os = new FileOutputStream(fileName);
            //写数据
            while ((len = in.read(bs)) != -1) {
                os.write(bs, 0, len);
            }
            //完成后关闭流
            os.close();
            in.close();
            Log.e("run", "下载完成了~" + dirName);

            File apkFile = downloadFile;
            installAPk(apkFile);

        } catch (Exception e) {
            Log.e(TAG, "download apk file error");
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {

                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {

                }
            }
        }
    }

    private void installAPk(File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        //如果没有设置SDCard写权限，或者没有sdcard,apk文件保存在内存中，需要授予权限才能安装
        try {
            String[] command = {"chmod", "777", apkFile.toString()};
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.start();
        } catch (IOException ignored) {
        }
        intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
