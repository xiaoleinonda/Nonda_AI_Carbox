package us.nonda.ai.app.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_video_record.*
import us.nonda.ai.R
import us.nonda.ai.cache.CameraConfig
import us.nonda.cameralibrary.camera.BackCameraMananger
import us.nonda.cameralibrary.camera.CameraCallback
import us.nonda.cameralibrary.camera.FrontCameraMananger
import us.nonda.cameralibrary.status.CameraStatus
import us.nonda.commonibrary.utils.FinishActivityManager
import us.nonda.facelibrary.callback.FaceDetectCallBack
import us.nonda.facelibrary.manager.FaceSDKManager
import us.nonda.facelibrary.model.LivenessModel

class VideoRecordActivity : AppCompatActivity() {


    companion object{
        fun starter(context:Context){
            context.startActivity(Intent(context, VideoRecordActivity::class.java))
        }

        fun finish(){
            FinishActivityManager.getManager().finishActivity(VideoRecordActivity::class.java)
        }
    }
    private var subscribe: Disposable? = null
    private var publishProcessor: PublishProcessor<ByteArray>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_record)
        FinishActivityManager.getManager().addActivity(this)
        oepnCamera()


        btn_camera.setOnClickListener {
            //            oepnCamera()
            starterRecord()
        }

//        initPublish()
    }

    private fun initPublish() {
        publishProcessor = PublishProcessor.create<ByteArray>()

        if (subscribe?.isDisposed == false) {
            subscribe?.dispose()
        }
        subscribe = publishProcessor!!.subscribeOn(Schedulers.computation())
            .unsubscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                //                cameraCallback?.onYuvCbFrame(it)
            }, {})

    }

    fun starterRecord() {
//        BackCameraMananger.instance.startRecord()
//        FrontCameraMananger.instance.startRecord()
    }

    var timeBack: Long = 0;
    var timeFront: Long = 0;
    private fun oepnCamera() {
        us.nonda.cameralibrary.camera.BackCameraMananger.instance.initBackCamera(surfaceViewBack, object :
            us.nonda.cameralibrary.camera.CameraCallback {
            override fun onRecordSucceed() {
                println("录制 BACK  onRecordSucceed")
            }

            override fun onRecordFailed(code: Int) {
                println("录制 BACK  onRecordFailed $code")
            }

            override fun onOpenCameraSucceed() {
                println("录制 BACK  onOpenCameraSucceed")
            }

            override fun onOpenCameraFaile(msg: String) {
                println("录制 BACK  onOpenCameraFaile $msg")
            }

            override fun onYuvCbFrame(bytes: ByteArray, width: Int, height: Int) {
                println("百度BACK   onYuvCbFrame $bytes")
                face(bytes, width, height)

            }

        })

        us.nonda.cameralibrary.camera.FrontCameraMananger.instance.initBackCamera(surfaceViewFront, object :
            us.nonda.cameralibrary.camera.CameraCallback {
            override fun onRecordSucceed() {
                println("录制 Front  onRecordSucceed")

            }

            override fun onRecordFailed(code: Int) {
                println("录制 Front  onRecordFailed $code")

            }

            override fun onOpenCameraSucceed() {
                println("录制 Front  onOpenCameraSucceed")
            }

            override fun onOpenCameraFaile(msg: String) {
                println("录制  onOpenCameraFaile $msg")
            }

            override fun onYuvCbFrame(bytes: ByteArray, width: Int, height: Int) {
                println("Front  onYuvCbFrame $bytes")

            }

        })
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
                draw_detect_face_view.onFaceDetectCallback(isDetect, faceWidth, faceHeight, faceCenterX, faceCenterY, imgWidth, imgHeight)
            }

            override fun onTip(code: Int, msg: String?) {
                println("识别  onTip=${msg}")
            }


            override fun onEnmotionCallback(livenessModel: LivenessModel?) {
                println("识别  onEnmotionCallback=${livenessModel?.emotionsMsg}")
            }

            override fun onFaceFeatureCallBack(livenessModel: LivenessModel?) {
                println("识别  onFaceFeatureCallBack=${livenessModel?.featureStatus}")

            }


        })

    }

    /**
     * 绘制人脸框。
     */

    private fun showFrame(isDetect: Boolean, model: LivenessModel?) {


    }

    private fun face(bytes: ByteArray, width: Int, height: Int) {
        FaceSDKManager.instance.recognition(bytes, width, height)
    }

    override fun onDestroy() {
        super.onDestroy()
        us.nonda.cameralibrary.camera.BackCameraMananger.instance.closeCamera()
        us.nonda.cameralibrary.camera.FrontCameraMananger.instance.closeCamera()

    }
}
