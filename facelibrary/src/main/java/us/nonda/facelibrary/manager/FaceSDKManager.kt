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
import java.io.InputStreamReader


class FaceSDKManager private constructor() {


    private lateinit var faceDetect: FaceDetect
    private lateinit var faceFeature: FaceFeature
    private lateinit var faceLive: FaceLive
    private lateinit var faceEnvironment: FaceEnvironment
    private lateinit var faceAttribute: FaceAttributes
    private var faceLivenessManager: FaceLiveness? = null


    private var context: Context? = null


    var config: FaceConfig? = null


    private var status = STATUS_INIT

    private val featureLRUCache = LRUCache<String, Feature>(1000)


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
        faceFreq = this.config!!.faceFreq
    }


    fun initSDK() {
        initConfig(null)
        faceDetect = FaceDetect()
        faceFeature = FaceFeature()
        faceLive = FaceLive()
        faceAttribute = FaceAttributes()
        faceEnvironment = FaceEnvironment()
    }

    private fun onInitSucceed() {
        status = STATUS_INITED
        Log.d(TAG, "初始化成功")
        DBManager.getInstance().init(context)
        setFeature()
        faceLivenessManager = FaceLiveness(faceDetect, faceLive, faceFeature, faceAttribute)
        faceLivenessManager?.initConfig(config!!)

        getRegistFaceImage()
    }

    /**
     * http请求注册人脸的图片
     */
    private fun getRegistFaceImage() {


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


            faceLive.initModel(
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
        faceEnvironment.setDetectInterval(100)
        faceEnvironment.setTrackInterval(100)
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




    fun recognition(data: ByteArray, srcWidth: Int, srcHeight: Int) {
        if (status != STATUS_INITED) {
            return
        }

        if (data == null) return

        Log.d("FaceLiveness", "recognition=" + data.size)

        val currentTimeMillis = System.currentTimeMillis()
        if (currentTimeMillis - facePreTime >faceFreq) {
            facePreTime = currentTimeMillis
            faceLivenessManager?.setCallback(callback)
            faceLivenessManager?.dealData(data)
        }
    }


    fun getFeatureLRUCache(): LRUCache<String, Feature> {
        return featureLRUCache
    }



    fun getFaceFeature(
        featureType: FaceFeature.FeatureType,
        curFeature: ByteArray,
        liveModel: LivenessModel
    ): Feature? {
        Log.d("注册的事情", "cache=${featureLRUCache.getAll().size}       curFeature=${curFeature.size}")

        if (this.featureLRUCache.all.isNotEmpty()) {
            for (featureEntry: Map.Entry<String, Feature> in featureLRUCache.all) {
                val feature = featureEntry.value
                val similariry: Float

                if (featureType == FaceFeature.FeatureType.FEATURE_VIS) {
                    similariry = featureCompare(feature.getFeature(), curFeature)
                    Log.d("注册的事情", "similariry=$similariry")
                    if (similariry > 0.8f) {
                        liveModel.featureScore = similariry
                        featureLRUCache.put(feature.getUserName(), feature)
                        return feature
                    }
                }
            }
        }

        val featureCpp = faceFeature.featureCompareCpp(
            curFeature, featureType, 90f
        )
        Log.d("注册的事情", "featureCpp=$featureCpp")

        if (featureCpp != null) {
            liveModel.featureScore = featureCpp.score
            Log.d("注册的事情", "Id=" + featureCpp.id)

            val features = DBManager.getInstance().queryFeatureById(featureCpp.id)
            if (features != null && features!!.size > 0) {
                val feature = features!!.get(0)
                Log.d("注册的事情", "注册成功" + features.size)

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
        Log.d(TAG, "featureCompare: 是否一致$v")
        return v
    }

      fun setFeature(): Int {
        val listFeatures = DBManager.getInstance().queryFeature()
        if (listFeatures != null && faceFeature != null) {
            Log.d(TAG, "setFeature: r人脸库的数量listFeatures.size()=" + listFeatures!!.size)
            faceFeature.setFeature(listFeatures)
            return listFeatures!!.size
        }
        return 0
    }



    fun getMinFaceSize() = faceEnvironment?.getMinFaceSize()


    fun stop() {

    }

    private var faceFreq:Long = 500
    private var facePreTime:Long = 0

    fun registFace(){
        if (status != STATUS_INITED) {
            return
        }

        val string = StringUtils.getString(context?.assets?.open("imagedata2.txt"))
        val faceImage = FaceImage(string, "nonda")
        Log.d("注册", "解析的图片faceImage=" + string)
        val register = FaceRegister(faceDetect, faceFeature)
        register.registFace(faceImage)
    }




}