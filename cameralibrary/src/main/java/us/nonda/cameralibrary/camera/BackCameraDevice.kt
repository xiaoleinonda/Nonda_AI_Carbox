package us.nonda.cameralibrary.camera

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
import us.nonda.cameralibrary.model.PictureModel
import us.nonda.cameralibrary.path.FilePathManager
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.utils.FileUtils
import us.nonda.mqttlibrary.mqtt.MqttManager

class BackCameraDevice constructor(private var surfaceView: SurfaceView) : SurfaceHolder.Callback {

//    private var subscribeEmotion: Disposable? = null
//    private var subscribeFace: Disposable? = null

    private val TAG = "USB摄像头"

    /***       CONFIG               ***/
    private var width = 640
    private var height = 480

    private var videoDurationMS = 1000 * 60 * 1
    private var rotation = 0
    private var videoFilePathName = FilePathManager.get().getBackVideoPath()
    private var videoQuality: Int = CamcorderProfile.QUALITY_480P
    private var videoFrameRate: Int = 15
    private var videoBitRate: Int = 1 * 500 * 1000
    private var videoSizeLimi: Int = 300//M
    private var videoFileName: String = "back_"

    private var cameraDevice: CameraDevice? = null
    private var cameraID: Int = -1

    private var isPreviewed = false
    private var surfaceCreated = false


    private var cameraCallback: CameraCallback? = null

//    var pictureProcessor: PublishProcessor<PictureModel> = PublishProcessor.create()
//    var pictureFaceProcessor: PublishProcessor<PictureModel> = PublishProcessor.create()

    private var cameraAvailableCallback: CarcorderManager.CameraAvailableCallback? = null

    fun camera(callback: CameraCallback) {
        cameraCallback = callback
        val surfaceHolder = initPreview()

        if (cameraAvailableCallback == null) {
            cameraAvailableCallback = object : CarcorderManager.CameraAvailableCallback {
                override fun onAvailable(cameraid: Int, status: Int) {
                    MyLog.d(TAG, "热插拔：p0=$cameraid  p1=$status")
                    if (cameraid == 1) {
                        when (status) {
                            CarcorderManager.CameraAvailableCallback.STATUS_CAMERA_ADDED -> {
                                initCamera()
                                if (!isPreviewed && surfaceCreated) {
                                    _startPreview(surfaceHolder)
                                }
                            }
                            CarcorderManager.CameraAvailableCallback.STATUS_CAMERA_REMOVED -> {
                                closeCamera()
                            }
                            else -> {
                            }
                        }

                    }
                }

            }

        }
        CarcorderManager.get().addCameraAvailableCallback(cameraAvailableCallback)

       /* if (subscribeEmotion?.isDisposed == false) {
            subscribeEmotion?.dispose()
        }

        subscribeEmotion = pictureProcessor.subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .distinctUntilChanged { t: us.nonda.cameralibrary.model.PictureModel ->
                t.emotion
            }
            .doOnNext {
                var folderPath = FilePathManager.get().getBackEmotionPictureFolderPath() + it.emotion + "/"
                FileUtils.saveBitmapToSDCard(it.argb, it.width, it.height, folderPath, it.fileName)
            }.subscribe({ Log.d("图片", "保存情绪图片成功") }, {})

        if (subscribeFace?.isDisposed == false) {
            subscribeFace?.dispose()
        }
        subscribeFace = pictureFaceProcessor.subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .distinctUntilChanged { t: us.nonda.cameralibrary.model.PictureModel ->
                t.emotion
            }
            .doOnNext {
                var folderPath = FilePathManager.get().getFacePictureFolderPath() + it.emotion + "/"
                FileUtils.saveBitmapToSDCard(it.argb, it.width, it.height, folderPath, it.fileName)

            }.subscribe({ Log.d("图片", "保存人脸图片成功") }, {})
*/
    }


    private fun initPreview(): SurfaceHolder {
        return surfaceView.run {
            holder.addCallback(this@BackCameraDevice)
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
            setZOrderMediaOverlay(true)
            holder
        }
    }

