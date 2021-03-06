package us.nonda.cameralibrary.camera

import android.media.CamcorderProfile
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.mediatek.carcorder.CameraDevice
import com.mediatek.carcorder.CameraDevice.*
import com.mediatek.carcorder.CameraInfo
import com.mediatek.carcorder.CarcorderManager
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import us.nonda.cameralibrary.model.PictureModel
import us.nonda.cameralibrary.path.FilePathManager
import us.nonda.commonibrary.MyLog
import us.nonda.mqttlibrary.mqtt.MqttManager

class FrontCameraDevice constructor(private var surfaceView: SurfaceView) : SurfaceHolder.Callback {

    private val TAG = "Main摄像头"

    /***       CONFIG               ***/
    private var width = 640
    private var height = 480

    private var videoDurationMS = 1000 * 60 * 1
    private var rotation = 0
    private var videoFilePathName = FilePathManager.get().getFrontVideoPath()
    private var videoQuality: Int = CamcorderProfile.QUALITY_480P
    private var videoFrameRate: Int = 30
    private var videoBitRate: Int = 1 * 500 * 1000
    private var videoSizeLimi: Int = 300//M
    private var videoFileName: String = "front_"


    private var cameraDevice: CameraDevice? = null
    private var cameraID: Int = -1

    private var isPreviewed = false
    private var surfaceCreated = false
//    var pictureFrontProcessor: PublishProcessor<PictureModel> = PublishProcessor.create()


    private var cameraCallback: CameraCallback? = null

    private var recording = false
//    private var subscribeEmotion: Disposable? = null

    fun camera(callback: CameraCallback) {
        cameraCallback = callback
        val surfaceHolder = initPreview()
/*

        synchronized(this) {
            initCamera(surfaceHolder)
        }
*/

      /*  if (subscribeEmotion?.isDisposed == false) {
            subscribeEmotion?.dispose()
        }
        subscribeEmotion = pictureFrontProcessor.subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .distinctUntilChanged { t: us.nonda.cameralibrary.model.PictureModel ->
                t.emotion
            }
            .doOnNext {
                //                val folderPath = FilePathManager.get().getFrontEmotionPictureFolderPath() +   "可爱/"

//                val currentTimeMillis = System.currentTimeMillis()
//                takePicture(folderPath, "${currentTimeMillis}可爱")
                val folderPath = FilePathManager.get().getFrontEmotionPictureFolderPath() + it.emotion + "/"
                takePicture(folderPath, it.fileName)
            }.subscribe({ Log.d("图片", "保存情绪图片成功") }, {})
*/

    }

    private fun initPreview(): SurfaceHolder {
        return surfaceView.run {
            holder.addCallback(this@FrontCameraDevice)
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
            setZOrderMediaOverlay(true)
            holder
        }
    }

    private fun initCamera() {
        val cameraID = getCameraID(CarcorderManager.get(), CameraInfo.CAMERA_MAIN_SENSOR)

        val cameraDevice = openCamera(cameraID)

        if (cameraDevice == null) {
            MyLog.d(TAG, "摄像头打开失败")
            cameraCallback?.onOpenCameraFaile("摄像头打开失败 cameraDevice=$cameraDevice")
            return
        }
        cameraCallback?.onOpenCameraSucceed()
        this.cameraDevice = cameraDevice
        this.cameraID = cameraID
        //录制声音
        cameraDevice.setRecordingMuteAudio(false)

        initParameters(cameraDevice, cameraID);

        /* cameraDevice.setYuvCallback { bytes, i, i1 ->
             cameraCallback?.onYuvCbFrame(bytes, width, height)

         }*/

        /* if (!isPreviewed && surfaceCreated) {
             _startPreview(surfaceHolder)
         }*/

        cameraDevice.setRecordStatusCallback(object : RecordStatusCallback {
            override fun onRecordStatusChanged(p0: Int, p1: Int) {
                MyLog.d(TAG, "录制状态 recordStatus=$p0  cameraID=$p1")
            }

        })
    }

