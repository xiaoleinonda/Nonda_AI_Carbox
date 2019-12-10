package us.nonda.facelibrary.manager

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.baidu.idl.facesdk.*
import com.baidu.idl.facesdk.callback.Callback
import com.baidu.idl.facesdk.model.Feature
import us.nonda.cameralibrary.status.CameraStatus
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.commonibrary.http.NetModule
import us.nonda.commonibrary.utils.*
import us.nonda.facelibrary.auth.FaceAuthManager
import us.nonda.facelibrary.auth.IFaceAuthCallback
import us.nonda.facelibrary.callback.FaceDetectCallBack
import us.nonda.facelibrary.common.FaceEnvironment
import us.nonda.facelibrary.config.FaceConfig
import us.nonda.facelibrary.db.DBManager
import us.nonda.facelibrary.db.FaceApi
import us.nonda.facelibrary.db.LRUCache
import us.nonda.facelibrary.model.*
import us.nonda.facelibrary.model.FaceImage
import us.nonda.facelibrary.status.FaceStatusCache
import us.nonda.mqttlibrary.mqtt.MqttManager
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.lang.Exception
import java.util.concurrent.ExecutorService


class FaceSDKManager2 private constructor() {


    private lateinit var faceDetect: FaceDetect
    private lateinit var faceFeature: FaceFeature
    private lateinit var faceLive: FaceLive
    private lateinit var faceEnvironment: FaceEnvironment
    private lateinit var faceAttribute: FaceAttributes
    private var faceLivenessManager: FaceLiveness? = null


    private var context: Context = AppUtils.context


    var config: FaceConfig? = null
    private val initES: ExecutorService = Executors.newSingleThreadExecutor()
    private var initFeature: Future<*>? = null


    private var status = STATUS_INIT

    private val featureLRUCache = LRUCache<String, Feature>(1000)

    private var faceCache: FaceStatusCache = FaceStatusCache.instance
    val threadForInit = Executors.newSingleThreadExecutor()
    val futureForInit: Future<*>? = null

    var isRegisted = false

    private var faceFreq: Long = 500
    private var facePreTime: Long = 0

    private var callback: FaceDetectCallBack? = null

