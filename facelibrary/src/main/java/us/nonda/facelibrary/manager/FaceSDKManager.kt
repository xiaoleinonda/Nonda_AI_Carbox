package us.nonda.facelibrary.manager

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.baidu.idl.facesdk.FaceAttributes
import com.baidu.idl.facesdk.FaceDetect
import com.baidu.idl.facesdk.FaceFeature
import com.baidu.idl.facesdk.FaceLive
import com.baidu.idl.facesdk.callback.Callback
import com.baidu.idl.facesdk.model.BDFaceSDKCommon
import com.baidu.idl.facesdk.model.BDFaceSDKEmotions
import com.baidu.idl.facesdk.model.FaceInfo
import com.baidu.idl.facesdk.model.Feature
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import us.nonda.commonibrary.utils.NetworkUtil
import us.nonda.facelibrary.`interface`.IFaceInitListener
import us.nonda.facelibrary.auth.FaceAuthManager
import us.nonda.facelibrary.auth.IFaceAuthCallback
import us.nonda.facelibrary.callback.FaceDetectCallBack
import us.nonda.facelibrary.common.FaceEnvironment
import us.nonda.facelibrary.config.FaceConfig
import us.nonda.facelibrary.db.DBManager
import us.nonda.facelibrary.db.LRUCache
import us.nonda.facelibrary.model.*
import us.nonda.facelibrary.status.FaceStatusCache
import java.util.concurrent.Executors
import java.util.concurrent.Future

class FaceSDKManager private constructor() {


    private lateinit var faceDetect: FaceDetect
    private lateinit var faceFeature: FaceFeature
    private lateinit var faceLiveness: FaceLive
    private lateinit var faceEnvironment: FaceEnvironment
    private lateinit var faceAttribute: FaceAttributes


    private var context: Context? = null


    var config: FaceConfig? = null

    private var publishProcessor: PublishProcessor<ByteArray>? = null

    private val es = Executors.newSingleThreadExecutor()
    private var future: Future<*>? = null
    private val es2 = Executors.newSingleThreadExecutor()
    private var future2: Future<*>? = null
    private val es3 = Executors.newSingleThreadExecutor()
    private var future3: Future<*>? = null

    private val featureLRUCache = LRUCache<String, Feature>(1000)


    private var status = STATUS_INIT

