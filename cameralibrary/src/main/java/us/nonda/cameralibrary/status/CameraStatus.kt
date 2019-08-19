package us.nonda.cameralibrary.status

class CameraStatus {

    /**
     * 前摄像头open状态
     * 0打开成功
     */
    var frontCameraOpenStatus:Int = -1

    /**
     * 后摄像头open状态
     * 0打开成功
     */
    var backCameraOpenStatus:Int = -1

    /**
     * 前摄像头录制状态
     * 0正在录制
     */
    var frontCameraRecordStatus = -1

    /**
     * 后摄像头录制状态
     * 0正在录制
     */
    var backCameraRecordStatus = -1


    fun resetStatus() {
        frontCameraOpenStatus = -1
        backCameraOpenStatus = -1
        frontCameraRecordStatus = -1
        backCameraRecordStatus = -1
    }


}