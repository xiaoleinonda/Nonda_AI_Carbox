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
import us.nonda.commonibrary.http.NetModuleTest;
import us.nonda.commonibrary.model.*;
import us.nonda.commonibrary.utils.AppUtils;
import us.nonda.commonibrary.utils.DeviceUtils;
import us.nonda.commonibrary.utils.FileUtils;
import us.nonda.commonibrary.utils.SPUtils;
import us.nonda.videopushlibrary.utlis.Md5Utlis;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class UploadThread extends Thread {
    private Context context;

    private static final String VIDEO_PATH = "/zusai";

    private static final String TEMP_PATH = "/temp";

    private static final int CHUNK_SIZE = 2 * 1024 * 1024;

    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors() + 1;

    private ExecutorService mExecutor;

    //已上传分片数
    private String FILE_UPLOAD_COUNT = "file_upload_count";


    public UploadThread(Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        File[] allFiles = getAllFileList();
        mExecutor = Executors.newFixedThreadPool(THREAD_COUNT);
//        for (final File file : allFiles) {
        final File file = allFiles[1];
        try {
            splitPart(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                submitUploadTask(file);
            }
        });
//        }
    }

    private void submitUploadTask(final File file) {
        String fileMd5 = Md5Utlis.getMD5(file.getAbsolutePath());
        int videoType = getVideoType(file.getAbsolutePath());
        String imei = DeviceUtils.getIMEICode(AppUtils.context);
        int chunks = getChunks(file);
        //获取文件名（时间戳）
//            String createTime = file.getName().substring(0, file.getName().lastIndexOf("."));
        String createTime = String.valueOf(System.currentTimeMillis());

        final PartFileInfo partFileInfo = new PartFileInfo(imei, "", chunks, fileMd5, file.getAbsolutePath());
        InitPartUploadBody initPartUploadBody = new InitPartUploadBody(imei, fileMd5, file.getName(), videoType, Long.valueOf(createTime), chunks);

        //初始化分片上传，每个file都需要初始化一次
        NetModuleTest.Companion.getInstance().provideAPIService()
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
                        uploadTask(file, partFileInfo);
                        MyLog.d("分片上传", "初始化");
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                MyLog.d("分片上传", e);
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

    private void uploadTask(File file, PartFileInfo partFileInfo) {
        uploadFile(file, partFileInfo);
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
        StorageManager storageManager = (StorageManager) context.getSystemService(context.STORAGE_SERVICE);

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
                .url("http://10.0.0.90:8081" + "/api/v1/vehiclebox/partupload/upload")
                .addHeader("token", "7c09b979489a4bca8684c0922bb8a0e7")
                .post(fileBody)
                .build();

        MyLog.d("分片上传", "上传分片" + partIndex);

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                uploadPart(part, partFileInfo, partIndex, length);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Gson gson = new Gson();
                UploadPartResponseModel partUploadResponseModel = gson.fromJson(response.body().string(), UploadPartResponseModel.class);
                if (partUploadResponseModel.getData().isResult()) {
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
        //上传分片完成，每个分片都要调用
        NetModuleTest.Companion.getInstance().provideAPIService()
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
//                    handlePostPartUploadComplete(partFileInfo);
                    File file = new File(partFileInfo.getFilePath());
//                    if (file.exists()) {
//                        file.delete();
//                        MyLog.d("分片上传", "传完一个完整文件删除原视频");
//                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                MyLog.d("分片上传", "完成时异常" + e);
            }

            @Override
            public void onComplete() {
                MyLog.d("分片上传", "完成");
            }
        });
    }
}

