package us.nonda.facelibrary.auth

import android.content.Context
import android.text.TextUtils
import com.baidu.idl.facesdk.FaceAuth
import com.baidu.idl.facesdk.callback.AuthCallback
import com.baidu.idl.facesdk.callback.Callback
import com.baidu.idl.facesdk.model.BDFaceSDKCommon
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.http.NetModule
import us.nonda.commonibrary.model.PostLicenceBody
import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.DeviceUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceAuthManager {

    val SIM_ID = "118BB9401E107C84AC1708900C29DBB6ED"
    val WIFI_ID = "15CDCF3615CFE44BFC4EB4900C29DBB6ED"
    val WIFI_ID2 = "150364FC114FBFE5F1AF7E900C29DBB6ED"
    val WIFI_ID_NEW_ROM = "150364FC114FBFE5F1AF7E900C29DB5892"

    private var disposable: Disposable? = null

    private val es: ExecutorService = Executors.newSingleThreadExecutor()

    fun initLicense(context: Context, licenseID: String, callback: IFaceAuthCallback) {
        val faceAuth = FaceAuth()
        faceAuth.setActiveLog(FaceAuth.BDFaceLogInfo.BDFACE_LOG_ALL_MESSAGE)
        faceAuth.setAnakinThreadsConfigure(2, 0)
        var licenseIDNew: String = ""

        val imeiCode = DeviceUtils.getIMEICode(context)
        when (imeiCode) {
            "869455047237132" -> {
                licenseIDNew =   "LY77-J8DW-8YCZ-5X6L"//wifi

            }
            "869455047237124" -> {
//                licenseIDNew =   "6HDB-HCPB-B4PW-RQVS"//sim卡
                licenseIDNew =   "LY77-J8DW-8YCZ-5X6L"//wifi

            }
            "869455047237298" -> {
                licenseIDNew =   "BJHH-JFNI-WYP2-FNTL"//wifi

            }
            else -> {
            }
        }

      /*  var deviceId = faceAuth.getDeviceId(context)
        var licenseIDNew: String = ""
        if (SIM_ID == deviceId) {
            licenseIDNew = "EEJK-DAFA-X7HG-LU2G"
        } else if (WIFI_ID == deviceId) {
            licenseIDNew = "JJXH-SEIJ-EGF3-PWEQ"
        } else if (WIFI_ID2 == deviceId) {
            licenseIDNew = "EZMH-DDPY-KBZO-HCCA"
        } else if (WIFI_ID_NEW_ROM == deviceId) {
            licenseIDNew = "BHM9-9ODP-GD5M-2DOM"
        } else {
            licenseIDNew = "LY77-J8DW-8YCZ-5X6L"
        }
*/
        initLicenseOnLine(context, faceAuth, licenseIDNew, callback)

    }

    private fun initLicenseOnLine(
        context: Context,
        faceAuth: FaceAuth,
        licenseID: String,
        callback: IFaceAuthCallback
    ) {
//        var str = "6HDB-HCPB-B4PW-RQVS"

//        es.execute {
        faceAuth.initLicenseOnLine(context, licenseID, object : AuthCallback {
            override fun onResponse(p0: Int, p1: String?, p2: String?) {
                if (p0 == 0) {
                    callback.onSucceed()
                    postLicenseSucceed()
                } else {
                    callback.onFailed(p1 ?: "")
                }
            }


        })

//        }


    }


    private fun postLicenseSucceed() {
        if (disposable != null && !disposable!!.isDisposed) {
            disposable!!.dispose()
        }

        disposable = NetModule.instance.provideAPIService()
            .postLicenceSucceed(PostLicenceBody("869455047237132", "111", "123"))
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .retry(3)
            .subscribe({
                val data = it.data
                if (it.code == 200 && data != null) {
                    if (data.reslut) {
                        MyLog.d("激活状态", "激活成功")
                    } else {
                        MyLog.d("激活状态", "激活失败=${data.content}")
                    }
                } else {
                    MyLog.d("激活状态", "激活失败=${it.msg}")

                }
            }, { println("失败${it.message}") })
    }
}