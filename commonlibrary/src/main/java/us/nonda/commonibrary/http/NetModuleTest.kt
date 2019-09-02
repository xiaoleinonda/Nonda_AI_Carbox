package us.nonda.commonibrary.http

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class NetModuleTest private constructor() {

    private val DEFAULT_TIMEOUT: Long = 30

    private val BASE_URL = "http://10.0.0.90:8081"

    private  var retrofit: Retrofit

    private var api: ApiService? = null

    companion object {

        val instance: NetModuleTest by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            NetModuleTest()
        }
    }

    init {
        retrofit = Retrofit.Builder()
            .client(getOkHttpClient())
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
    }

    private fun getOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(ILoggingInterceptor())
            .retryOnConnectionFailure(true)
            .build()
    }

    fun provideAPIService(): ApiService {
        if (api == null) {
            api = retrofit.create(ApiService::class.java)
        }
        return api!!
    }


}