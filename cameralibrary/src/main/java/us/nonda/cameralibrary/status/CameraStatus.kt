package us.nonda.cameralibrary.status

import com.mediatek.carcorder.CarcorderManager

class CameraStatus private constructor(){

    companion object{
        val instance: CameraStatus by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED){
            CameraStatus()
        }

    }

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


    /**
     * 返回acc状态
     * 1 ACC ON， 0 ACC OFF， -1 UNKNOW
     */
    fun getAccStatus() = CarcorderManager.get().queryCarEngineState()


    /**
     * 主动唤醒休眠
     */
    fun exitIpo() = CarcorderManager.get().ipodProxy?.exitIpod(0)


    fun resetStatus() {
        frontCameraOpenStatus = -1
        backCameraOpenStatus = -1
        frontCameraRecordStatus = -1
        backCameraRecordStatus = -1
    }


}