package us.nonda.ai.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.SurfaceView
import kotlinx.android.synthetic.main.activity_video_record2.*
import kotlinx.android.synthetic.main.activity_video_record2.draw_detect_face_view
import kotlinx.android.synthetic.main.activity_video_record2.surfaceViewBack
import kotlinx.android.synthetic.main.activity_video_record2.surfaceViewFront
import us.nonda.ai.R
import us.nonda.ai.app.service.SensorReportService
import us.nonda.cameralibrary.camera.*
import us.nonda.cameralibrary.model.PictureModel
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.commonibrary.utils.FinishActivityManager
import us.nonda.facelibrary.callback.FaceDetectCallBack
import us.nonda.facelibrary.db.DBManager
import us.nonda.facelibrary.manager.FaceSDKManager2
import us.nonda.facelibrary.model.LivenessModel
import us.nonda.mqttlibrary.model.EmotionBean
import us.nonda.mqttlibrary.model.FaceResultBean
import us.nonda.mqttlibrary.mqtt.MqttManager

class VideoRecord2Activity : AppCompatActivity() {

    private var backCameraDevice: BackCameraDevice? = null
    private var frontCameraDevice: FrontCameraDevice? = null

    private val TAG = "VideoRecord2Activity"

    private val emotionData = arrayListOf<EmotionBean>()
    private val faceData = arrayListOf<FaceResultBean>()


    companion object {
        fun starter(context: Context) {
            val intent = Intent(context, VideoRecord2Activity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        fun finish() {
            FinishActivityManager.getManager().finishActivity(VideoRecord2Activity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_video_record2)
        FinishActivityManager.getManager().addActivity(this)

        FaceSDKManager2.instance.isRegisted = false

        btn.setOnClickListener {
            startActivity(Intent(this@VideoRecord2Activity, TestActivity::class.java))
        }


        //开启服务
        service()

        //开启摄像头录制
        initCamera()

//        initkFace()
        FaceSDKManager2.instance.setCallback(object : FaceDetectCallBack {
            override fun onFaceDetectCallback(
                isDetect: Boolean,
                faceWidth: Int,
                faceHeight: Int,
                faceCenterX: Int,
                faceCenterY: Int,
                imgWidth: Int,
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
//                println("识别  onTip=${msg}")
            }


            override fun onEnmotionCallback(livenessModel: LivenessModel?) {
                println("识别  onEnmotionCallback=${livenessModel?.emotionsMsg}")
                reportEmotion(livenessModel)

            }

            override fun onFaceFeatureCallBack(livenessModel: LivenessModel?) {
                reportFace(livenessModel)
                println("识别  onFaceFeatureCallBack=${livenessModel?.featureStatus}")

            }


        })

    }

    private fun reportEmotion(livenessModel: LivenessModel?) {
        livenessModel?.run {
            val currentTimeMillis = System.currentTimeMillis()

            val fileName = "$currentTimeMillis$emotionsMsg"
            setEnmotion(emotionsMsg)
            MyLog.d(TAG, "情绪结果emotionsMsg=$emotionsMsg  size=${emotionData.size}")


            if (emotionData.size > 0) {
                val time = emotionData[0].time
                MyLog.d(
                    TAG,
                    "情绪结果size=${emotionData.size}  time=$time   currentTimeMillis=$currentTimeMillis 结果=${currentTimeMillis - time > 10000}"
                )

                if (currentTimeMillis - time > 10000) {
                    MyLog.d(TAG, "可以上报情绪了${emotionData.size}")
                    var reportData = arrayListOf<EmotionBean>()
                    reportData.addAll(emotionData)
                    MqttManager.getInstance().publishEmotion(reportData)
                    emotionData.clear()
                    MyLog.d(TAG, "上报情绪完成${emotionData.size}")

                }
            }
            emotionData.add(EmotionBean(emotionsMsg, currentTimeMillis))


            val pictureModel = PictureModel(
                emotionsMsg!!,
                imageFrame.width, imageFrame.height, imageFrame.argb, fileName
            )
            backCameraDevice?.pictureProcessor?.onNext(

                pictureModel
            )
            frontCameraDevice?.pictureFrontProcessor?.onNext(pictureModel)

        }


    }

    private fun reportFace(livenessModel: LivenessModel?) {
        livenessModel?.run {
            MyLog.d(TAG, "人脸比对结果=$featureStatus")

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


            setResult("成功")
            var pictureModel: PictureModel
            if (featureStatus == 1) {
                var fileName = "${currentTimeMillis}ture"
                pictureModel = PictureModel(
                    "ture", imageFrame.width, imageFrame.height, imageFrame.argb
                    , fileName
                )
                setResult("成功")

            } else {
                var fileName = "${currentTimeMillis}false"
                pictureModel = PictureModel(
                    "false", imageFrame.width, imageFrame.height, imageFrame.argb
                    , fileName
                )
                setResult("失败=$featureStatus")

            }
            backCameraDevice?.pictureFaceProcessor?.onNext(pictureModel)

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
                MqttManager.getInstance().publishEventData(1005, "1")

                MyLog.d("相机", "内路 onOpenCameraSucceed")
            }

            override fun onOpenCameraFaile(msg: String) {
                MqttManager.getInstance().publishEventData(1005, "2")

                MyLog.d("相机", "内路 onOpenCameraFaile= $msg")
            }

            override fun onYuvCbFrame(bytes: ByteArray, width: Int, height: Int) {
//                MyLog.d("相机", "内路 onYuvCbFrame bytes=$bytes")
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
    }


    override fun onDestroy() {
        super.onDestroy()
        FaceSDKManager2.instance.onCameraClose()
        closeService()
//        val deleteAllFeature = DBManager.getInstance().deleteAllFeature("0")

        backCameraDevice?.closeCamera()
        frontCameraDevice?.closeCamera()
        FinishActivityManager.getManager().removeActivity(this)
    }

    private fun closeService() {

        SensorReportService.unbindService(this, serviceConnection)
    }


}
