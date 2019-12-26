package us.nonda.commonibrary.http

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.config.CarboxConfigRepostory

class ILoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request =
            chain.request().newBuilder().addHeader("Content-type", "application/json")
                .addHeader("token", CarboxConfigRepostory.HTTP_TOKEN).build()
        MyLog.d("网络", "发送请求${request.url()}")
        return chain.proceed(request)
    }
}