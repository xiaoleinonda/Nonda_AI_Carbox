package us.nonda.videopushlibrary.uploadTask;

import android.os.storage.StorageManager;
import android.util.Log;
import com.google.gson.Gson;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import kotlin.jvm.Volatile;
import okhttp3.*;
import us.nonda.cameralibrary.path.FilePathManager;
import us.nonda.cameralibrary.status.CameraStatus;
import us.nonda.commonibrary.BuildConfig;
import us.nonda.commonibrary.MyLog;
import us.nonda.commonibrary.config.CarboxConfigRepostory;
import us.nonda.commonibrary.http.BaseResult;
import us.nonda.commonibrary.http.NetModule;
import us.nonda.commonibrary.model.*;
import us.nonda.commonibrary.utils.*;
import us.nonda.mqttlibrary.mqtt.MqttManager;
import us.nonda.videopushlibrary.utlis.Md5Utlis;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.*;


public class UploadManager {

    private static final String TEMP_PATH = "/temp";

    private static final int CHUNK_SIZE = 2 * 1024 * 1024;

    private static final int THREAD_COUNT = 10;

    private static UploadManager INSTANCE;

    private ExecutorService mExecutor;

    @Volatile
    private int completeUploadFileCount = 0;

    @Volatile
    private int errorCount = 0;

    private final int MAX_ERROR_COUNT = 50;

    //已上传分片数
    private String FILE_UPLOAD_COUNT = "file_upload_count";

    //全部要上传文件数
    private int mFileSize;

    private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    private Future future;

    private UploadManager() {

    }

