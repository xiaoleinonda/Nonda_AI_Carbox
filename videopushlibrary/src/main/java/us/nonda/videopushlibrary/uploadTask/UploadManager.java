package us.nonda.videopushlibrary.uploadTask;

import android.content.Context;
import android.os.storage.StorageManager;
import android.util.Log;
import com.google.gson.Gson;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.*;
import org.json.JSONObject;
import us.nonda.cameralibrary.path.FilePathManager;
import us.nonda.commonibrary.MyLog;
import us.nonda.commonibrary.http.BaseResult;
import us.nonda.commonibrary.http.NetModule;
import us.nonda.commonibrary.model.*;
import us.nonda.commonibrary.utils.AppUtils;
import us.nonda.commonibrary.utils.DeviceUtils;
import us.nonda.commonibrary.utils.FileUtils;
import us.nonda.commonibrary.utils.SPUtils;
import us.nonda.videopushlibrary.utlis.Md5Utlis;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class UploadManager {

    private static final String VIDEO_PATH = "/zusai";

    private static final String TEMP_PATH = "/temp";

    private static final int CHUNK_SIZE = 2 * 1024 * 1024;

    private static final int THREAD_COUNT = 3;

    private static UploadManager INSTANCE;

    private ExecutorService mExecutor;

    private int completeUploadFileCount = 0;

    private int errorCount = 0;

    private final int MAX_ERROR_COUNT = 50;

    //已上传分片数
    private String FILE_UPLOAD_COUNT = "file_upload_count";

    //全部要上传文件数
    private int mFileSize;

    private UploadManager() {

    }

    public static synchronized UploadManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UploadManager();
        }
        return INSTANCE;
    }

    //    @Override
    public void start() {
        File[] allFiles = getAllFileList();
        mFileSize = allFiles.length;
        MyLog.d("分片上传", "总共上传" + mFileSize);
        //如果没有未上传的视频说明上传完毕
        if (mFileSize == 0) {
            onVideoUploadListener.onVideoUploadSuccess();
            return;
        }
        //按照最后修改时间排序
        Arrays.sort(allFiles, new UploadManager.CompratorByLastModified());
        mExecutor = Executors.newFixedThreadPool(THREAD_COUNT);
        for (final File file : allFiles) {
            try {
                splitPart(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    String partFilePath = getPartDir(file) + "/" + file.getName();
                    submitUploadTask(file, partFilePath, getPartDir(file));
                }

            });
        }
    }

    public void stopUpload() {
        if (mExecutor != null) {
            mExecutor.shutdown();
        }
    }

    private void submitUploadTask(final File file, final String partFilePath, final String partFolderPath) {
        Float carBattery = Float.valueOf(DeviceUtils.getCarBatteryInfo());
        //如果电压小于11.5V停止上传
        if (carBattery < 11.5) {
            onVideoUploadListener.onLowBattery();
            MyLog.d("分片上传", "电压过小" + carBattery);
            return;
        }

        String fileMd5 = Md5Utlis.getMD5(file.getAbsolutePath());
        int videoType = getVideoType(file.getAbsolutePath());
        String imei = DeviceUtils.getIMEICode(AppUtils.context);
        int chunks = getChunks(file);
        //获取文件名（时间戳）
//            String createTime = file.getName().substring(0, file.getName().lastIndexOf("."));
        String createTime = String.valueOf(file.lastModified());

        final PartFileInfo partFileInfo = new PartFileInfo(imei, "", chunks, fileMd5, file.getAbsolutePath(), partFilePath, partFolderPath);
        InitPartUploadBody initPartUploadBody = new InitPartUploadBody(imei, fileMd5, file.getName(), videoType, Long.valueOf(createTime), chunks);

        //初始化分片上传，每个file都需要初始化一次
        NetModule.Companion.getInstance().provideAPIService()
                .postInitPartUpload(initPartUploadBody)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
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
                        uploadFile(file, partFileInfo);
                        MyLog.d("分片上传", "初始化");
                    } else {
                        File file = new File(partFileInfo.getFilePath());
                        if (file.exists()) {
                            file.delete();
                            MyLog.d("分片上传", "传完一个完整文件删除原视频");
                            uploadAllFileComplete();
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                MyLog.d("分片上传", e);
                needFinishUpload();
            }

            @Override
            public void onComplete() {

            }
        });

    }

    private void needFinishUpload() {
        errorCount++;
        if (errorCount > MAX_ERROR_COUNT) {
            onVideoUploadListener.onVideoUploadFail();
        }
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


    /**
     * 获取前后摄像头然后合并成一个文件数组
     */
    private File[] getAllFileList() {
        String videoBackVideoPath = FilePathManager.Companion.get().getBackVideoPath();
        String videoFrontVideoPath = FilePathManager.Companion.get().getFrontVideoPath();
        File[] allBackFiles = getFileListByPath(videoBackVideoPath);
        File[] allFrontFiles = getFileListByPath(videoFrontVideoPath);
        int frontFileSize = allFrontFiles == null ? 0 : allFrontFiles.length;
        int backFileSize = allBackFiles == null ? 0 : allBackFiles.length;

        File[] allFiles = new File[backFileSize + frontFileSize];
        if (allBackFiles != null) {
            System.arraycopy(allBackFiles, 0, allFiles, 0, backFileSize);
        }

        if (allFrontFiles != null) {
            System.arraycopy(allFrontFiles, 0, allFiles, backFileSize, frontFileSize);
        }
        return allFiles;
    }


    private File[] getFileList() {
        String videoFullPath = getVideoPath();
        File videoDir = new File(videoFullPath);
        if (!videoDir.exists()) {
            Log.e("upload", "video path not exists");
            return null;
        }

        File[] files = videoDir.listFiles();
        return files;
    }

    private File[] getFileListByPath(String path) {
        File videoDir = new File(path);
        if (!videoDir.exists()) {
            Log.e("upload", "video path not exists");
            return null;
        }

        File[] files = videoDir.listFiles();
        return files;
    }

    private String getVideoPath() {
        String videoFullPath = getSdPath() + VIDEO_PATH;
        return videoFullPath;
    }

    private String getPartFilePath(File file, int chunk) {
        String partPath = getPartDir(file)
                + "/" + file.getName() + ".part" + chunk;
        return partPath;
    }

    private String getPartDir(File file) {
        String partDir = getTempPath(file)
                + "/" + file.getName().replace(".mp4", "");
        File dir = new File(partDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return partDir;
    }

    private String getTempPath(File file) {
        String tempPath = FilePathManager.Companion.get().getSdcard() + TEMP_PATH;
        return tempPath;
    }

    private String getSdPath() {
        String[] paths = null;
        try {
            paths = getAllExtPaths();
        } catch (Exception e) {
            Log.e("error", e.getMessage());
            return "";
        }
        if (paths == null || paths.length == 0) {
            return "";
        }
        if (paths.length == 1) {
            return paths[0];
        }
        return paths[1];
    }

    private String[] getAllExtPaths() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        StorageManager storageManager = (StorageManager) AppUtils.context.getSystemService(AppUtils.context.STORAGE_SERVICE);

        Method getVolumePathsMethod = StorageManager.class.getMethod("getVolumePaths");
        getVolumePathsMethod.setAccessible(true);
        Object result = getVolumePathsMethod.invoke(storageManager);

        return (String[]) result;
    }


    private void uploadFile(final File file, final PartFileInfo partFileInfo) {
        final File[] partList = getPartList(file);
        if (partList == null) return;

        ExecutorService uploadPartFileExecutor = Executors.newFixedThreadPool(partList.length);

        for (int i = 1; i <= partList.length; i++) {
            final int partIndex = i;
            uploadPartFileExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    uploadPart(partList[partIndex - 1], partFileInfo, partIndex, partList.length);
                    MyLog.d("分片上传", file.getName() + "第" + partIndex + "片开始上传");
                }
            });
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
//        List<File> partFileList = new ArrayList<>();
//        for (File part : files) {
//            if (!part.getName().contains(file.getName()))
//                continue;
//            }
//            partFileList.add(part);
//
//        }
//        return (File[]) partFileList.toArray();

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
//                .url("http://10.0.0.90:8081" + "/api/v1/vehiclebox/partupload/upload")
                .url("https://api-clouddrive-qa.zus.ai" + "/api/v1/vehiclebox/partupload/upload")
                .addHeader("token", "7c09b979489a4bca8684c0922bb8a0e7")
                .post(fileBody)
                .build();

        MyLog.d("分片上传", "上传分片" + partIndex);

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                needFinishUpload();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Gson gson = new Gson();
                UploadPartResponseModel partUploadResponseModel = gson.fromJson(response.body().string(), UploadPartResponseModel.class);
                if (partUploadResponseModel.getData().getResult()) {
                    File partfile = new File(part.getAbsolutePath());
                    if (partfile.exists()) {
                        partfile.delete();
                        MyLog.d("分片上传", "传完一个分片删除一个");
                    }
                    int completeChunks = (int) SPUtils.get(AppUtils.context, FILE_UPLOAD_COUNT + partFileInfo.getUploadId(), 0);
                    completeChunks++;
                    if (completeChunks < length) {
                        SPUtils.put(AppUtils.context, FILE_UPLOAD_COUNT + partFileInfo.getUploadId(), completeChunks);
                    } else {
                        handlePostPartUploadComplete(partFileInfo);
                    }
                }
            }
        });
    }

    /**
     * 完成分片上传
     */
    private void handlePostPartUploadComplete(final PartFileInfo partFileInfo) {
        CompletePartUploadBody completePartUploadBody = new CompletePartUploadBody(
                partFileInfo.getImei(), partFileInfo.getUploadId(), partFileInfo.getChunks(), partFileInfo.getFileMD5());
        //上传分片完成，每个文件都要调用
        NetModule.Companion.getInstance().provideAPIService()
                .postCompletePartUpload(completePartUploadBody)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .retry(3).subscribe(new Observer<BaseResult<PartUploadResponseModel>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(BaseResult<PartUploadResponseModel> result) {
                if (result.getData() != null && result.getData().getResult()) {
                    File partfile = new File(partFileInfo.getPartFolderPath());
                    if (partfile.exists()) {
                        partfile.delete();
                        MyLog.d("分片上传", "传完一个完整文件删除临时文件夹");
                    }
                    File file = new File(partFileInfo.getFilePath());
                    if (file.exists()) {
                        file.delete();
                        MyLog.d("分片上传", "传完一个完整文件删除原视频");
                        uploadAllFileComplete();
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                MyLog.d("分片上传", "完成时异常" + e);
                needFinishUpload();
            }

            @Override
            public void onComplete() {
                MyLog.d("分片上传", "完成");
            }
        });
    }

    //判断是否上传完成，进入回调
    private void uploadAllFileComplete() {
        completeUploadFileCount++;
        if (completeUploadFileCount >= mFileSize) {
            onVideoUploadListener.onVideoUploadSuccess();
            MyLog.d("分片上传", "全部上传完成");
        }
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