    companion object {
        const val TAG = "FaceSDKManager"
        const val STATUS_INIT = 0;//未初始化
        const val STATUS_INITING = 1;//初始化中
        const val STATUS_INITED = 2//已初始化

        val instance: FaceSDKManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            FaceSDKManager()
        }
    }

    private lateinit var featureProcessor: PublishProcessor<LivenessModel>
    private lateinit var enmotionProcessor: PublishProcessor<LivenessModel>

    private var callback: FaceDetectCallBack? = null

    private var pass = false


    /**
     * 每次应用启动初始化时调用
     */
    @Synchronized
    fun init(context: Context, license: String) {

        if (status != STATUS_INIT) {
            return
        }
        log("开始初始化")
        status = STATUS_INITING

        init(context, license, object : IFaceInitListener {
            override fun onSucceed() {
                status = STATUS_INITED
                initPulish()
            }

            override fun onFailed(msg: String) {
                status = STATUS_INIT
            }

        })


    }

    /**
     * 是否已经初始化了
     */
    private fun checkInited(): Boolean {
        return status == STATUS_INITED
    }

    private fun initPulish() {
        /* publishProcessor = PublishProcessor.create<ByteArray>()
         publishProcessor!!.subscribeOn(Schedulers.computation())
             .unsubscribeOn(Schedulers.computation())
             .observeOn(Schedulers.computation())
             .map {
                 yuv422To420(
                     it,
                     CarBoxCache.instance.getCameraConfig().width,
                     CarBoxCache.instance.getCameraConfig().height
                 );
             }
             .subscribe({
                 onDetectCheck(it, null, null, )
             }, {})*/

    }

    /**
     * 激活License
     * 在收到服务器推送的license时调用
     */
    fun initLicense(context: Context, license: String, listener: IFaceInitListener?) {
        if (TextUtils.isEmpty(license)) {
            log("license is empty")
            status = STATUS_INIT
            return
        }
        if (NetworkUtil.getConnectivityStatus(context)) {
            log("开始激活")

            FaceAuthManager().initLicense(context, license, object : IFaceAuthCallback {
                override fun onFailed(msg: String) {
                    listener?.onFailed("设备激活失败：$msg")
                    status = STATUS_INIT
                    FaceStatusCache.instance.faceLicence = ""
                    Log.d(TAG, "激活失败=$msg")
                }

                override fun onSucceed() {
                    FaceStatusCache.instance.faceLicence = license
                    Log.d(TAG, "激活成功")
                    initModel(listener)
                }


            })
        } else {
            status = STATUS_INIT
            log("net error")
        }

    }

    fun init(context: Context, license: String, listener: IFaceInitListener?) {
        this.context = context.applicationContext

        if (FaceStatusCache.instance.isLicence()) {
            initModel(listener)
        } else {
            initLicense(context, license, listener)
        }

    }

    private fun log(s: String) {
        Log.d(TAG, s)

    }

    fun initConfig(config: FaceConfig?) {
        this.config = config ?: FaceConfig()
    }


    fun initSDK() {
        initConfig(null)
        faceDetect = FaceDetect()
        faceFeature = FaceFeature()
        faceLiveness = FaceLive()
        faceAttribute = FaceAttributes()
        faceEnvironment = FaceEnvironment()


        featureProcessor = PublishProcessor.create<LivenessModel>()
        enmotionProcessor = PublishProcessor.create<LivenessModel>()

        featureProcessor.subscribeOn(Schedulers.computation())
            .unsubscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .filter {
                val visFeature = ByteArray(512)

                val length = extractFeature(
                    it.imageFrame.argb,
                    it.imageFrame.height,
                    it.imageFrame.width,
                    visFeature,
                    it.landmarks
                )


                if (length == 128f) {
                    it.featureByte = visFeature
                    return@filter true
                }

                it.featureStatus = 0
                callback?.onFaceFeatureCallBack(it)
                false
            }.map {
                val feature = getFeature(FaceFeature.FeatureType.FEATURE_VIS, it.getFeatureByte(), it)
                if (feature != null) {
                    it.feature = feature
                    it.featureStatus = 1
                    pass = true
                } else {
                    it.featureStatus = 2
                    pass = false
                }
                it
            }
            .subscribe({
                callback?.onFaceFeatureCallBack(it)
            }, {})

        enmotionProcessor.subscribeOn(Schedulers.computation())
            .unsubscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .map {
                val emotions =
                    faceAttribute.emotions(it.imageFrame.argb, it.imageFrame.height, it.imageFrame.width, it.landmarks)
                Log.d("人脸识别", "情绪=" + emotions)
                val parseFaceEnmition = parseFaceEnmition(emotions)
                it.emotionsMsg = parseFaceEnmition
                it
            }.subscribe({
                callback?.onEnmotionCallback(it)
            }, {})
    }

    private fun onInitSucceed() {
        status = STATUS_INITED
        Log.d(TAG, "初始化成功")
        DBManager.getInstance().init(context)
        setFeature()
    }

    private fun onInitFailed() {
        status = STATUS_INIT
        Log.d(TAG, "初始化失败")
    }

    @Synchronized
    fun initModel(listener: IFaceInitListener?) {
        Log.d(TAG, "开始初始化模型")
        try {
            initSDK()

            var detectSucceed = false
            var livenessSucceed = false
            var featureSucceed = false
            var attributeSucceed = false
            faceDetect?.initModel(context,
                GlobalSet.DETECT_VIS_MODEL,
                GlobalSet.DETECT_NIR_MODE,
                GlobalSet.ALIGN_MODEL,
                Callback { code, response ->
                    if (code == 0) {
                        detectSucceed = true
                        if (livenessSucceed && featureSucceed && attributeSucceed) {
                            listener?.onSucceed()
                            onInitSucceed()
                        }
                    } else {
                        listener?.onFailed(response)
                        onInitFailed()
                    }
                })

            faceDetect.loadConfig(getFaceEnvironmentConfig().config)
            faceAttribute.initModel(
                context,
                GlobalSet.ATTRIBUTE_ATTTIBUTE_MODEL,
                GlobalSet.ATTRIBUTE_EMOTION_MODEL,
                Callback { code, response ->
                    if (code == 0) {
                        attributeSucceed = true
                        if (livenessSucceed && featureSucceed && detectSucceed) {
                            listener?.onSucceed()
                            onInitSucceed()
                        }
                    } else {
                        listener?.onFailed(response)
                        onInitFailed()
                    }
                })


            faceLiveness.initModel(
                context,
                GlobalSet.LIVE_VIS_MODEL,
                GlobalSet.LIVE_NIR_MODEL,
                GlobalSet.LIVE_DEPTH_MODEL
            ) { code, response ->
                if (code == 0) {
                    livenessSucceed = true
                    if (detectSucceed && featureSucceed && attributeSucceed) {
                        listener?.onSucceed()
                        onInitSucceed()
                    }
                } else {
                    listener?.onFailed(response)
                    onInitFailed()
                }
            }

            faceFeature.initModel(
                context,
                GlobalSet.RECOGNIZE_IDPHOTO_MODEL,
                GlobalSet.RECOGNIZE_VIS_MODEL,
                ""
            ) { code, response ->
                if (code == 0) {
                    featureSucceed = true
                    if (detectSucceed && livenessSucceed && attributeSucceed) {
                        listener?.onSucceed()
                        onInitSucceed()
                    }
                } else {
                    listener?.onFailed(response)
                    onInitFailed()
                }
            }

        } catch (e: UnsatisfiedLinkError) {
            initLicense(context!!, FaceStatusCache.instance.faceLicence!!, listener)
        }
    }


    /**
     * minFaceSize	    检测最小人脸值	50
     * maxFaceSize	    检测最大人脸值	-1（不做限制）
     * trackInterval	人脸跟踪，检测的时间间隔	500
     * detectInterval	人脸跟踪，跟踪时间间隔	1000
     * noFaceSize	    非人脸阈值	0.5
     * pitch	        抬头低头角度	15
     * yaw	            左右摇头角度	15
     * yaw	            左右摇头角度	15
     * roll	            偏头角度	15
     * isCheckBlur	    是否进行模糊检测	false
     * isIllumination	是否进行光照检测	false
     * isOcclusion	    是否进行遮挡检测	false
     * detectMethodType	图片检测类型	VIS
     *
     * @return
     */
    private fun getFaceEnvironmentConfig(): FaceEnvironment {
        faceEnvironment.setMinFaceSize(50)
        faceEnvironment.setMaxFaceSize(-1)
        faceEnvironment.setDetectInterval(1000)
        faceEnvironment.setTrackInterval(1000)
        faceEnvironment.setNoFaceSize(0.5f)
        faceEnvironment.setPitch(30)
        faceEnvironment.setYaw(30)
        faceEnvironment.setRoll(30)
        faceEnvironment.setCheckBlur(true)
        faceEnvironment.setOcclusion(true)
        faceEnvironment.setIllumination(true)
        /**
         * 检测方法类型设置，目前两种支持FACEBOX_VIS和FACEBOX_NIR，建议FACEBOX_VIS 可见光检测
         */
        faceEnvironment.setDetectMethodType(FaceDetect.DetectType.DETECT_VIS)
        return faceEnvironment
    }


    fun setCallback(callback: FaceDetectCallBack?) {
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

    @Volatile
    private var argbData: IntArray? = null

    fun recognition(data: ByteArray, srcWidth: Int, srcHeight: Int) {
        if (data == null) return
        onDetectCheck(data, srcWidth, srcHeight)
        /*if (future3 != null && !future3!!.isDone()) {
            return
        }

        future3 = es3.submit {
            if (argbData == null) {
                Log.d(TAG, "数据转换前=" +System.currentTimeMillis())
                val yuv420 = yuv422To420(data, srcWidth, srcHeight)
                Log.d(TAG, "数据转换后=" +System.currentTimeMillis() + "yuv420=" + yuv420)

                val argb = IntArray(srcWidth * srcHeight)
                faceDetect.getDataFromYUVimg(
                    yuv420,
                    argb,
                    srcWidth,
                    srcHeight,
                    CarBoxCache.instance.getCameraConfig().rotation,
                    0
                )
                argbData = argb
            }


            Log.d(TAG, "去识别=" +System.currentTimeMillis() )

            checkData(srcWidth, srcHeight)
        }*/


    }

    @Synchronized
    private fun checkData(srcWidth: Int, srcHeight: Int) {
        if (argbData != null) {
//            onDetectCheck(argbData!!,srcWidth, srcHeight )
            argbData = null
        }
    }


    /**
     * 人脸追踪
     */
    @Synchronized
    fun onDetectCheck(data: ByteArray, srcWidth: Int, srcHeight: Int) {
        if (!checkInited()) {
            return
        }
        var isLiveness = false
        if (data == null) {
            return
        }

        if (future != null && !future!!.isDone()) {
            return
        }

        future = es.submit {
            Log.d("人脸识别", "数据转换前=" + System.currentTimeMillis())
            val yuv420 = yuv422To420(data, srcWidth, srcHeight)
            Log.d("人脸识别", "数据转换后=" + System.currentTimeMillis() + "yuv420=" + yuv420)

            val argb = IntArray(srcWidth * srcHeight)

            faceDetect.getDataFromYUVimg(
                yuv420,
                argb,
                srcWidth,
                srcHeight,
                config!!.rotation,
                0
            )

//Log.d(TAG, "argb="+argb)
            val faces = trackMaxFace(argb, srcWidth, srcHeight)

            Log.d(TAG, "face=" + faces)
            val livenessModel = LivenessModel()
            livenessModel.imageFrame.argb = argb
            livenessModel.imageFrame.width = srcWidth
            livenessModel.imageFrame.height = srcHeight

            if (faces != null && faces.size > 0) {
                livenessModel.trackFaceInfo = faces
                val faceInfo = faces[0]
                livenessModel.landmarks = faceInfo.landmarks
                livenessModel.faceInfo = faceInfo
                livenessModel.faceID = faceInfo.face_id

                callback?.onFaceDetectCallback(
                    true, faceInfo.mWidth.toInt(),
                    faceInfo.mWidth.toInt(), faceInfo.mCenter_x.toInt(), faceInfo.mCenter_y.toInt(),
                    srcWidth, srcHeight
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

    }


    private var liveType = 0

    private fun livenessFeatures(livenessModel: LivenessModel) {
        if (future2 != null && !future2!!.isDone) {
            return
        }

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


    /**
     * 人脸比对
     */
    private fun filterFeature(livenessModel: LivenessModel) {
        featureProcessor.onNext(livenessModel)
    }

    /**
     * 情绪识别
     */
    private fun startEnmotion(livenessModel: LivenessModel) {
        Log.d("人脸识别", "开始识别情绪")
        enmotionProcessor.onNext(livenessModel)
    }


    /**
     * 是否要做情绪识别
     */
    private fun isEnmotion(): Boolean {
//        return pass
        return true
    }

    /**
     * 是否要做人脸比对
     */
    private fun isFeature(): Boolean {
        return true
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

    private fun trackMaxFace(argb: IntArray, width: Int, height: Int): Array<FaceInfo>? {
        val minFaceSize = getFaceEnvironmentConfig().getMinFaceSize()
        if (width < minFaceSize || height < minFaceSize) {
            return null
        }

        return faceDetect.trackMaxFace(argb, height, width)
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


    private fun getFeatureLRUCache(): LRUCache<String, Feature> {
        return featureLRUCache
    }

    private fun getFeature(
        featureType: FaceFeature.FeatureType,
        curFeature: ByteArray,
        liveModel: LivenessModel
    ): Feature? {

        if (this.featureLRUCache.all.isNotEmpty()) {
            for (featureEntry: Map.Entry<String, Feature> in featureLRUCache.all) {
                val feature = featureEntry.value
                val similariry: Float
                if (featureType == FaceFeature.FeatureType.FEATURE_VIS) {
                    similariry = featureCompare(feature.getFeature(), curFeature)
                    if (similariry > 0.8f) {
                        liveModel.featureScore = similariry
                        featureLRUCache.put(feature.getUserName(), feature)
                        return feature
                    }
                }
            }
        }

        val featureCpp = faceFeature.featureCompareCpp(
            curFeature, featureType, 0.8f
        )

        if (featureCpp != null) {
            liveModel.featureScore = featureCpp.score
            val features = DBManager.getInstance().queryFeatureById(featureCpp.id)
            if (features != null && features!!.size > 0) {
                val feature = features!!.get(0)
                featureLRUCache.put(feature.getUserName(), feature)
                return feature
            }
        }
        return null
    }

    /**
     * 人脸特征比对,并且映射到0--100
     *
     * @param feature1
     * @param feature2
     * @return
     */
    private fun featureCompare(feature1: ByteArray, feature2: ByteArray): Float {
        val v = faceFeature.featureCompare(FaceFeature.FeatureType.FEATURE_VIS, feature1, feature2)
        Log.d("百度", "featureCompare: 是否一致$v")
        return v
    }

    private fun setFeature(): Int {
        val listFeatures = DBManager.getInstance().queryFeature()
        if (listFeatures != null && faceFeature != null) {
            Log.d(TAG, "setFeature: r人脸库的数量listFeatures.size()=" + listFeatures!!.size)
            faceFeature.setFeature(listFeatures)
            return listFeatures!!.size
        }
        return 0
    }

    private fun parseFaceEnmition(emotions: BDFaceSDKEmotions?): String {
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


    fun stop() {

    }


}