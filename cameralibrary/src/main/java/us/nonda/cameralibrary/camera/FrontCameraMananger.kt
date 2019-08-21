package us.nonda.cameralibrary.camera

import android.view.SurfaceView
import com.mediatek.carcorder.CameraInfo

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
