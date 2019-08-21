package us.nonda.facelibrary.config

class FaceConfig {

    //旋转角度
    var rotation = 0
    //情绪检测的时间间隔
    var emotionFreqTime: Long = 1000
    //人脸比对的时间间隔
    var facefeatureFreqTime: Long = 1000
    //活体检测的实际间隔
    var faceFreq: Long = 500
    // 0：RGB无镜像，1：有镜像
    var mirror: Int = 0



}