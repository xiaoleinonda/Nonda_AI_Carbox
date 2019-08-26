package us.nonda.facelibrary.manager

import android.annotation.SuppressLint
import android.util.Log
import com.baidu.idl.facesdk.*
import com.baidu.idl.facesdk.model.BDFaceSDKCommon
import com.baidu.idl.facesdk.model.BDFaceSDKEmotions
import com.baidu.idl.facesdk.model.FaceInfo
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import us.nonda.commonibrary.utils.FileUtils
import us.nonda.facelibrary.callback.FaceDetectCallBack
import us.nonda.facelibrary.config.FaceConfig
import us.nonda.facelibrary.model.LivenessModel
import java.util.concurrent.Executors
import java.util.concurrent.Future

class FaceLiveness constructor(
    private var faceDetect: FaceDetect,
    private var faceLiveness: FaceLive,
    private var faceFeature: FaceFeature,
    private var faceAttribute: FaceAttributes
) {
//    private var faceDetector: FaceDetector ?=null


    private val es = Executors.newSingleThreadExecutor()
    private var future: Future<*>? = null
    private val es2 = Executors.newSingleThreadExecutor()
    private var future2: Future<*>? = null

    private val es3 = Executors.newSingleThreadExecutor()
    private var future3: Future<*>? = null

    private val esEnmotion = Executors.newSingleThreadExecutor()
    private var futureEnmotion: Future<*>? = null

    private val esFeature = Executors.newSingleThreadExecutor()
    private var futureFeature: Future<*>? = null

    private var callback: FaceDetectCallBack? = null

    private val TAG = "FaceLiveness"

    private var width = 640
    private var height = 480

    /******   config   ****/
    private var EMOTION_FREQ_TIME: Long = 1000
    private var FACEFEATURE_FREQ_TIME: Long = 1000
    private var rotation = 0
    private var mirror = 0

    private var emotionPreTime: Long = 0
    private var faceFeaturePreTime: Long = 0


    fun dealData(yuv422: ByteArray) {
        if (yuv422 == null) return
        if (future3 != null && !future3!!.isDone()) {
            return
        }

        future3 = es3.submit {
            Log.d("FaceLiveness", "转换前=" + yuv422.size)

            val to420 = yuv422To420(yuv422, width, height)
            Log.d("FaceLiveness", "转换后=" + to420.size)

            val argb = IntArray(width * height)

            faceDetect.getDataFromYUVimg(to420, argb, width, height, rotation, mirror)

            onDetectCheck(argb)
        }
    }


    fun onDetectCheck(rgbArray: IntArray): Boolean {
        var isLiveness = false
        /* if (mRgbArray == null) {
             return isLiveness
         }
 */
        if (future != null && !future!!.isDone()) {
            return isLiveness
        }

        future = es.submit {
            //  克隆基本数据，保证一致
//            var cloneRgbArray = rgbArray!!.clone()
            Log.d("FaceLiveness", "开始识别=" + rgbArray.size)

            val maxFace = trackMaxFace(rgbArray, width, height)

            Log.d(TAG, "maxFace=" + maxFace)

            val livenessModel = LivenessModel()
            livenessModel.imageFrame.argb = rgbArray
            livenessModel.imageFrame.width = width
            livenessModel.imageFrame.height = height

            if (maxFace != null && maxFace.size > 0) {
                livenessModel.trackFaceInfo = maxFace
                val faceInfo = maxFace[0]
                livenessModel.landmarks = faceInfo.landmarks
                livenessModel.faceInfo = faceInfo
                livenessModel.faceID = faceInfo.face_id

                callback?.onFaceDetectCallback(
                    true, faceInfo.mWidth.toInt(),
                    faceInfo.mWidth.toInt(), faceInfo.mCenter_x.toInt(), faceInfo.mCenter_y.toInt(),
                    width, height
                )

                livenessFeatures(livenessModel)

            } else {
                callback?.onTip(1, "未检测到人脸")
                callback?.onFaceDetectCallback(
                    false, 0,
                    0, 0, 0, 0, 0
                )

            }
        }

        return isLiveness

    }


    private fun livenessFeatures(livenessModel: LivenessModel) {
        if (future2 != null && !future2!!.isDone) {
            return
        }

        var liveType = 0
        future2 = es2.submit {
            var rgbScore: Float = 0f

            when (liveType) {
                0 -> {
                    rgbScore = rgbLiveness(
                        livenessModel.imageFrame.argb,
                        livenessModel.imageFrame.width,
                        livenessModel.imageFrame.height,
                        livenessModel.landmarks
                    )
                }
                1 -> {
                    rgbScore = irLiveness(
                        livenessModel.imageFrame.ir,
                        livenessModel.imageFrame.width,
                        livenessModel.imageFrame.height,
                        livenessModel.landmarks
                    )
                }

            }
            livenessModel.rgbLivenessScore = rgbScore

            Log.d("人脸识别", "活体=" + rgbScore)
            if (isFeature()) {
                filterFeature(livenessModel)
            }

            if (isEnmotion()) {
                startEnmotion(livenessModel)
            }

        }

    }


    @SuppressLint("CheckResult")
    private fun startEnmotion(livenessModel: LivenessModel) {
        if (futureEnmotion != null && !futureEnmotion!!.isDone) {
            return
        }

        futureEnmotion = esEnmotion.submit {
            livenessModel.run {
                val emotions = faceAttribute.emotions(imageFrame.argb, imageFrame.height, imageFrame.width, landmarks)
                val parseFaceEnmition = parseFaceEnmition(emotions)
                emotionsMsg = parseFaceEnmition
                Log.d(TAG, "情绪=" + parseFaceEnmition)
                callback?.onEnmotionCallback(this)
            }
        }


    }

    private fun parseFaceEnmition(emotions: BDFaceSDKEmotions?): String? {
        return when (emotions?.emotion) {
            BDFaceSDKCommon.BDFaceEmotionEnum.BDFACE_EMOTIONS_ANGRY -> {
                "生气"
            }
            BDFaceSDKCommon.BDFaceEmotionEnum.BDFACE_EMOTIONS_DISGUST -> {
                "恶心"
            }
            BDFaceSDKCommon.BDFaceEmotionEnum.BDFACE_EMOTIONS_FEAR -> {
                "害怕"
            }
            BDFaceSDKCommon.BDFaceEmotionEnum.BDFACE_EMOTIONS_HAPPY -> {
                "开心"
            }
            BDFaceSDKCommon.BDFaceEmotionEnum.BDFACE_EMOTIONS_SAD -> {
                "伤心"
            }

            BDFaceSDKCommon.BDFaceEmotionEnum.BDFACE_EMOTIONS_SURPRISE -> {
                "惊讶"
            }
            BDFaceSDKCommon.BDFaceEmotionEnum.BDFACE_EMOTIONS_NEUTRAL -> {
                "无情绪"
            }
            else -> {
                "解析失败"
            }
        }
    }

    private fun filterFeature(livenessModel: LivenessModel) {
        if (futureFeature != null && !futureFeature!!.isDone) {
            return
        }

        futureFeature = esFeature.submit {
            val visFeature = ByteArray(512)

            livenessModel.run {
                val length = extractFeature(
                    imageFrame.argb,
                    imageFrame.height,
                    imageFrame.width,
                    visFeature,
                    landmarks
                )


                if (length == 128f) {
                    featureByte = visFeature

                    val feature1 = FaceSDKManager.instance.getFaceFeature(
                        FaceFeature.FeatureType.FEATURE_VIS,
                        getFeatureByte(),
                        this
                    )

                    if (feature1 != null) {
                        this.feature = feature1
                        //特征比对成功
                        featureStatus = 1
                        Log.d(TAG, "特征比对一致")
                    } else {
                        //特征比对失败
                        featureStatus = 2
                        Log.d(TAG, "特征比对不一致")

                    }

                } else {
                    //特征提取失败
                    featureStatus = 3
                    Log.d(TAG, "特征提取失败")

                }
                callback?.onFaceFeatureCallBack(this)
            }

        }

    }


    private fun isEnmotion(): Boolean {
        val currentTimeMillis = System.currentTimeMillis()
        if (currentTimeMillis - emotionPreTime > EMOTION_FREQ_TIME) {
            emotionPreTime = currentTimeMillis
            return true
        } else {
            return false
        }
    }

    private fun isFeature(): Boolean {
        val currentTimeMillis = System.currentTimeMillis()
        if (currentTimeMillis - faceFeaturePreTime > FACEFEATURE_FREQ_TIME) {
            faceFeaturePreTime = currentTimeMillis
            return true
        } else {
            return false
        }
    }

    /**
     * 可见光静默活体分值检测，返回0-1结果，建议超过0.9 为活体
     *
     * @param data
     * @param width
     * @param height
     * @param landmarks
     * @return
     */
    private fun rgbLiveness(data: IntArray, width: Int, height: Int, landmarks: IntArray): Float {
        val rgbScore = faceLiveness.silentLive(FaceLive.LiveType.LIVEID_VIS, data, height, width, landmarks)
        Log.d("百度", "rgbLiveness: 活体检测=$rgbScore")
        return rgbScore
    }

    /**
     * 可见光静默活体分值检测，返回0-1结果，建议超过0.9 为活体
     *
     * @param data
     * @param width
     * @param height
     * @param landmarks
     * @return
     */
    private fun irLiveness(data: ByteArray, width: Int, height: Int, landmarks: IntArray): Float {
        val irScore = faceLiveness.silentLive(FaceLive.LiveType.LIVEID_NIR, data, height, width, landmarks);

        Log.d("百度", "irLiveness: 活体检测=$irScore")
        return irScore
    }


    private fun trackMaxFace(rgbArray: IntArray, width: Int, height: Int): Array<FaceInfo>? {
        val minFaceSize = FaceSDKManager.instance.getMinFaceSize()
        if (width < minFaceSize || height < minFaceSize) {
            return null
        }

        return faceDetect.trackMaxFace(rgbArray, height, width)
    }

    fun setCallback(callback: FaceDetectCallBack?) {
        if (callback == null || this.callback == callback) {
            return
        }
        this.callback = callback
    }

    /**
     * @param yuv422
     * @param width
     * @param height
     * @return
     */
    private fun yuv422To420(yuv422: ByteArray, width: Int, height: Int): ByteArray {
        val len = width * height
        //yuv格式数组大小，y亮度占len长度，u,v各占len/4长度。
        val yuv = ByteArray(len * 3 / 2)

        val y = 0

        var index_y = 0
        var index_u = 0

        var is_u = true

        for (i in 0 until height * 2) {
            var j = 0
            while (j < width) {
                yuv[y + index_y++] = yuv422[width * i + j]
                yuv[y + index_y++] = yuv422[width * i + j + 2]
                j = j + 4
            }
        }

        var i = 0
        while (i < height) {
            val base = i * width * 2
            var j = base + 1
            while (j < base + width * 2) {
                if (is_u) {
                    yuv[len + index_u++] = yuv422[j]
                    is_u = false
                } else {
                    yuv[len + index_u++] = yuv422[j]
                    is_u = true
                }
                j = j + 2

            }
            i = i + 2
        }

        return yuv
    }


    /**
     * 人脸特征提取
     *
     * @param argb
     * @param landmarks
     * @param height
     * @param width
     * @param feature
     * @return
     */

    private fun extractFeature(
        argb: IntArray,
        height: Int,
        width: Int,
        feature: ByteArray,
        landmarks: IntArray
    ): Float {
        return faceFeature.feature(FaceFeature.FeatureType.FEATURE_VIS, argb, height, width, landmarks, feature)
    }


    fun initConfig(config: FaceConfig) {
        EMOTION_FREQ_TIME = config.emotionFreqTime
        FACEFEATURE_FREQ_TIME = config.facefeatureFreqTime
        rotation = config.rotation
        mirror = config.mirror
    }

    fun stop() {
        callback == null
    }
}