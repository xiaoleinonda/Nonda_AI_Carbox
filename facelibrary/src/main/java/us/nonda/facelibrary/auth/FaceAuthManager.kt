package us.nonda.facelibrary.auth

import android.content.Context
import com.baidu.idl.facesdk.FaceAuth
import com.baidu.idl.facesdk.callback.AuthCallback
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.commonibrary.http.NetModule
import us.nonda.commonibrary.model.PostLicenceBody
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.DeviceUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceAuthManager {

    private val TAG = "FaceAuthManager"
    private var disposable: Disposable? = null

    private val es: ExecutorService = Executors.newSingleThreadExecutor()

    fun initLicense(context: Context, licenseID: String, callback: IFaceAuthCallback) {
        val faceAuth = FaceAuth()
        faceAuth.setActiveLog(FaceAuth.BDFaceLogInfo.BDFACE_LOG_ALL_MESSAGE)
        faceAuth.setAnakinThreadsConfigure(2, 0)
        val deviceId = faceAuth.getDeviceId(context)
        initLicenseOnLine(context, deviceId, faceAuth, licenseID, callback)
    }

    private fun initLicenseOnLine(
        context: Context,
        deviceId: String,
        faceAuth: FaceAuth,
        licenseID: String,
        callback: IFaceAuthCallback
    ) {
        var testLicenseID = "IT37-09PA-BHD8-FYYS"
        faceAuth.initLicenseOnLine(context, licenseID) { p0, p1, p2 ->
            if (p0 == 0) {
                callback.onSucceed()
                postLicenseSucceed(licenseID, deviceId)
            } else {
                callback.onFailed(p1 ?: "")
            }
        }
    }


    private fun postLicenseSucceed(licenseID: String, deviceId: String) {
        if (disposable != null && !disposable!!.isDisposed) {
            disposable!!.dispose()
        }

        val imeiCode = DeviceUtils.getIMEICode(AppUtils.context)
        MyLog.d(TAG, "百度激活成功 开始提交激活状态imeiCode=$imeiCode")
        MyLog.d(TAG, "百度激活成功 开始提交激活状态licenseID=$licenseID")
        MyLog.d(TAG, "百度激活成功 开始提交激活状态deviceId=$deviceId")

        val url = CarboxConfigRepostory.instance.getHttpUrl() + CarboxConfigRepostory.URL_CONFIRMACTIVATION

        disposable = NetModule.instance.provideAPIService()
            .postLicenceSucceed(url, PostLicenceBody(imeiCode, licenseID, deviceId))
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .retry(3)
            .subscribe({
                MyLog.d(TAG, "百度激活成功 开始提交激活状态imeiCode=$imeiCode")
                val data = it.data
                if (it.code == 200 && data != null) {
                    if (data.reslut) {
                        MyLog.d("激活状态", "激活成功")
                    } else {
                        MyLog.d("激活状态", "激活失败=${data.content}")
                    }
                } else {
                    MyLog.d(TAG, "百度激活成功 提交激活状态失败")

                    MyLog.d("激活状态", "激活失败=${it.msg}")

                }
            }, {
                MyLog.d(TAG, "百度激活成功 提交激活状态异常=${it.message}")

            })
    }
}