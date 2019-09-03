package us.nonda.commonibrary.http

import io.reactivex.Observable
import retrofit2.http.*
import us.nonda.commonibrary.model.*

interface ApiService {


    @GET("/api/v1/vehiclebox/check/appversion")
    fun getAppVersion(@Query(value = "imei") imei: String, @Query(value = "appVersion") appVersion: String): Observable<BaseResult<AppVersionModel>>

    @GET("/api/v1/vehiclebox/download/facepic")
    fun getFacepicture(@Query(value = "imei") imei: String): Observable<BaseResult<FacePictureModel>>

    @GET("/api/v1/vehiclebox/getserialnum")
    fun getSerialNum(@Query(value = "imei") imei: String, @Query(value = "hwfingerprint") hwfingerprint: String): Observable<BaseResult<FaceSerialModel>>

    @POST("/api/v1/vehiclebox/confirmactivation")
    fun postLicenceSucceed(@Body body: PostLicenceBody): Observable<BaseResult<PostLicenceModel>>

    /**
     * 视频单文件上传
     */
    @POST("/api/v1/vehiclebox/upload")
    fun upload(@Body body: UploadBody): Observable<BaseResult<UploadResponseModel>>

    /**
     * 初始化分片上传
     */
    @POST("/api/v1/vehiclebox/partupload/init")
    fun postInitPartUpload(@Body body: InitPartUploadBody): Observable<BaseResult<InitPartUploadResponseModel>>

    /**
     * 上传分片
     */
    @POST("/api/v1/vehiclebox/partupload/upload")
    fun postPartUpload(@Body body: PartUploadBody): Observable<BaseResult<PartUploadResponseModel>>

    /**
     * 完成分片上传
     */
    @POST("/api/v1/vehiclebox/partupload/complete")
    fun postCompletePartUpload(@Body body: CompletePartUploadBody): Observable<BaseResult<PartUploadResponseModel>>

    /**
     * 查询已上传分片
     */
    @GET("/api/v1/vehiclebox/partupload/query")
    fun getQueryPartUpload(@Query(value = "uploadId") uploadId: String): Observable<BaseResult<QueryPartUploadModel>>
}