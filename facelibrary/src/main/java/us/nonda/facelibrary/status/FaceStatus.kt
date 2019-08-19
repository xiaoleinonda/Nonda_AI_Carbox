package us.nonda.facelibrary.status

import android.content.Context
import com.google.gson.Gson
import us.nonda.commonibrary.utils.SPUtils

class FaceStatus private constructor(private var context: Context) {

    companion object {
        val SP_KEY = "FaceStatus"

        const val STATUS_INIT = 0;//未初始化
        const val STATUS_INITING = 1;//初始化中
        const val STATUS_INITED = 2//已初始化


        private var instance: FaceStatus? = null
        fun get(context: Context): FaceStatus {
            if (instance == null) {
                val json = SPUtils.get(context, SP_KEY, "") as String
                val faceStatus = Gson().fromJson(json, FaceStatus::class.java)
                if (faceStatus == null) {
                    instance = FaceStatus(context)
                } else {
                    instance = faceStatus
                }
            }
            return instance!!
        }
    }

    /**
     * 初始化状态
     */
     var faceInitStatus: Int = 0


    /**
     * 人脸识别状态
     * 0正在人脸识别
     */
    var recognitionStatus: Int = -1

    /**
     * 情绪识别状态
     * 0正在情绪识别
     */
    var enmotionStatus: Int = -1

    /**
     * 人脸比对状态
     * 0正在人脸比对
     */
    var faceCheckStatus: Int = -1


    private var faceLicense:String = ""


    /**
     * 人脸是否比对成功
     * 0是成功
     */
   private var facePassStatus: Boolean = false



    fun getFaceLicense() = faceLicense

    fun setFaceLicense(license:String){
        faceLicense = license
        updateCache()
    }



    fun getFacePass() = facePassStatus

    fun resetStatus() {
        faceInitStatus = 0
        enmotionStatus = -1
        faceCheckStatus = -1
        facePassStatus = false
        SPUtils.remove(context, SP_KEY)
    }

    fun updateCache() {
        val json = Gson().toJson(this)
        SPUtils.put(context, SP_KEY, json)
    }
}