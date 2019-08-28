package us.nonda.cameralibrary.camera

interface CameraCallback {
    fun onRecordSucceed()
    fun onRecordFailed(code: Int)
    fun onOpenCameraSucceed()
    fun onOpenCameraFaile(msg: String)
    fun onYuvCbFrame(bytes: ByteArray, width:Int, height:Int)
    fun onCloseCamera()
}