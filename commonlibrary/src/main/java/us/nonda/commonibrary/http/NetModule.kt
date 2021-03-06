package us.nonda.commonibrary.http

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import us.nonda.commonibrary.BuildConfig
import us.nonda.commonibrary.config.CarboxConfigRepostory
import java.util.concurrent.TimeUnit

class NetModule private constructor() {

    private val DEFAULT_TIMEOUT: Long = 30
    private val BASE_URL = CarboxConfigRepostory.instance.getHttpUrl()

    private  var retrofit: Retrofit

    private var api: ApiService? = null

    companion object {
        val instance: NetModule by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            NetModule()
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