    private fun _startPreview(surfaceHolder: SurfaceHolder) {
        cameraDevice?.run {
            setPreviewSurface(surfaceHolder.surface)
            val cameraStatus1 = getCameraStatus(cameraID)
            MyLog.d(TAG, "开始预览 cameraStatus=$cameraStatus1")


            startPreview()
            startYuvVideoFrame(YUVFrameType.yuvPreviewFrame)
            isPreviewed = true
            _startRecord()

            /*  if (cameraStatus1 == STATE_IDLE) {
                  startPreview()
                  val cameraStatus2 = getCameraStatus(cameraID)
                  MyLog.d(TAG, "预览结束  cameraStatus=$cameraStatus2")
              }

              startYuvVideoFrame(YUVFrameType.yuvPreviewFrame)
              isPreviewed = true

  //            val cameraStatus = getCameraStatus(cameraID)

  //            MyLog.d("相机", "MAINcheck是否需要开启录制cameraStatus=$cameraStatus")
              _startRecord()*/


            /*  if ( cameraStatus== STATE_RECORDING) {
                  cameraDevice?.stopRecord()
                  _startRecord()

              }*/
            /* if (cameraStatus == STATE_PREVIEW || cameraStatus== STATE_RECORDING) {
                 MyLog.d("相机", "准备去开启录制cameraStatus=$cameraStatus")
                 _startRecord()
             }*/

        }

    }

    private var isFirst = true

    private fun _startRecord() {
        /*  val cameraStatus = getCameraStatus(cameraID)
          if (cameraStatus == STATE_RECORDING) {

              MyLog.d("相机", "MAIN发现正在录制开时 retrue cameraStatus=$cameraStatus 开始关闭录制重新打开")
              cameraDevice?.stopRecord()
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
            stopYuvVideoFrame(YUVFrameType.yuvPreviewFrame)
            setYuvCallback(null)
            release()
//            if (isPreviewed) {
            stopPreview()
//            }
            isPreviewed = false
            CarcorderManager.get().closeCameraDevice(cameraID)

            cameraCallback?.onCloseCamera()
            recording = false
            MyLog.d(TAG, "closeCamera")

        }
    }

    private fun initParameters(cameraDevice: CameraDevice, cameraID: Int) {
        val parameters = cameraDevice.getParameters()

        parameters.setPreviewSize(640, 480)

        parameters.setCameraId(cameraID)
        parameters.setVideoRotateDuration(videoDurationMS)
        parameters.setVideoRotation(rotation)

        parameters.setYUVCallbackType(YUVCallbackType.yuvCBAndRecord)//录制同时 获取yuv数据

        parameters.setOutputFile(videoFilePathName)//录制的保存路径
        val profile = CamcorderProfile.get(cameraID, videoQuality)
        profile.videoFrameRate = videoFrameRate//usb摄像头设置15帧
        profile.videoBitRate = videoBitRate
        parameters.setVideoProfile(profile) //video audio 编码器，帧率等设置 params.setMainVideoFrameMode(VideoFrameMode.DISABLE) ; //保存为视频文件
        parameters.setRecordingHint(true)

        parameters.setFreeSizeLimit(videoSizeLimi)//设置视频长度
        parameters.setFileNameTypeFormat(videoFileName, "%Y%m%d%H%M%S%n")
        parameters.setMainVideoFrameMode(VideoFrameMode.DISABLE) //保存为视频文件
        parameters.setOutputFileFormat(OutputFormat.MPEG_4)
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
    private fun getCameraID(manager: CarcorderManager, cameraType: Int): Int {
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
//                    cameraID = i
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


    private fun restartPreview(surfaceHolder: SurfaceHolder) {
        cameraDevice?.run {
            setPreviewSurface(surfaceHolder.surface)
            val cameraStatus1 = getCameraStatus(cameraID)
            MyLog.d(TAG, "resetPreview开始预览 cameraStatus=$cameraStatus1")

            if (cameraStatus1 == STATE_IDLE) {
                startPreview()
                val cameraStatus2 = getCameraStatus(cameraID)
                MyLog.d(TAG, "resetPreview预览结束  cameraStatus=$cameraStatus2")
            }

//            startYuvVideoFrame(YUVFrameType.yuvPreviewFrame)
            isPreviewed = true
        }

    }

    fun takePicture(folder: String, pictureName: String) {
        if (cameraDevice == null) return
        val path = "$folder$pictureName.jpeg"
        cameraDevice!!.takePicture(path, ShutterCallback { },
            PictureCallback {
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

    fun onDestroy() {
        closeCamera()
       /* if (subscribeEmotion?.isDisposed == false) {
            subscribeEmotion?.dispose()
        }*/
    }
}