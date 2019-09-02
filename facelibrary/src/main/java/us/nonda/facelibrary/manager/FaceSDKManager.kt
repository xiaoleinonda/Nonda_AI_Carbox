package us.nonda.facelibrary.manager

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.baidu.idl.facesdk.*
import com.baidu.idl.facesdk.callback.Callback
import com.baidu.idl.facesdk.model.BDFaceSDKCommon
import com.baidu.idl.facesdk.model.BDFaceSDKEmotions
import com.baidu.idl.facesdk.model.FaceInfo
import com.baidu.idl.facesdk.model.Feature
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import us.nonda.cameralibrary.status.CameraStatus
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.http.NetModule
import us.nonda.commonibrary.model.PostLicenceBody
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.DeviceUtils
import us.nonda.commonibrary.utils.NetworkUtil
import us.nonda.facelibrary.`interface`.IFaceInitListener
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
import java.io.InputStreamReader
import java.util.concurrent.ExecutorService


class FaceSDKManager private constructor() {


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


    var isRegisted = false

    companion object {
        const val TAG = "FaceSDKManager"
        const val STATUS_INIT = 0;//未初始化
        const val STATUS_INITING = 1;//初始化中
        const val STATUS_INITED = 2//已初始化

        val instance: FaceSDKManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            FaceSDKManager()
        }
    }


    private var callback: FaceDetectCallBack? = null

    /**
     * 激活动作状态
     */
    private var activating = false

    private var future: Future<*>? = null

    val threadForInit = Executors.newSingleThreadExecutor()
    val futureForInit: Future<*>? = null

    fun initttt() {
        if (futureForInit!=null && !futureForInit.isDone){
            return
        }
        threadForInit.submit {

        }
    }


    var checking = false
    fun check() {
        if (checking) {
            return
        }
/*
        if (future != null && !future!!.isDone()) {
            return
        }
        future = newSingleThreadExecutor.submit {*/
        MyLog.d(TAG, "check  status=$status")
        if (status == STATUS_INIT) {
            checkLicenceStatus()
        }

        checkRegistFaceStatus()
//        }

    }

    fun check(checkRegist: Boolean) {
        if (status != STATUS_INIT) {
            checkLicenceStatus()
        }

        if (checkRegist) {
            checkRegistFaceStatus()
        }
    }


    @Synchronized
    fun initModel() {
        if (CameraStatus.instance.getAccStatus() == 0) {
            checking = false
            return
        }

        if (initFeature != null && !initFeature!!.isDone) {
            return
        }

        initFeature = initES.submit {
            executeInitModel()
        }

    }

    private fun executeInitModel() {
        if (status != STATUS_INIT) {
            log("已经在初始化状态status=$status")
            checking = false
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
//                            listener?.onSucceed()
                            onInitSucceed()
                        }
                    } else {
//                        listener?.onFailed(response)
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
//                        listener?.onSucceed()
                        onInitSucceed()
                    }
                } else {
//                    listener?.onFailed(response)
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
//                        listener?.onSucceed()
                        onInitSucceed()
                    }
                } else {
//                    listener?.onFailed(response)
                    onInitFailed()
                }
            }

        } catch (e: UnsatisfiedLinkError) {
            //进到这里说明未激活， 需要重新激活
            log("激活状态失效 重新激活")
            status = STATUS_INIT
            initLicence(FaceStatusCache.instance.faceLicence)
        }
    }


    /**
     * 每次应用启动初始化时调用
     */
    /*   @Synchronized
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


       }*/

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
/*
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
*/

/*
    fun init(context: Context, license: String, listener: IFaceInitListener?) {
        this.context = context.applicationContext

        if (FaceStatusCache.instance.isLicence()) {
            initModel(listener)
        } else {
            initLicense(context, license, listener)
        }

    }
*/

    private fun
            log(s: String) {
        MyLog.d(TAG, s)
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
        faceLivenessManager = FaceLiveness(faceDetect, faceLive, faceFeature, faceAttribute)
        faceLivenessManager?.initConfig(config!!)

        //检查人脸图片
        checkRegistFaceStatus()
    }


    /**
     * 判断是否需要注册人脸
     */

    private fun onInitFailed() {
        MqttManager.getInstance().publishEventData(1003, "2")

        status = STATUS_INIT
        log("初始化失败")
        checking = false

    }
