package us.nonda.facelibrary.utils

import com.baidu.idl.facesdk.model.Feature
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.facelibrary.db.DBManager
import us.nonda.facelibrary.db.FaceApi
import us.nonda.facelibrary.manager.FaceSDKManager
import us.nonda.facelibrary.manager.UserInfoManager

/**
 * Created by chenjun on 2019-05-30.
 */
object DeleteFaceUtil {
    private val mUserInfoListener = UserListener()
    fun deleteFace(deleteSuccessCallback: () -> Unit) {
        mUserInfoListener.deleteSuccessCallback = deleteSuccessCallback
        // 初始化数据库
//        DBManager.getInstance().init(AppUtils.context)
        // 读取数据库信息
        UserInfoManager.getInstance().getFeatureInfo(
            null,
            mUserInfoListener
        )
    }
}


class UserListener : UserInfoManager.UserInfoListener() {
    var deleteSuccessCallback: (() -> Unit)? = null
    // 人脸库信息查找成功
    override fun featureQuerySuccess(listFeatureInfo: List<Feature>?) {
        if (!listFeatureInfo.isNullOrEmpty()) {
            for (item in listFeatureInfo) {
                FaceApi.getInstance().featureDelete(item)
            }
            FaceSDKManager.instance.getFeatureLRUCache().clear()
        }
        deleteSuccessCallback?.invoke()
    }

    // 人脸库信息查找失败
    override fun featureQueryFailure(message: String) {

    }

    // 显示删除进度条
    override fun showDeleteProgressDialog(progress: Float) {

    }

    // 删除成功
    override fun deleteSuccess() {
    }
}