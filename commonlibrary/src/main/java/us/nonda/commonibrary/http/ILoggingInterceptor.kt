package us.nonda.commonibrary.http

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class ILoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request =
            chain.request().newBuilder().addHeader("Content-type", "application/json")
                .addHeader("token", "7c09b979489a4bca8684c0922bb8a0e7").build()
        Log.d("网络", "发送请求${request.url()}")
        return chain.proceed(request)
    }
}