    public static synchronized UploadManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UploadManager();
        }
        return INSTANCE;
    }

    private boolean checkUploadValid() {
        Float carBattery = Float.valueOf(DeviceUtils.getCarBatteryInfo());
        //如果电压小于11.5V停止上传
        if (carBattery < 11.5) {
            onVideoUploadListener.onLowBattery();
            MyLog.d("分片上传", "电压过小" + carBattery);
            return false;
        }

        //acc打开
        if (CameraStatus.Companion.getInstance().getAccStatus() != 0) {
            MyLog.d("分片上传", "accon结束上传");
            return false;
        }

        return true;
    }

    public void start() {
        if (future != null && !future.isDone()) {
            return;
        }
        future = singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                MyLog.d("分片上传", "开始上传视频");
                //每次重置需要上传文件数，上传成功文件数，上传失败数
                mFileSize = 0;
                completeUploadFileCount = 0;
                errorCount = 0;
                //获取所有mp4文件
                File[] allFiles = FileUtils.traverseFolderGetMP4(FilePathManager.Companion.get().getAllVideoPath());
                //如果没有未上传的视频说明上传完毕
                if (allFiles == null || allFiles.length == 0) {
                    MyLog.d("分片上传", "没有需要上传的视频");
                    onVideoUploadListener.onVideoUploadSuccess();
                    return;
                }

                DeviceLightUtils.Companion.putLightStatus();
                DeviceLightUtils.Companion.flashWathet();
                mFileSize = allFiles.length;
                MqttManager.Companion.getInstance().publishEventData(1020, String.valueOf(mFileSize));
                //按照最后修改时间排序
                Arrays.sort(allFiles, new UploadManager.CompratorByLastModified());

                //唤醒设备
                DeviceUtils.cancelIPO();

                if (mExecutor != null) {
                    mExecutor.shutdownNow();
                }
                mExecutor = Executors.newFixedThreadPool(THREAD_COUNT);

                final CountDownLatch countDownLatch = new CountDownLatch(mFileSize);

                final int[] count = {0};
                for (final File file : allFiles) {
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (checkUploadValid()) {
                                    splitPart(file);
                                    String partFilePath = getPartDir(file) + "/" + file.getName();
                                    submitUploadTask(file, partFilePath, getPartDir(file));
                                } else {
//                                    stopUpload();
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                countDownLatch.countDown();
                                MyLog.d("分片上传", "完成countDownLatch" + countDownLatch.getCount());
                            }
                        }

                    });
                }
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                uploadAllFileComplete();
            }
        });


    }

    public void stopUpload() {
        MyLog.d("分片上传", "停止上传任务");
        if (mExecutor != null) {
            mExecutor.shutdownNow();
        }
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }

    }

    private void submitUploadTask(final File file, final String partFilePath, final String partFolderPath) {
        String fileMd5 = Md5Utlis.getMD5(file.getAbsolutePath());
        int videoType = getVideoType(file.getAbsolutePath());
        String imei = DeviceUtils.getIMEICode(AppUtils.context);
        int chunks = getChunks(file);
        //获取文件名（时间戳）
//            String createTime = file.getName().substring(0, file.getName().lastIndexOf("."));
        String createTime = String.valueOf(file.lastModified());

        final PartFileInfo partFileInfo = new PartFileInfo(imei, "", chunks, fileMd5, file.getAbsolutePath(), partFilePath, partFolderPath);
        InitPartUploadBody initPartUploadBody = new InitPartUploadBody(imei, fileMd5, file.getName(), videoType, Long.valueOf(createTime), chunks);

        String url = CarboxConfigRepostory.Companion.getInstance().getHttpUrl() + CarboxConfigRepostory.Companion.getURL_PARTUPLOAD_INIT();

        //初始化分片上传，每个file都需要初始化一次
        NetModule.Companion.getInstance().provideAPIService()
                .postInitPartUpload(url, initPartUploadBody)
                .retry(3).subscribe(new Observer<BaseResult<InitPartUploadResponseModel>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(BaseResult<InitPartUploadResponseModel> result) {
                boolean isUploaded = false;
                String uploadId = "";
                if (result.getData() != null) {
                    isUploaded = result.getData().getUploaded();
                    uploadId = result.getData().getUploadId();
                    //如果没有上传过才上传
                    if (!isUploaded) {
                        partFileInfo.setUploadId(uploadId);
                        MyLog.d("分片上传", "初始化" + uploadId);
                        uploadFile(file, partFileInfo);
                    } else {
                        File file = new File(partFileInfo.getFilePath());
                        if (file.exists()) {
                            file.delete();
                            MyLog.d("分片上传", "传完一个完整文件删除原视频(已上传)" + partFileInfo.getUploadId());
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                MyLog.d("分片上传初始化异常", e);
            }

            @Override
            public void onComplete() {
            }
        });

    }

    private int getChunks(File file) {
        long fileLength = file.length();
        int chunks = (int) ((fileLength - 1) / CHUNK_SIZE + 1);
        return chunks;
    }

    private int getVideoType(String absolutePath) {
        if (absolutePath.contains("back")) {
            return 2;
        } else if (absolutePath.contains("front")) {
            return 1;
        }
        return 2;
    }

    private String getPartFilePath(File file, int chunk) {
        String partPath = getPartDir(file)
                + "/" + file.getName() + ".part" + chunk;
        return partPath;
    }

    private String getPartDir(File file) {
        String partDir = getTempPath()
                + "/" + file.getName().replace(".mp4", "");
        File dir = new File(partDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return partDir;
    }

    private String getTempPath() {
        String tempPath = FilePathManager.Companion.get().getSdcard() + TEMP_PATH;
        return tempPath;
    }

    private void uploadFile(final File file, final PartFileInfo partFileInfo) {
        final File[] partList = getPartList(file);
        if (partList == null) return;
        MyLog.d("分片上传", file.getName() + "共有" + partList.length);
        for (int i = 1; i <= partList.length; i++) {
            final int partIndex = i;
            uploadPart(partList[partIndex - 1], partFileInfo, partIndex, partList.length);
        }
    }

    private File[] getPartList(File file) {
        String partDirPath = getPartDir(file);
        File partDir = new File(partDirPath);
        if (!partDir.exists()) {
            Log.e("upload", "part dir path not exists");
            return null;
        }

        File[] files = partDir.listFiles();
        return files;
    }

    private void splitPart(File file) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        long fileLength = file.length();
        long chunks = (fileLength - 1) / CHUNK_SIZE + 1;

        long offset = 0;
        int chunk = 1;
        while (offset < fileLength) {
            randomAccessFile.seek(offset);
            long size = offset + CHUNK_SIZE > fileLength ? (fileLength - offset) : CHUNK_SIZE;
            byte[] content = new byte[(int) size];
            randomAccessFile.read(content);

            FileOutputStream fos = new FileOutputStream(getPartFilePath(file, chunk));
            fos.write(content, 0, content.length);
            fos.flush();
            fos.close();

            offset = offset + CHUNK_SIZE;
            chunk = chunk + 1;
        }
    }

    private void uploadPart(final File part, final PartFileInfo partFileInfo, final int partIndex, final int length) {
        String partFileMd5 = Md5Utlis.getMD5(part.getAbsolutePath());
        OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build();

        MediaType type = MediaType.parse("application/octet-stream");//"text/xml;charset=utf-8"
        RequestBody requestPart = RequestBody.create(type, part);

        RequestBody fileBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", part.getName(), requestPart)
                .addFormDataPart("imei", partFileInfo.getImei())
                .addFormDataPart("uploadId", partFileInfo.getUploadId())
                .addFormDataPart("chunks", String.valueOf(partFileInfo.getChunks()))
                .addFormDataPart("chunk", String.valueOf(partIndex))
                .addFormDataPart("chunkMD5", partFileMd5)
                .addFormDataPart("fileMD5", partFileInfo.getFileMD5())
                .build();

        Request request = new Request.Builder()
                .url(CarboxConfigRepostory.Companion.getInstance().getHttpUrl() + CarboxConfigRepostory.Companion.getURL_PARTUPLOAD_UPLOAD())
                .addHeader("token", CarboxConfigRepostory.Companion.getHTTP_TOKEN())
                .post(fileBody)
                .build();

        Call call = client.newCall(request);


        Response response = null;
        try {
            response = call.execute();
            if (!response.isSuccessful()) {
                MyLog.d("分片上传上传错误", response.body().string());
            } else {
                Gson gson = new Gson();
                UploadPartResponseModel partUploadResponseModel = gson.fromJson(response.body().string(), UploadPartResponseModel.class);
                if (partUploadResponseModel.getData().getResult()) {
                    File partfile = new File(part.getAbsolutePath());
                    MyLog.d("分片上传", "传完一个分片" + partFileInfo.getUploadId() + "第" + partIndex);
                    int completeChunks = (int) SPUtils.get(AppUtils.context, FILE_UPLOAD_COUNT + partFileInfo.getUploadId(), 0);
                    completeChunks++;
                    if (completeChunks < length) {
                        SPUtils.put(AppUtils.context, FILE_UPLOAD_COUNT + partFileInfo.getUploadId(), completeChunks);
                    } else {
                        handlePostPartUploadComplete(partFileInfo);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 完成分片上传
     */
    private void handlePostPartUploadComplete(final PartFileInfo partFileInfo) {
        CompletePartUploadBody completePartUploadBody = new CompletePartUploadBody(
                partFileInfo.getImei(), partFileInfo.getUploadId(), partFileInfo.getChunks(), partFileInfo.getFileMD5());
        //上传分片完成，每个文件都要调用
        String url = CarboxConfigRepostory.Companion.getInstance().getHttpUrl() + CarboxConfigRepostory.Companion.getURL_PARTUPLOAD_COMPLETE();
        NetModule.Companion.getInstance().provideAPIService()
                .postCompletePartUpload(url, completePartUploadBody)
                .retry(3).subscribe(new Observer<BaseResult<PartUploadResponseModel>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(BaseResult<PartUploadResponseModel> result) {
                if (result.getData() != null && result.getData().getResult()) {
                    File file = new File(partFileInfo.getFilePath());
                    if (file.exists()) {
                        file.delete();
                        MyLog.d("分片上传", "传完一个完整文件删除原视频");
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                MyLog.d("分片上传", "完成时异常" + e);
            }

            @Override
            public void onComplete() {
                MyLog.d("分片上传", "完成");
                completeUploadFileCount++;
            }
        });
    }

    //判断是否上传完成，进入回调，上报结果，删除临时文件夹
    private void uploadAllFileComplete() {
        MyLog.d("分片上传", "全部上传执行完成，成功" + completeUploadFileCount + "个");
        MqttManager.Companion.getInstance().publishEventData(1021, String.valueOf(completeUploadFileCount));
        FileUtils.deleteDir(getTempPath());
        stopUpload();
        onVideoUploadListener.onVideoUploadSuccess();
    }


    public interface onVideoUploadListener {
        void onVideoUploadSuccess();

        void onLowBattery();

        void onVideoUploadFail();
    }

    public onVideoUploadListener onVideoUploadListener;

    public void setOnVideoUploadListener(onVideoUploadListener onVideoUploadListener) {
        this.onVideoUploadListener = onVideoUploadListener;
    }

    //根据文件修改时间进行比较的内部类
    static class CompratorByLastModified implements Comparator<File> {

        public int compare(File f1, File f2) {
            long diff = f1.lastModified() - f2.lastModified();
            if (diff > 0) {
                return 1;
            } else if (diff == 0) {
                return 0;
            } else {
                return -1;
            }
        }
    }
}

