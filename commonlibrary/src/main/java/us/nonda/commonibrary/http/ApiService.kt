package us.nonda.commonibrary.http

import io.reactivex.Observable
import retrofit2.http.*
import us.nonda.commonibrary.model.*

interface ApiService {


    @GET("/api/v1/vehiclebox/check/appversion")
    fun getAppVersion(@Query(value = "imei") imei:String):Observable<BaseResult<AppVersionModel>>

    @GET("/api/v1/vehiclebox/download/facepic")
    fun getFacepicture(@Query(value = "imei") imei:String):Observable<BaseResult<FacePictureModel>>

    @GET("/api/v1/vehiclebox/getserialnum")
    fun getSerialNum(@Query(value = "imei") imei:String, @Query(value = "hwfingerprint")hwfingerprint:String):Observable<BaseResult<FaceSerialModel>>

    @POST("/api/v1/vehiclebox/confirmactivation")
    fun postLicenceSucceed(@Body body: PostLicenceBody):Observable<BaseResult<PostLicenceModel>>
}