package us.nonda.facelibrary.status

import android.content.Context
import android.text.TextUtils
import com.google.gson.Gson
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.SPUtils

class FaceStatusCache private constructor() {

    private var context: Context = AppUtils.context

    companion object {
        val SP_KEY = "FaceStatusCache"
        val SP_KEY_LICENCE = "sp_key_licence"
        val SP_KEY_FACE_PASS_STATUS = "sp_key_face_pass_status"
        val SP_KEY_FACE_PICTURE = "sp_key_face_picture"

        val instance: FaceStatusCache by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            FaceStatusCache()
        }
    }

    /**
     * 人脸注册图片
     */
    var facePicture: String? = null
        get() {
            if (TextUtils.isEmpty(field)) {
                field = SPUtils.get(context, SP_KEY_FACE_PICTURE, "") as String
            }
            return field
        }
        set(value) {
            field = value
            SPUtils.put(context, SP_KEY_FACE_PICTURE, value)
        }


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


    var faceLicence: String? = null
        set(value) {
            field = value
            SPUtils.put(context, SP_KEY_LICENCE, value)
        }
        get() {
            if (TextUtils.isEmpty(field)) {
                field = SPUtils.get(context, SP_KEY_LICENCE, "") as String
            }
            return field
        }

    /**
     * 人脸是否比对成功
     * 0是成功
     */
    var facePassStatus: Int = -1
        set(value) {
            field = value
            SPUtils.put(context, SP_KEY_FACE_PASS_STATUS, value)
        }
        get() {
            if (facePassStatus == -1) {
                facePassStatus = SPUtils.get(context, SP_KEY_FACE_PASS_STATUS, -1) as Int
            }
            return facePassStatus
        }


    /**
     * 是否已经激活
     */
    fun isLicence() = !TextUtils.isEmpty(faceLicence)

    fun clearFacePicture(){
        facePicture = ""
    }

    fun clearFacePassStatus(){
        facePassStatus = -1
    }



    fun resetStatus() {
        enmotionStatus = -1
        faceCheckStatus = -1
    }


}