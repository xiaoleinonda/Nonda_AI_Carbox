package us.nonda.cameralibrary.camera

import android.annotation.SuppressLint
import android.content.Context
import android.media.CamcorderProfile
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.mediatek.carcorder.CameraDevice
import com.mediatek.carcorder.CameraInfo
import com.mediatek.carcorder.CarcorderManager
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import us.nonda.ai.cache.CameraConfig
import us.nonda.cameralibrary.model.PictureModel
import us.nonda.cameralibrary.path.FilePathManager
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.utils.FileUtils

abstract class CarboxCameraManager : SurfaceHolder.Callback {

    companion object {
        const val STATUS_NOT_RECORD = 0
        const val STATUS_RECORDING = 1
    }

    val TAG = "相机"
    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
        surfaceCreated = false
    }

    override fun surfaceCreated(p0: SurfaceHolder?) {
        if (!isPreviewed) {
            startPreview(p0)
            surfaceCreated = true
        }
    }

    var status: Int = 0


    private var isPreviewed = false
    private var surfaceCreated = false

    private var surfaceHolder: SurfaceHolder? = null

    var cameraCallback: CameraCallback? = null

    private var cameraDevice: CameraDevice? = null

    private var previewWidth: Int = 0

    private var previewHeight: Int = 0

    private var rotation: Int = 0
    private var video_duration_ms: Int = 0

    private var videoFileName: String = ""
    private var videoFrameRate: Int = 0
    private var videoSizeLimi: Int = 0
    private var previewFrameRate: Int = 0
    private var videoBitRate: Int = 0
    private var video_record_quality: Int = CamcorderProfile.QUALITY_480P


    private var subscribe: Disposable? = null

    private var isConvertYUV = false


    var pictureProcessor: PublishProcessor<PictureModel> = PublishProcessor.create()
    var pictureFrontProcessor: PublishProcessor<PictureModel> = PublishProcessor.create()
    var pictureFaceProcessor: PublishProcessor<PictureModel> = PublishProcessor.create()


    @SuppressLint("CheckResult")
    fun initCamera(surfaceView: SurfaceView, yuvData: Boolean, cameraType: Int, callback: CameraCallback) {


        if (subscribe?.isDisposed == false) {
            subscribe?.dispose()
        }
        subscribe = pictureProcessor.subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .distinctUntilChanged { t: us.nonda.cameralibrary.model.PictureModel ->
                t.emotion
            }
            .doOnNext {
                var folderPath = FilePathManager.get().getBackEmotionPictureFolderPath() + it.emotion + "/"
                FileUtils.saveBitmapToSDCard(it.argb, it.width, it.height, folderPath, it.fileName)
            }
            .doOnNext {
                val folderPath = FilePathManager.get().getFrontEmotionPictureFolderPath() + it.emotion + "/"
                takePicture(folderPath, it.fileName)
            }.subscribe({ Log.d("图片", "保存情绪图片成功") }, {})

        pictureFrontProcessor.subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .distinctUntilChanged { t: us.nonda.cameralibrary.model.PictureModel ->
                t.emotion
            }
            .doOnNext {
                val folderPath = FilePathManager.get().getFrontEmotionPictureFolderPath() + it.emotion + "/"
                takePicture(folderPath, it.fileName)
            }.subscribe({ Log.d("图片", "保存情绪图片成功") }, {})


        pictureFaceProcessor.subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .distinctUntilChanged { t: us.nonda.cameralibrary.model.PictureModel ->
                t.emotion
            }
            .doOnNext {
                var folderPath = FilePathManager.get().getFacePictureFolderPath() + it.emotion + "/"
                FileUtils.saveBitmapToSDCard(it.argb, it.width, it.height, folderPath, it.fileName)

            }.subscribe({ Log.d("图片", "保存人脸图片成功") }, {})


        this.cameraCallback = callback
        isPreviewed = false
        surfaceCreated = false

        initConfig(surfaceView.context, cameraType)
        CarcorderManager.get().watermarkImageOpaque = false

        initPreview(surfaceView)

        val cameraID = getCameraID(CarcorderManager.get(), cameraType)

        openCamera(cameraID, yuvData)

    }


    /**
     * 初始化属性配置
     */
    private fun initConfig(context: Context?, cameraType: Int) {

        val cameraConfig = CameraConfig.get(context!!.applicationContext)

        previewWidth = CameraConfig.WIDTH
        previewHeight = CameraConfig.HEIGHT
        rotation = cameraConfig.rotation
        video_duration_ms = cameraConfig.video_duration_ms
        videoSizeLimi = cameraConfig.videoSizeLimi
        videoBitRate = cameraConfig.videoBitRate
        video_record_quality = when (cameraConfig.video_record_quality) {
            480 -> {
                CamcorderProfile.QUALITY_480P
            }
            720 -> {
                CamcorderProfile.QUALITY_720P
            }
            1080 -> {
                CamcorderProfile.QUALITY_1080P
            }
            else -> {
                CamcorderProfile.QUALITY_480P
            }
        }

        when (cameraType) {
            CameraInfo.CAMERA_USB_CAMERA -> {
                videoFileName = FilePathManager.get().getBackVideoPath()
                videoFrameRate = cameraConfig.videoFrameRateBack
                previewFrameRate = cameraConfig.videoFrameRateFront
                isConvertYUV = true
            }
            else -> {
                videoFileName = FilePathManager.get().getFrontVideoPath()
                videoFrameRate = cameraConfig.videoFrameRateFront
                previewFrameRate = cameraConfig.videoFrameRateFront
                isConvertYUV = false
            }
        }
    }


    private fun initPreview(surfaceView: SurfaceView) {
        surfaceView?.run {
            surfaceHolder = holder
            holder.addCallback(this@CarboxCameraManager)
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
            setZOrderMediaOverlay(true)
//            surfaceView.invalidate()
        }
    }

    /**
     * 获取指定摄像头的ID
     *
     * @param manager
     * @param cameraType 指定的摄像头
     * CameraInfo.CAMERA_MAIN_SENSOR : 主摄像头
     * CameraInfo.CAMERA_SUB_SENSOR : cvbs 摄像头
     * CameraInfo.CAMERA_CVBS_DUAL_SENSOR : dual cvbs 摄像头
     * CameraInfo.CAMERA_USB_CAMERA : usb 摄像头
     * @return 如果没有cameraType摄像头 返回任意一个
     */
    fun getCameraID(manager: CarcorderManager, cameraType: Int): Int {
        var cameraID = -1
        try {
            val numberOfCameras = manager.numberOfCameras
            if (numberOfCameras < 0) {
                return cameraID
            }
            for (i in 0 until numberOfCameras) {
                val cameraInfo = CameraInfo()
                val cameraInfoResult = manager.getCameraInfo(i, cameraInfo)
                if (cameraInfoResult == 0) {
                    cameraID = i
                    if (cameraInfo.type == cameraType) {
                        return cameraID
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return cameraID
        }

        return cameraID
    }


    /**
     * 打开相机
     */
    fun openCamera(cameraId: Int, yuvData: Boolean) {
        try {
            cameraDevice = CarcorderManager.get().openCameraDevice(cameraId)
            MyLog.d(TAG, "摄像头打开成功")

        } catch (e: java.lang.Exception) {
            cameraCallback?.onOpenCameraFaile("Open Camera Failed")
            MyLog.d(TAG, "摄像头打开失败")

            e.printStackTrace()
        }
        cameraDevice!!.setRecordingMuteAudio(false)

        val parameters = cameraDevice!!.getParameters()

        setPreviewSize(
            parameters.supportedPreviewSizes,
            previewWidth,
            previewHeight
        )

        parameters.setPreviewSize(previewWidth, previewHeight)

        parameters.setCameraId(cameraId)
        parameters.setVideoRotateDuration(video_duration_ms)
        parameters.setVideoRotation(rotation)

        parameters.setYUVCallbackType(CameraDevice.YUVCallbackType.yuvCBAndRecord)//录制同时 获取yuv数据

        parameters.setOutputFile(videoFileName)//录制的保存路径
        val profile = CamcorderProfile.get(cameraId, video_record_quality)
        profile.videoFrameRate = videoFrameRate//usb摄像头设置15帧
        profile.videoBitRate = videoBitRate
        parameters.setVideoProfile(profile) //video audio 编码器，帧率等设置 params.setMainVideoFrameMode(VideoFrameMode.DISABLE) ; //保存为视频文件
        parameters.setRecordingHint(true)

        parameters.setFreeSizeLimit(videoSizeLimi)//设置视频长度

        parameters.setMainVideoFrameMode(CameraDevice.VideoFrameMode.DISABLE) //保存为视频文件
        parameters.setOutputFileFormat(CameraDevice.OutputFormat.MPEG_4)

        if (yuvData) {
            cameraDevice!!.setYuvCallback { bytes, i, i1 ->
                //                cameraCallback?.onYuvCbFrame(bytes)
                Log.d(TAG, "转换前数据=" + bytes.size)
                cameraCallback?.onYuvCbFrame(bytes, previewWidth, previewHeight)

//                publishProcessor?.onNext(bytes)

            }
        }

        parameters.previewFrameRate = previewFrameRate//usb摄像头设置15帧
        cameraDevice!!.setParameters(parameters)


        if (!isPreviewed && surfaceCreated && surfaceHolder != null) {
            startPreview(surfaceHolder!!)
        }

        cameraCallback?.onOpenCameraSucceed()
    }


    /**
     * 选择合适的像素尺寸
     *
     * @param supportedPreviewSizes 相机支持的所有像素
     * @param width                 预想的像素宽
     * @param height                预想的像素高
     *                              <p>
     *                              百度好像预览像素不能超过1000*1000（待确认）
     *                              <p>
     *                              前置摄像头参数：1280-720  640-480
     */
    private fun setPreviewSize(supportedPreviewSizes: List<CameraDevice.Size>, width: Int?, height: Int?) {
        val widthConfig = width
        val heightConfig = height
        if (supportedPreviewSizes != null && supportedPreviewSizes.isNotEmpty()) {
            for (supportedPreviewSize in supportedPreviewSizes) {
                previewWidth = supportedPreviewSize.width
                previewHeight = supportedPreviewSize.height
                if (previewWidth == widthConfig && heightConfig == height) {
                    return
                }
            }
        }

    }


    /**
     * 开启预览和录制
     */
    private fun startPreview(holder: SurfaceHolder?) {
        if (holder == null) return
        cameraDevice?.run {
            setPreviewSurface(holder.surface)
            startPreview()
            startYuvVideoFrame(CameraDevice.YUVFrameType.yuvPreviewFrame)
            isPreviewed = true
            this@CarboxCameraManager.startRecord()
        }
    }

    fun startRecord() {
        if (cameraDevice?.state == 2) {
            cameraDevice?.stopRecord()
        }

        val record = cameraDevice?.startRecord()
        when (record) {
            null -> {
                cameraCallback?.onRecordFailed(-100)
            }
            0 -> {

                cameraCallback?.onRecordSucceed()
            }
            else -> {
                cameraCallback?.onRecordFailed(record)
            }
        }
        MyLog.d(TAG, "录制结果=" + record)

//            startRecordResult(record)
    }

//    abstract fun startRecordResult(record: Int)

    /**
     * 关闭摄像头和录制
     */
    fun closeCamera() {
        cameraDevice?.run {
            stopRecord()
            stopYuvVideoFrame(CameraDevice.YUVFrameType.yuvPreviewFrame)
            setYuvCallback(null)
            release()
            if (isPreviewed) {
                stopPreview()
            }

            if (subscribe?.isDisposed == false) {
                subscribe?.dispose()
            }
        }

    }

    protected fun takePicture(path: String, callback: CameraDevice.CamPictureCallback) {
        cameraDevice?.takePicture(path, CameraDevice.ShutterCallback { }, callback)
    }

    fun log(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    /**
     * @param yuv422
     * @param width
     * @param height
     * @return
     */
    private fun yuv422To420(yuv422: ByteArray, width: Int, height: Int): ByteArray {
        val len = width * height
        //yuv格式数组大小，y亮度占len长度，u,v各占len/4长度。
        val yuv = ByteArray(len * 3 / 2)

        val y = 0

        var index_y = 0
        var index_u = 0

        var is_u = true

        for (i in 0 until height * 2) {
            var j = 0
            while (j < width) {
                yuv[y + index_y++] = yuv422[width * i + j]
                yuv[y + index_y++] = yuv422[width * i + j + 2]
                j = j + 4
            }
        }

        var i = 0
        while (i < height) {
            val base = i * width * 2
            var j = base + 1
            while (j < base + width * 2) {
                if (is_u) {
                    yuv[len + index_u++] = yuv422[j]
                    is_u = false
                } else {
                    yuv[len + index_u++] = yuv422[j]
                    is_u = true
                }
                j = j + 2

            }
            i = i + 2
        }

        return yuv
    }

    fun getWidth() = previewWidth
    fun geHeight() = previewHeight


    fun takePicture(folder: String, pictureName: String) {
        if (cameraDevice == null) return
        val path = "$folder$pictureName.jpeg"
        cameraDevice!!.takePicture(path, CameraDevice.ShutterCallback { },
            CameraDevice.PictureCallback {
                Log.d("back takePicture图片", "path=$it");
            })

    }
}