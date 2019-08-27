package us.nonda.cameralibrary.camera

import android.util.Log
import android.view.SurfaceView
import com.mediatek.carcorder.CameraDevice
import com.mediatek.carcorder.CameraInfo
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import us.nonda.cameralibrary.model.PictureModel
import us.nonda.cameralibrary.path.FilePathManager
import us.nonda.commonibrary.utils.FileUtils
import us.nonda.commonibrary.utils.PathUtils

class BackCameraMananger private constructor() : CarboxCameraManager() {
    private var subscribe: Disposable? = null

    var pictureProcessor: PublishProcessor<PictureModel> = PublishProcessor.create()
    var pictureFaceProcessor: PublishProcessor<PictureModel> = PublishProcessor.create()

    companion object {

        val TAG = "BACK CAMERA"
        val instance: BackCameraMananger by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            BackCameraMananger()
        }
    }


    fun initBackCamera(surfaceView: SurfaceView, cameraCallback: CameraCallback) {

        initCamera(surfaceView, true, CameraInfo.CAMERA_USB_CAMERA, cameraCallback)

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


    override fun closeCamera() {
        super.closeCamera()
        if (subscribe != null && !subscribe!!.isDisposed) {
            subscribe!!.dispose()
        }
    }



}