    private fun initCamera() {
        val cameraID = getCameraID(CarcorderManager.get(), CameraInfo.CAMERA_USB_CAMERA)
        MyLog.d(TAG, "cameraID=$cameraID")
        val cameraDevice = openCamera(cameraID)

        if (cameraDevice == null) {
            MyLog.d(TAG, "摄像头打开失败")
            cameraCallback?.onOpenCameraFaile("摄像头打开失败 cameraDevice=$cameraDevice")
            return
        }
        cameraCallback?.onOpenCameraSucceed()
        MqttManager.getInstance().publishEventData(1005, "1")

        this.cameraDevice = cameraDevice
        this.cameraID = cameraID
        //录制声音
        cameraDevice.setRecordingMuteAudio(false)

        initParameters(cameraDevice, cameraID);

        cameraDevice.setYuvCallback { bytes, i, i1 ->
            cameraCallback?.onYuvCbFrame(bytes, width, height)

        }

        cameraDevice?.setRecordStatusCallback(object : CameraDevice.RecordStatusCallback {
            override fun onRecordStatusChanged(p0: Int, p1: Int) {
                MyLog.d(TAG, "录制状态 recordStatus=$p0  cameraID=$p1")
            }

        })


    }

    private fun _startPreview(surfaceHolder: SurfaceHolder) {
        cameraDevice?.run {
            setPreviewSurface(surfaceHolder.surface)
            startPreview()
            startYuvVideoFrame(CameraDevice.YUVFrameType.yuvPreviewFrame)
            isPreviewed = true
            _startRecord()

            /*  if (getCameraStatus(cameraID) == CameraDevice.STATE_IDLE) {
                  startPreview()
                  val cameraStatus = getCameraStatus(cameraID)
                  MyLog.d(TAG, "预览后 cameraStatus=$cameraStatus")
              }

              startYuvVideoFrame(CameraDevice.YUVFrameType.yuvPreviewFrame)
              isPreviewed = true



              _startRecord()*/

            /* if (cameraStatus == CameraDevice.STATE_PREVIEW || cameraStatus == CameraDevice.STATE_RECORDING) {
                 _startRecord()
             }*/
        }

    }

    private var isFirst = true

    private fun restartPreview(surfaceHolder: SurfaceHolder) {
        cameraDevice?.run {
            setPreviewSurface(surfaceHolder.surface)
            val cameraStatus1 = getCameraStatus(cameraID)
            MyLog.d(TAG, "rrestartPreview开始预览 cameraStatus=$cameraStatus1")

            if (cameraStatus1 == CameraDevice.STATE_IDLE) {
                startPreview()
                val cameraStatus2 = getCameraStatus(cameraID)
                MyLog.d(TAG, "restartPreview预览结束  cameraStatus=$cameraStatus2")
            }

            startYuvVideoFrame(CameraDevice.YUVFrameType.yuvPreviewFrame)
            isPreviewed = true
        }

    }

    private fun _startRecord() {
        /*   val cameraStatus = getCameraStatus(cameraID)
           if (cameraStatus == CameraDevice.STATE_RECORDING) {
               MyLog.d("相机", "USB发现正在录制开时 retrue cameraStatus=$cameraStatus 开始关闭录制重新打开")

               cameraDevice?.stopRecord()

   //            return
           }*/
        val record = cameraDevice?.startRecord()

        val cameraStatus2 = getCameraStatus(cameraID)

        MyLog.d(TAG, "开启录制结束 record=$record  cameraStatus=$cameraStatus2")
        when (record) {
            null -> {
                cameraCallback?.onRecordFailed(-100)
            }
            0 -> {
                recording = true
                cameraCallback?.onRecordSucceed()
            }
            else -> {
                if (isFirst) {
                    cameraDevice?.stopRecord()
                    MyLog.d(TAG, "摄像头录制失败record=$record， 已关闭录制status=${getCameraStatus(cameraID)}  重新开启录制")
                    isFirst = false
                    _startRecord()
                } else {
                    cameraCallback?.onRecordFailed(record)
                }
            }
        }
        MyLog.d(TAG, "录制结果=" + record)

    }