/*

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

*/

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
        if (currentTimeMillis - facePreTime > faceFreq) {
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

        val featureCpp = faceFeature.featureCompareCpp(
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
        log("featureCompare: 是否一致$v")
        return v
    }

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


    fun getMinFaceSize() = faceEnvironment?.getMinFaceSize()


    fun stop() {
        faceLivenessManager?.stop()
        faceCache.clearFacePassStatus()
        faceCache.clearFacePicture()
        featureLRUCache.clear()


        val listFeatures = DBManager.getInstance().queryFeature()
        if (listFeatures != null && listFeatures.size > 0) {
            for (listFeature in listFeatures) {
                FaceApi.getInstance().featureDelete(listFeature)
            }
        }

        MyLog.d(TAG, "face stop成功")
        MyLog.d(TAG, "facePassStatus=${faceCache.facePassStatus}")
        MyLog.d(TAG, "clearFacePicture=${faceCache.facePicture}")
        MyLog.d(TAG, "featureLRUCache=${featureLRUCache.all.size}")
        MyLog.d(TAG, "数据库=${DBManager.getInstance().queryFeature()}")

    }

    fun onCameraClose() {
        faceLivenessManager?.stop()
        faceCache.clearFacePassStatus()
        featureLRUCache.clear()
        /*      val listFeatures = DBManager.getInstance().queryFeature()
              if (listFeatures != null && listFeatures.size > 0) {
                  for (listFeature in listFeatures) {
                      FaceApi.getInstance().featureDelete(listFeature)
                  }
              }*/
    }

    private var faceFreq: Long = 500
    private var facePreTime: Long = 0


    /****       licence         ****/


    /**
     * 查看激活状态
     */
    fun checkLicenceStatus() {
        MyLog.d(TAG, "checkLicenceStatus  status=$status")

        if (faceCache.isLicence()) {
            log("已激活 直接初始化")
            initModel()
        } else {
            log("未激活 开始请求序列号")
            getLicenceStrHttp()
        }
    }

    private var requestSerialNumDisposable: Disposable? = null
    private fun getLicenceStrHttp() {
        onGetSerialNumSucceed("123")// todo
        if (!NetworkUtil.getConnectivityStatus(AppUtils.context)) {
            checking = false

            return
        }

        if (TextUtils.isEmpty(deviceId)) {
            deviceId = FaceAuth().getDeviceId(context)
        }
        var imeiCode = DeviceUtils.getIMEICode(context)
          imeiCode = "869455047237132"
        log("IMEI号=$imeiCode")

        if (requestSerialNumDisposable != null && !requestSerialNumDisposable!!.isDisposed) {
            requestSerialNumDisposable!!.dispose()
        }
        requestSerialNumDisposable = NetModule.instance.provideAPIService()
            .getSerialNum(imeiCode, deviceId!!)
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .retry(2)
            .subscribe({
                if (it.code == 200 && it.data != null) {
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
                it.message?.let { it1 -> onGetSerialNumFailed(it1) }
            })

    }

    /**
     * 获取序列号失败
     */
    private fun onGetSerialNumFailed(content: String) {
        log("请求序列号失败=$content")
        checking = false

    }

    /**
     * 获取序列号成功
     */
    private fun onGetSerialNumSucceed(serialNum: String) {
        log("请求序列号成功")
        initLicence(serialNum)
        checking = false

    }


    private fun initLicence(license: String?) {
        if (TextUtils.isEmpty(license)) {
            return
        }

        if (status != STATUS_INIT || activating) {
            log("已经在激活状态status=$status   activating=$activating")
            return
        }

        if (!NetworkUtil.getConnectivityStatus(context)) {
            activating = false
            log("initLicense net error")
        }

        log("开始激活")
        activating = true

        FaceAuthManager().initLicense(context, license!!, object : IFaceAuthCallback {
            override fun onFailed(msg: String) {
//                    listener?.onFailed("设备激活失败：$msg")
//                    FaceStatusCache.instance.faceLicence = ""
                log("激活失败=$msg")
                activating = false
                MqttManager.getInstance().publishEventData(1002, "2")
            }

            override fun onSucceed() {
                MqttManager.getInstance().publishEventData(1002, "1")

                FaceStatusCache.instance.faceLicence = license
                log("激活成功")
                initModel()
                activating = false
            }
        })
    }


    /**********         Regist face          ***********/


    fun checkRegistFaceStatus() {
        if (CameraStatus.instance.getAccStatus() == 0) {
            checking = false

            return
        }

        if (isRegisted) {
            MyLog.d(TAG, "已经注册过人脸了")
            checking = false

            return
        }
        log("检测人脸图片状态")
        val facePicture = faceCache.facePicture
        if (TextUtils.isEmpty(facePicture)) {
            getHttpFacePicture()
        } else {
            registFace(facePicture!!)
        }
    }

    fun registFace(facePicture: String) {
        if (status != STATUS_INITED || TextUtils.isEmpty(facePicture)) {
            log("sdk 还未初始化 不能注册")
            checking = false

            return
        }
        if (CameraStatus.instance.getAccStatus() == 0) {
            checking = false

            return
        }
//        val string = StringUtils.getString(context?.assets?.open("imagedata2.txt"))
        val faceImage = FaceImage(facePicture, "nonda")
//        Log.d(TAG, "解析的图片faceImage=" + string)
        val register = FaceRegister(faceDetect, faceFeature)
        register.registFace(faceImage)
    }

    private var deviceId: String? = null

    private var requestFacePicDisposable: Disposable? = null
    /**
     * http请求注册人脸的图片
     */
    @SuppressLint("CheckResult")
    fun getHttpFacePicture() {
        if (CameraStatus.instance.getAccStatus() == 0) {
            checking = false

            return
        }
        log("开始请求人脸图片")
        if (!NetworkUtil.getConnectivityStatus(AppUtils.context)) {
            checking = false

            return
        }


        val imeiCode = DeviceUtils.getIMEICode(context)

        if (requestFacePicDisposable != null && !requestFacePicDisposable!!.isDisposed) {
            requestFacePicDisposable!!.dispose()
        }


        requestFacePicDisposable = NetModule.instance.provideAPIService()
            .getFacepicture("869455047237132")
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
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
                it.message?.let { it1 -> onGetFacePictureFailed(it1) }
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


}