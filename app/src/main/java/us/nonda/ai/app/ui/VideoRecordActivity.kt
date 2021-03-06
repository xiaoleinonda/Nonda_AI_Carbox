package us.nonda.ai.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import kotlinx.android.synthetic.main.activity_video_record2.*
import kotlinx.android.synthetic.main.activity_video_record2.draw_detect_face_view
import kotlinx.android.synthetic.main.activity_video_record2.surfaceViewBack
import kotlinx.android.synthetic.main.activity_video_record2.surfaceViewFront
import us.nonda.ai.BuildConfig
import us.nonda.ai.R
import us.nonda.ai.app.service.SensorReportService
import us.nonda.cameralibrary.camera.*
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.commonibrary.utils.DeviceLightUtils
import us.nonda.commonibrary.utils.FinishActivityManager
import us.nonda.facelibrary.callback.FaceDetectCallBack
import us.nonda.facelibrary.local.FaceEmotionsSnapshot
import us.nonda.facelibrary.manager.FaceSDKManager2
import us.nonda.facelibrary.model.LivenessModel
import us.nonda.mqttlibrary.model.EmotionBean
import us.nonda.mqttlibrary.model.FaceResultBean
import us.nonda.mqttlibrary.mqtt.MqttManager

class VideoRecordActivity : AppCompatActivity() {

    private var backCameraDevice: BackCameraDevice? = null
    private var frontCameraDevice: FrontCameraDevice? = null

    private val TAG = "VideoRecordActivity"

    private val emotionData = arrayListOf<EmotionBean>()
    private val faceData = arrayListOf<FaceResultBean>()