    fun closeCamera() {
        cameraDevice?.run {
            setRecordStatusCallback(null)
            stopRecord()
            stopYuvVideoFrame(CameraDevice.YUVFrameType.yuvPreviewFrame)
            setYuvCallback(null)
            release()
            if (isPreviewed) {
                stopPreview()
                isPreviewed = false
            }
            CarcorderManager.get().closeCameraDevice(cameraID)

            cameraCallback?.onCloseCamera()
            recording = false
            MyLog.d(TAG, "closeCamera")
        }
        cameraDevice = null
    }

    private fun initParameters(cameraDevice: CameraDevice, cameraID: Int) {
        val parameters = cameraDevice.getParameters()

        parameters.setPreviewSize(640, 480)

        parameters.setCameraId(cameraID)
        parameters.setVideoRotateDuration(videoDurationMS)
        parameters.setVideoRotation(rotation)

        parameters.setYUVCallbackType(CameraDevice.YUVCallbackType.yuvCBAndRecord)//录制同时 获取yuv数据

        parameters.setOutputFile(videoFilePathName)//录制的保存路径
        val profile = CamcorderProfile.get(cameraID, videoQuality)
        profile.videoFrameRate = videoFrameRate//usb摄像头设置15帧
        profile.videoBitRate = videoBitRate
        parameters.setVideoProfile(profile) //video audio 编码器，帧率等设置 params.setMainVideoFrameMode(VideoFrameMode.DISABLE) ; //保存为视频文件
        parameters.setRecordingHint(true)

        parameters.setFreeSizeLimit(videoSizeLimi)//设置视频长度
        parameters.setFileNameTypeFormat(videoFileName, "%Y%m%d%H%M%S%n")
        parameters.setMainVideoFrameMode(CameraDevice.VideoFrameMode.DISABLE) //保存为视频文件
        parameters.setOutputFileFormat(CameraDevice.OutputFormat.MPEG_4)
        parameters.previewFrameRate = videoFrameRate//usb摄像头设置15帧
        cameraDevice.parameters = parameters

    }

    private fun openCamera(cameraId: Int): CameraDevice? {
        CarcorderManager.get().closeCameraDevice(cameraId)
        var cameraDevice: CameraDevice? = null
        try {
            cameraDevice = CarcorderManager.get().openCameraDevice(cameraId)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return cameraDevice
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
            MyLog.d(TAG, "摄像头数量=$numberOfCameras")
            val cameraInfo = CameraInfo()
            for (i in 0 until numberOfCameras) {
                val cameraInfoResult = manager.getCameraInfo(i, cameraInfo)
                if (cameraInfoResult == 0) {
                    if (cameraInfo.type == cameraType) {
                        return i
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return cameraID
        }

        return cameraID
    }


    private fun getCameraStatus(cameraId: Int) = CarcorderManager.get().getCameraState(cameraId)


    fun takePicture(folder: String, pictureName: String) {
        if (cameraDevice == null) return
        val path = "$folder$pictureName.jpeg"
        cameraDevice!!.takePicture(path, CameraDevice.ShutterCallback { },
            CameraDevice.PictureCallback {
            })

    }

    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
        MyLog.d(TAG, "surfaceChanged")

    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {

        synchronized(this) {
            MyLog.d(TAG, "surfaceDestroyed")
            isPreviewed = false

            surfaceCreated = false
//            closeCamera()
        }
    }

    override fun surfaceCreated(p0: SurfaceHolder?) {

        synchronized(this) {
            MyLog.d(TAG, "surfaceCreated")
            surfaceCreated = true

            if (cameraDevice == null) {
                initCamera()
            }

            if (cameraDevice != null) {
                startPreview(p0!!)
            }
        }

    }

    fun startPreview(surfaceHolder: SurfaceHolder) {
        if (!isPreviewed) {
            if (recording) {
                MyLog.d(TAG, "restartPreview")
                restartPreview(surfaceHolder)
            } else {
                MyLog.d(TAG, "_startPreview")
                _startPreview(surfaceHolder)
            }
        }
    }

    private var recording = false

    fun onDestroy() {
        closeCamera()
        if (cameraAvailableCallback != null) {
            CarcorderManager.get().removeCameraAvailableCallback(cameraAvailableCallback)
        }
     /*   if (subscribeEmotion?.isDisposed == false) {
            subscribeEmotion?.dispose()
        }
        if (subscribeFace?.isDisposed == false) {
            subscribeFace?.dispose()
        }*/

    }

}