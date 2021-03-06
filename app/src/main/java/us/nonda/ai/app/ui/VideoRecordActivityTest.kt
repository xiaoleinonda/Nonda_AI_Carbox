package us.nonda.ai.app.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_video_record.*
import us.nonda.ai.R
import us.nonda.cameralibrary.camera.BackCameraMananger
import us.nonda.cameralibrary.camera.CameraCallback
import us.nonda.cameralibrary.camera.FrontCameraMananger
import us.nonda.cameralibrary.model.PictureModel
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.commonibrary.utils.FinishActivityManager
import us.nonda.facelibrary.callback.FaceDetectCallBack
import us.nonda.facelibrary.db.DBManager
import us.nonda.facelibrary.manager.FaceSDKManager
import us.nonda.facelibrary.model.LivenessModel
import us.nonda.mqttlibrary.model.EmotionBean
import us.nonda.mqttlibrary.model.FaceResultBean
import us.nonda.mqttlibrary.mqtt.MqttManager

class VideoRecordActivityTest : AppCompatActivity() {

    private val TAG = "VideoRecordActivityTest"

    private val emotionData = arrayListOf<EmotionBean>()
    private val faceData = arrayListOf<FaceResultBean>()


    companion object {
        fun starter(context: Context) {
            /*  val activitySum = FinishActivityManager.getManager().activitySum
              if (activitySum > 0) {
                  FinishActivityManager.getManager().finishActivity(VideoRecordActivityTest::class.java)
              }*/
            val intent = Intent(context, VideoRecordActivityTest::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
/*
            Observable.timer(5, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe{
                    val activitySum = FinishActivityManager.getManager().activitySum
                    if (activitySum > 0) {
                        FinishActivityManager.getManager().finishActivity(VideoRecordActivityTest::class.java)
                    }

                    val intent = Intent(context, VideoRecordActivityTest::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                }
*/
        }

        fun finish() {
            FinishActivityManager.getManager().finishActivity(VideoRecordActivityTest::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_record)

        FinishActivityManager.getManager().addActivity(this)
        MyLog.d(TAG, "onCreate")
        oepnCamera()

        btn_camera.setOnClickListener {
            //            oepnCamera()
//            starterRecord()
//
        }


//        initPublish()
    }

    override fun onResume() {
        super.onResume()
        MyLog.d(TAG, "onResume")
    }


    fun starterRecord() {
//        BackCameraMananger.instance.startRecord()
//        FrontCameraMananger.instance.startRecord()
    }

    private fun openFrontCamera() {
        FrontCameraMananger.instance.initBackCamera(surfaceViewFront, object :
            CameraCallback {
            override fun onCloseCamera() {
                MqttManager.getInstance().publishEventData(1006, "1")
                MqttManager.getInstance().publishEventData(1010, "1")
            }

            override fun onRecordSucceed() {
                MqttManager.getInstance().publishEventData(1008, "1")

            }

            override fun onRecordFailed(code: Int) {
                MqttManager.getInstance().publishEventData(1008, "2")

            }

            override fun onOpenCameraSucceed() {
//                println("录制 Front  onOpenCameraSucceed")
            }

            override fun onOpenCameraFaile(msg: String) {
//                println("录制  onOpenCameraFaile $msg")
            }

            override fun onYuvCbFrame(bytes: ByteArray, width: Int, height: Int) {
//                println("Front  onYuvCbFrame $bytes")

            }

        })

    }

    private fun openBackCamera() {
        BackCameraMananger.instance.initBackCamera(surfaceViewBack, object :
            CameraCallback {
            override fun onCloseCamera() {
                MqttManager.getInstance().publishEventData(1011, "1")

            }

            override fun onRecordSucceed() {
                MqttManager.getInstance().publishEventData(1009, "1")
            }

            override fun onRecordFailed(code: Int) {
                MqttManager.getInstance().publishEventData(1009, "2")

            }

            override fun onOpenCameraSucceed() {
                MyLog.d(TAG, "打开back摄像头成功， 开始初始化face")
                FaceSDKManager.instance.check()
//                println("录制 BACK  onOpenCameraSucceed")
            }

            override fun onOpenCameraFaile(msg: String) {
//                println("录制 BACK  onOpenCameraFaile $msg")
            }

            override fun onYuvCbFrame(bytes: ByteArray, width: Int, height: Int) {
//                println("百度BACK   onYuvCbFrame $bytes")
                face(bytes, width, height)

            }

        })

    }

    private fun oepnCamera() {
        openBackCamera()
        openFrontCamera()

        FaceSDKManager.instance.setCallback(object : FaceDetectCallBack {
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
                livenessModel?.run {
                    val currentTimeMillis = System.currentTimeMillis()

                    val fileName = "$currentTimeMillis$emotionsMsg"
                    setEnmotion(emotionsMsg)
                    MyLog.d(TAG, "情绪结果emotionsMsg=$emotionsMsg  size=${emotionData.size}")


                    if (emotionData.size > 0) {
                        val time = emotionData[0].time
                        MyLog.d(TAG, "情绪结果size=${emotionData.size}  time=$time   currentTimeMillis=$currentTimeMillis 结果=${currentTimeMillis - time > 10000}")

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
                    BackCameraMananger.instance.pictureProcessor.onNext(

                        pictureModel
                    )
//                    val folderPath = FilePathManager.get().getFrontEmotionPictureFolderPath() + emotionsMsg + "/"
//                    BackCameraMananger.instance.takePicture(folderPath, fileName)
                    FrontCameraMananger.instance.pictureFrontProcessor.onNext(pictureModel)

                }

            }

            override fun onFaceFeatureCallBack(livenessModel: LivenessModel?) {
                println("识别  onFaceFeatureCallBack=${livenessModel?.featureStatus}")
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
                    BackCameraMananger.instance.pictureFaceProcessor.onNext(pictureModel)

                }

            }


        })

    }


    private fun face(bytes: ByteArray, width: Int, height: Int) {
        FaceSDKManager.instance.recognition(bytes, width, height)
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

    override fun onDestroy() {
        MyLog.d(TAG, "onDestroy")

        super.onDestroy()
        us.nonda.cameralibrary.camera.BackCameraMananger.instance.closeCamera()
        us.nonda.cameralibrary.camera.FrontCameraMananger.instance.closeCamera()


        FinishActivityManager.getManager().removeActivity(this)

        FaceSDKManager.instance.onCameraClose()

        val deleteAllFeature = DBManager.getInstance().deleteAllFeature("0")

        val queryFeature = DBManager.getInstance().queryFeature()

        MyLog.d("删除数据", "$deleteAllFeature   ---   ${queryFeature.size}")

    }
}
