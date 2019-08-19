package us.nonda.facelibrary.auth

import android.content.Context
import com.baidu.idl.facesdk.FaceAuth
import com.baidu.idl.facesdk.callback.AuthCallback
import com.baidu.idl.facesdk.callback.Callback
import com.baidu.idl.facesdk.model.BDFaceSDKCommon

class FaceAuthManager {

    val SIM_ID = "118BB9401E107C84AC1708900C29DBB6ED"
    val WIFI_ID = "15CDCF3615CFE44BFC4EB4900C29DBB6ED"
    val WIFI_ID2 = "150364FC114FBFE5F1AF7E900C29DBB6ED"
    val WIFI_ID_NEW_ROM = "150364FC114FBFE5F1AF7E900C29DB5892"
     fun initLicense(context: Context, licenseID:String, callback: IFaceAuthCallback) {
        val faceAuth = FaceAuth()
        faceAuth.setActiveLog(FaceAuth.BDFaceLogInfo.BDFACE_LOG_ALL_MESSAGE)
        faceAuth.setAnakinThreadsConfigure(2, 0)


         var deviceId =faceAuth.getDeviceId(context)
         var licenseIDNew:String = ""
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

         initLicenseOnLine(context, faceAuth, licenseIDNew, callback)

     }

    private fun initLicenseOnLine(
        context: Context,
        faceAuth: FaceAuth,
        licenseID: String,
        callback: IFaceAuthCallback
    ) {

        faceAuth.initLicenseOnLine(context, licenseID, object : AuthCallback {
            override fun onResponse(p0: Int, p1: String?, p2: String?) {
                if (p0 == 0) {
                    callback.onSucceed()
                } else {
                    callback.onFailed(p1 ?: "")
                }
            }


        })
    }
}