    companion object {
        const val TAG = "FaceSDKManager2"
        const val STATUS_INIT = 0;//未初始化
        const val STATUS_INITING = 1;//初始化中
        const val STATUS_INITED = 2//已初始化

        val instance: FaceSDKManager2 by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            FaceSDKManager2()
        }
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
        if (currentTimeMillis - facePreTime > faceFreq) {
            facePreTime = currentTimeMillis
            faceLivenessManager?.setCallback(callback)
            faceLivenessManager?.dealData(data)
        }
    }


    fun init() {
        if (futureForInit != null && !futureForInit.isDone) {
            return
        }
        threadForInit.submit {
            if (status == STATUS_INITED) {
                checkRegistFaceStatus()
            } else {
                checkLicenceStatus()
            }
        }

    }

    private fun checkLicenceStatus() {
        MyLog.d(TAG, "checkLicenceStatus  status=$status")
        if (faceCache.isLicence()) {
            log("已激活 直接初始化")
            initModel()
        } else {
            log("未激活 开始请求序列号")
            getLicenceStrHttp()
        }
    }

    fun getLicence(): String {
        return FaceAuth().getDeviceId(context)
    }

    @SuppressLint("CheckResult")
    private fun getLicenceStrHttp() {
        if (!NetworkUtil.getConnectivityStatus(AppUtils.context)) {
            return
        }

        val deviceId = getLicence()
        val imeiCode = DeviceUtils.getIMEICode(context)
        MyLog.d(TAG, "获取序列号， 指纹ID=$deviceId")
        val url = CarboxConfigRepostory.instance.getHttpUrl() + CarboxConfigRepostory.URL_SERIALNUM
        NetModule.instance.provideAPIService()
            .getSerialNum(url, imeiCode, deviceId!!)
            .retry(2)
            .subscribe({
                MyLog.d(TAG, "获取序列号，成功 it=${it.toString()}")

                if (it.code == 200 && it.data != null) {
                    MyLog.d(TAG, "获取序列号，成功 serialNum=${it.data!!.serialNum}")

                    val data = it.data
                    if (data!!.reslut) {
                        onGetSerialNumSucceed(data.serialNum)
                    } else {
                        onGetSerialNumFailed(data.content)
                    }
                } else {
                    onGetSerialNumFailed(it.msg)
                }
            }, {
                MyLog.d(TAG, "获取序列号， 异常it=${it.message}")

                it.message?.let { it1 -> onGetSerialNumFailed(it1) }
            })

    }

    private fun onGetSerialNumFailed(content: String) {
        log("请求序列号失败=$content")

    }

    private fun onGetSerialNumSucceed(serialNum: String) {
        log("请求序列号成功")
        initLicence(serialNum)
    }

    private fun initLicence(license: String?) {
        if (TextUtils.isEmpty(license)) {
            return
        }

        if (status != STATUS_INIT) {
            log("已经在激活状态status=$status  ")
            return
        }

        if (!NetworkUtil.getConnectivityStatus(context)) {
            log("initLicense net error")
        }

        log("开始激活")

        FaceAuthManager().initLicense(context, license!!, object : IFaceAuthCallback {
            override fun onFailed(msg: String) {
//                    listener?.onFailed("设备激活失败：$msg")
//                    FaceStatusCache.instance.faceLicence = ""
                log("激活失败=$msg")
                MqttManager.getInstance().publishEventData(1002, "2")
            }

            override fun onSucceed() {
                MqttManager.getInstance().publishEventData(1002, "1")

                FaceStatusCache.instance.faceLicence = license
                log("激活成功")
                initModel()
            }
        })
    }

    @Synchronized
    fun initModel() {
        if (CameraStatus.instance.getAccStatus() == 0) {
            return
        }

        if (status != STATUS_INIT) {
            log("已经在初始化状态status=$status")
            return
        }
        log("开始初始化模型")
        status = STATUS_INITING
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
//                            listener?.onSucceed()
                            onInitSucceed()
                        }
                    } else {
//                        listener?.onFailed(response)
                        onInitFailed(response)
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
//                            listener?.onSucceed()
                            onInitSucceed()
                        }
                    } else {
//                        listener?.onFailed(response)
                        onInitFailed(response)
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
                        onInitSucceed()
                    }
                } else {
                    onInitFailed(response)
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
//                        listener?.onSucceed()
                        onInitSucceed()
                    }
                } else {
//                    listener?.onFailed(response)
                    onInitFailed(response)
                }
            }

        } catch (e: UnsatisfiedLinkError) {
            //进到这里说明未激活， 需要重新激活
            log("激活状态失效 重新激活")
            status = STATUS_INIT
            initLicence(FaceStatusCache.instance.faceLicence)
        }
    }


    fun initSDK() {
        faceDetect = FaceDetect()
        faceFeature = FaceFeature()
        faceLive = FaceLive()
        faceAttribute = FaceAttributes()
        faceEnvironment = FaceEnvironment()
    }


    /**
     *
     * 初始化model成功
     *
     */
    private fun onInitSucceed() {
        MqttManager.getInstance().publishEventData(1003, "1")

        status = STATUS_INITED
        log("初始化成功")
//        setFeature()
        this.config = config ?: FaceConfig()

        faceLivenessManager = FaceLiveness(faceDetect, faceLive, faceFeature, faceAttribute)
        faceLivenessManager?.initConfig(config!!)

        //检查人脸图片
        checkRegistFaceStatus()
    }

    private fun onInitFailed(msg: String) {
        MqttManager.getInstance().publishEventData(1003, "2")
        status = STATUS_INIT
        log("初始化失败:$msg")
    }

    fun isInitSucceed(): Boolean {
        return status == STATUS_INITED
    }

    fun isActiveSucceed(): Boolean {
        return FaceStatusCache.instance.isLicence()
    }

    fun getFaceLicence(): String? {
        return FaceStatusCache.instance.faceLicence
    }

    fun checkRegistFaceStatus() {
        if (CameraStatus.instance.getAccStatus() == 0) {
            return
        }
        if (isRegisted) {
            MyLog.d(TAG, "已经注册过人脸了")
            return
        }
        log("检测人脸图片状态")
        registerRetryCount = 0
        val facePicture = faceCache.facePicture
        if (TextUtils.isEmpty(facePicture)) {
            getHttpFacePicture()
        } else {
            registFace(facePicture!!)
        }
    }

    private var registerRetryCount = 0

    fun registFace(facePicture: String) {
        if (status != STATUS_INITED || TextUtils.isEmpty(facePicture)) {
            log("sdk 还未初始化 不能注册")
            return
        }
        if (CameraStatus.instance.getAccStatus() == 0) {
            return
        }
        val faceImage = FaceImage(facePicture, "nonda")
        val faceRegister = FaceRegister(faceDetect, faceFeature)
        val registResult = faceRegister.registFace(faceImage)


        MyLog.d(TAG, "人脸注册结束  registResult=$registResult， registerRetryCount=$registerRetryCount")

        /**
         * 如果注册失败 就重试2次
         */
        if (registResult == -1 && registerRetryCount < 4) {
            registerRetryCount++
            try {
                Thread.sleep(3000)
            } catch (e: Exception) {
            }
            MyLog.d(TAG, "人脸注册失败， 开始重新请求接口注册。 重试次数=$registerRetryCount")
            getHttpFacePicture()
        }
    }


    /**
     * http请求注册人脸的图片
     */
    @SuppressLint("CheckResult")
    fun getHttpFacePicture() {
        if (CameraStatus.instance.getAccStatus() == 0) {
            return
        }
        log("开始请求人脸图片")
        if (!NetworkUtil.getConnectivityStatus(AppUtils.context)) {
            return
        }

        val url = CarboxConfigRepostory.instance.getHttpUrl() + CarboxConfigRepostory.URL_FACE_PICTURE

        val imeiCode = DeviceUtils.getIMEICode(context)
        NetModule.instance.provideAPIService()
            .getFacepicture(url, imeiCode)
            .retry(2)
            .subscribe({
                if (it.code == 200 && it.data != null) {
                    val data = it.data
                    if (data!!.reslut) {
                        onGetFacePictureSucceed(data.pic)
                    } else {
                        onGetFacePictureFailed(data.content)
                    }
                } else {
                    onGetFacePictureFailed(it.msg)
                }
            }, {
                it.message?.let { it1 -> onGetFacePictureFailed("异常：" + it1) }
            })

    }


    /**
     * 服务器获取图片成功
     */
    private fun onGetFacePictureSucceed(facePicture: String) {
        MqttManager.getInstance().publishEventData(1012, "1")

        if (CameraStatus.instance.getAccStatus() == 0) {
            return
        }
        log("请求人脸图片成功")
        registFace(facePicture!!)

    }

    /**
     * 服务器获取图片失败
     */
    private fun onGetFacePictureFailed(msg: String) {
        MqttManager.getInstance().publishEventData(1012, "2")

        if (CameraStatus.instance.getAccStatus() == 0) {
            return
        }
        log("请求人脸图片失败=$msg")

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

    fun onCameraClose() {
        faceLivenessManager?.stop()
        featureLRUCache.clear()
        clearFace()
        /*      val listFeatures = DBManager.getInstance().queryFeature()
              if (listFeatures != null && listFeatures.size > 0) {
                  for (listFeature in listFeatures) {
                      FaceApi.getInstance().featureDelete(listFeature)
                  }
              }*/
    }

    fun getMinFaceSize() = faceEnvironment?.getMinFaceSize()

    fun setFeature(): Int {
        val listFeatures = DBManager.getInstance().queryFeature()
        if (listFeatures != null && faceFeature != null) {
            log("setFeature: r人脸库的数量listFeatures.size()=" + listFeatures!!.size)
            faceFeature.setFeature(listFeatures)

            log(
                "faceFeature=$faceFeature  groupId=${listFeatures[0].groupId}" +
                        "id=${listFeatures[0].id}" +
                        "faceToken=${listFeatures[0].faceToken}"
            )
            return listFeatures!!.size
        }
        return 0
    }

    fun getFaceFeature(
        featureType: FaceFeature.FeatureType,
        curFeature: ByteArray,
        liveModel: LivenessModel
    ): Feature? {
        log("cache=${featureLRUCache.getAll().size}       curFeature=${curFeature.size}")

        /* if (this.featureLRUCache.all.isNotEmpty()) {
             for (featureEntry: Map.Entry<String, Feature> in featureLRUCache.all) {
                 val feature = featureEntry.value
                 val similariry: Float

                 if (featureType == FaceFeature.FeatureType.FEATURE_VIS) {
                     similariry = featureCompare(feature.getFeature(), curFeature)
                     log("similariry=$similariry")
                     if (similariry > 0.8f) {
                         liveModel.featureScore = similariry
                         featureLRUCache.put(feature.getUserName(), feature)
                         return feature
                     }
                 }
             }
         }*/
        val queryFeature = DBManager.getInstance().queryFeature()
        if (queryFeature != null && queryFeature.size > 0) {
            val feature = queryFeature[0]
            val v = faceFeature.featureCompare(FaceFeature.FeatureType.FEATURE_VIS, feature.feature, curFeature)
            liveModel.featureScore = v
            MyLog.d(TAG, "比对结果featureCompare=$v")
            if (v >= 70f) {
                return feature;
            }
        } else {
            MyLog.d(TAG, "还没有注册人脸")
        }

        /*    val featureCpp = faceFeature.featureCompareCpp(
                curFeature, featureType, 90f
            )
            log("faceFeature=$faceFeature   featureCpp=$featureCpp")

            if (featureCpp != null) {
                liveModel.featureScore = featureCpp.score
                log("Id=" + featureCpp.id)
                val queryFeature = DBManager.getInstance().queryFeature()
                val features = DBManager.getInstance().queryFeatureById(featureCpp.id)
                if (features != null && features.size > 0) {
                    val feature = features.get(0)
                    log("注册成功" + features.size)

                    featureLRUCache.put(feature.getUserName(), feature)
                    return feature
                }
            }*/
        return null
    }

    fun clearFace() {
        val listFeatures = DBManager.getInstance().queryFeature()
        if (listFeatures != null && listFeatures.size > 0) {
            for (listFeature in listFeatures) {
                FaceApi.getInstance().featureDelete(listFeature)
            }
        }
        isRegisted = false
        faceCache.clearFacePicture()
        faceCache.clearFacePassStatus()
        MyLog.d(TAG, "人脸删除成功")


    }

    private fun log(msg: String) {
        MyLog.d(TAG, "$msg  Thread=${Thread.currentThread().name}")
    }
}