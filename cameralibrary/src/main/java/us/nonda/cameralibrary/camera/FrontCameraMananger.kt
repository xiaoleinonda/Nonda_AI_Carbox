package us.nonda.cameralibrary.camera

import android.content.Context
import android.view.SurfaceView
import com.mediatek.carcorder.CameraDevice
import com.mediatek.carcorder.CameraInfo
import us.nonda.commonibrary.path.FilePathManager
import us.nonda.commonibrary.utils.PathUtils

class FrontCameraMananger private constructor() : CarboxCameraManager() {


    companion object {

        val TAG = "Front CAMERA"
        val instance: FrontCameraMananger by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            FrontCameraMananger()
        }
    }

    fun initBackCamera(surfaceView: SurfaceView, cameraCallback: CameraCallback) {
        initCamera(surfaceView, true, CameraInfo.CAMERA_MAIN_SENSOR, cameraCallback)
    }

    fun takeFrontPicture(context: Context){
        takePicture(FilePathManager.get(context.applicationContext).getFrontPicturePath() + System.currentTimeMillis(),
            CameraDevice.CamPictureCallback { p0, p1, p2 -> })
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
