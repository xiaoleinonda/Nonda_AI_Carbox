package us.nonda.cameralibrary.camera

import android.view.SurfaceView
import com.mediatek.carcorder.CameraDevice
import com.mediatek.carcorder.CameraInfo
import us.nonda.commonibrary.utils.PathUtils

class BackCameraMananger private constructor() : CarboxCameraManager() {


    companion object {

        val TAG = "BACK CAMERA"
        val instance: BackCameraMananger by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            BackCameraMananger()
        }
    }


    fun initBackCamera(surfaceView: SurfaceView, cameraCallback: CameraCallback) {

        initCamera(surfaceView, true, CameraInfo.CAMERA_USB_CAMERA, cameraCallback)
    }

    fun takeFrontPicture(){
        takePicture(filePathManager?.getBackPicturePath() + System.currentTimeMillis(), object :CameraDevice.CamPictureCallback{
            override fun onPictureTaken(p0: Int, p1: Int, p2: String?) {

            }

        })
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
