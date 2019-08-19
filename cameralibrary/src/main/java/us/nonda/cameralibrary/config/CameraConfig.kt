package us.nonda.ai.cache

import android.content.Context
import com.google.gson.Gson
import us.nonda.commonibrary.utils.SPUtils


/**
 * 相机配置
 */
class CameraConfig private constructor(private var context: Context){



    companion object{

        private val SP_KEY = "CameraConfig"

        const val WIDTH = 640
        const val HEIGHT = 480

        private var instance:CameraConfig ?=null

        fun get(context: Context):CameraConfig{
            if (instance == null) {
                val json = SPUtils.get(context, SP_KEY, "") as String
                val cameraConfig = Gson().fromJson(json, CameraConfig::class.java)
                if (cameraConfig == null) {
                    instance = CameraConfig(context)
                } else {
                    instance = cameraConfig
                }
            }

            return instance!!
        }
    }

    public var rotation: Int = 0
    public var video_duration_ms: Int = 1000 * 60 * 1
    public var videoFrameRateFront: Int = 30
    public var videoFrameRateBack: Int = 15

    /**
     * 视频码率
     * 1080p: 4Mbps - 17Mbps
     * 720p: 2Mbps - 9Mbps
     * 480p: 1Mbps - 4Mbps
     */
    public var videoBitRate: Int = 1*1000*1000
    public var videoSizeLimi: Int = 300//视频长度 单位/M

    /**
     * 视频录制分辨率
     */
    public var video_record_quality: Int = 480//视频长度 单位/M


    fun updateCache(){
        val json = Gson().toJson(this)
        SPUtils.put(context, SP_KEY, json)
    }



}