    companion object {
        var isOpen = false
        fun starter(context: Context) {
            if (isOpen) {
                return
            }
            MyLog.d("VideoRecordActivity", "starter")
            val intent = Intent(context, VideoRecordActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        fun finish() {
            isOpen = false
            MyLog.d("VideoRecordActivity", "finish")
            FinishActivityManager.getManager().finishActivity(VideoRecordActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_record2)
        MyLog.d(TAG, "onCreate")
        FinishActivityManager.getManager().addActivity(this)
        isOpen = true
        FaceSDKManager2.instance.isRegisted = false

        //开启服务
        service()

        //开启摄像头录制
        initCamera()

        FaceSDKManager2.instance.setCallback(object : FaceDetectCallBack {
            override fun onFaceDetectCallback(
                isDetect: Boolean,
                faceWidth: Int,
                faceHeight: Int,
                faceCenterX: Int,
                faceCenterY: Int,
                imgWidth: Int,/**/
                imgHeight: Int
            ) {
                draw_detect_face_view.onFaceDetectCallback(
                    isDetect,
                    faceWidth,
                    faceHeight,
                    faceCenterX,
                    faceCenterY,
                    imgWidth,
                    imgHeight
                )
            }

            override fun onTip(code: Int, msg: String?) {
            }


            override fun onEnmotionCallback(livenessModel: LivenessModel?) {
                reportEmotion(livenessModel)

            }

            override fun onFaceFeatureCallBack(livenessModel: LivenessModel?) {
                reportFace(livenessModel)
            }


        })

    }

    private fun reportEmotion(livenessModel: LivenessModel?) {
        livenessModel?.run {
            MyLog.d(TAG, "情绪结果emotionsMsg=$emotionsMsg  size=${emotionData.size}")
            val currentTimeMillis = System.currentTimeMillis()

            if (BuildConfig.DEBUG) {
                setEnmotion(emotionsMsg)
            }

            if (emotionData.size > 0) {
                val time = emotionData[0].time
                if (currentTimeMillis - time > CarboxConfigRepostory.instance.emotionReportFreq) {
                    var reportData = arrayListOf<EmotionBean>()
                    reportData.addAll(emotionData)
                    MqttManager.getInstance().publishEmotion(reportData)
                    emotionData.clear()
                }
            }
            emotionData.add(EmotionBean(emotionsMsg, currentTimeMillis))
        }
    }

    private fun reportFace(livenessModel: LivenessModel?) {
        livenessModel?.run {
            MyLog.d(TAG, "人脸比对结果=$featureStatus   相似度值=$featureScore")
            val currentTimeMillis = System.currentTimeMillis()

            if (faceData.size > 0) {
                val time = faceData[0].time
                if (currentTimeMillis - time > CarboxConfigRepostory.instance.faceResultReportFreq) {
                    var reportData = arrayListOf<FaceResultBean>()
                    reportData.addAll(faceData)
                    MqttManager.getInstance().publishFaceResult(reportData)
                    faceData.clear()
                }
            }
            faceData.add(FaceResultBean(featureStatus, currentTimeMillis))

            if (BuildConfig.DEBUG) {
                setResult("相似度：$featureScore  ")
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {
        }

        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
        }

    }

    private fun service() {
        SensorReportService.bindService(this, serviceConnection)

    }

    private fun initCamera() {
        backCameraDevice = BackCameraDevice(surfaceViewBack)
        frontCameraDevice = FrontCameraDevice(surfaceViewFront)

        backCameraDevice?.camera(object : CameraCallback {
            override fun onRecordSucceed() {
                MyLog.d("相机", "内路 onRecordSucceed")
                MqttManager.getInstance().publishEventData(1009, "1")
            }

            override fun onRecordFailed(code: Int) {
                MyLog.d("相机", "内路 onRecordFailed code=$code")
                MqttManager.getInstance().publishEventData(1009, "2")
            }

            override fun onOpenCameraSucceed() {
                initkFace()
//                FaceEmotionsSnapshot.instance.initFile()
                MqttManager.getInstance().publishEventData(1005, "1")
                MyLog.d("相机", "内路 onOpenCameraSucceed")
            }

            override fun onOpenCameraFaile(msg: String) {
                MqttManager.getInstance().publishEventData(1005, "2")
                MyLog.d("相机", "内路 onOpenCameraFaile= $msg")
            }

            override fun onYuvCbFrame(bytes: ByteArray, width: Int, height: Int) {
                face(bytes, width, height)
            }

            override fun onCloseCamera() {
                MyLog.d("相机", "内路 onCloseCamera")
                MqttManager.getInstance().publishEventData(1011, "1")
                MqttManager.getInstance().publishEventData(1007, "1")
            }

        })
        frontCameraDevice?.camera(object : CameraCallback {
            override fun onRecordSucceed() {
                MyLog.d("相机", "外路 onRecordSucceed")
                MqttManager.getInstance().publishEventData(1008, "1")
            }

            override fun onRecordFailed(code: Int) {
                MyLog.d("相机", "外路 onRecordFailed code=$code")
                MqttManager.getInstance().publishEventData(1008, "2")
            }

            override fun onOpenCameraSucceed() {
                MqttManager.getInstance().publishEventData(1004, "1")
                MyLog.d("相机", "外路 onOpenCameraSucceed")
            }

            override fun onOpenCameraFaile(msg: String) {
                MqttManager.getInstance().publishEventData(1004, "2")
                MyLog.d("相机", "外路 onOpenCameraFaile= $msg")
            }

            override fun onYuvCbFrame(bytes: ByteArray, width: Int, height: Int) {
                MyLog.d("相机", "外路 onYuvCbFrame bytes=$bytes")
            }

            override fun onCloseCamera() {
                MyLog.d("相机", "外路 onCloseCamera")
                MqttManager.getInstance().publishEventData(1010, "1")
                MqttManager.getInstance().publishEventData(1006, "1")

            }

        })

    }

    private fun face(bytes: ByteArray, width: Int, height: Int) {
        FaceSDKManager2.instance.recognition(bytes, width, height)
    }

    private fun setEnmotion(emotion: String) {
        runOnUiThread {
            tv_emotion.setText(emotion)
        }
    }

    private fun setResult(pass: String) {
        runOnUiThread {
            tv_pass.setText(pass)
        }
    }

    private fun initkFace() {
        FaceSDKManager2.instance.init()
        DeviceLightUtils.normallYellow()
    }

    override fun onDestroy() {
        super.onDestroy()
        MyLog.d(TAG, "onDestroy")
        isOpen = false
        FaceSDKManager2.instance.setCallback(null)
        FaceSDKManager2.instance.onCameraClose()
        closeService()

        backCameraDevice?.onDestroy()
        frontCameraDevice?.onDestroy()
        FinishActivityManager.getManager().removeActivity(this)
//        FaceEmotionsSnapshot.instance.stop()
    }


    private fun closeService() {
        SensorReportService.unbindService(this, serviceConnection)
    }


}
