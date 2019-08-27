package us.nonda.cameralibrary.camera

import android.util.Log
import android.view.SurfaceView
import com.mediatek.carcorder.CameraInfo
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import us.nonda.cameralibrary.model.PictureModel
import us.nonda.cameralibrary.path.FilePathManager

class FrontCameraMananger private constructor() : CarboxCameraManager() {

    var pictureFrontProcessor: PublishProcessor<PictureModel> = PublishProcessor.create()

    companion object {

        val TAG = "Front CAMERA"
        val instance: FrontCameraMananger by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            FrontCameraMananger()
        }
    }

    fun initBackCamera(surfaceView: SurfaceView, cameraCallback: CameraCallback) {
        initCamera(surfaceView, true, CameraInfo.CAMERA_MAIN_SENSOR, cameraCallback)

        pictureFrontProcessor.subscribeOn(Schedulers.io())
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
    }


    /*override fun startRecordResult(record: Int) {
        cameraCallback?.run {
            when (record) {
                null -> {
                }
                0 -> {
                    log(TAG, "Back Camera Record Succeed：$record")
                    onRecordSucceed()
                }

                else -> {
                    log(TAG, "Back Camera Record Failed：$record")
                    onRecordFailed(record)
                }
            }
        }
    }*/
}
