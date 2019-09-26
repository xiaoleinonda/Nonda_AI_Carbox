package us.nonda.commonibrary.http

import io.reactivex.Observable
import retrofit2.http.*
import us.nonda.commonibrary.model.*

interface ApiService {


    @GET
    fun getAppVersion(@Url url: String, @Query(value = "imei") imei: String, @Query(value = "appVersion") appVersion: String): Observable<BaseResult<AppVersionModel>>

    @GET
    fun getFacepicture(@Url url: String, @Query(value = "imei") imei: String): Observable<BaseResult<FacePictureModel>>

    @GET
    fun getSerialNum(@Url url: String, @Query(value = "imei") imei: String, @Query(value = "hwfingerprint") hwfingerprint: String): Observable<BaseResult<FaceSerialModel>>

    @POST
    fun postLicenceSucceed(@Url url: String, @Body body: PostLicenceBody): Observable<BaseResult<PostLicenceModel>>

    /**
     * 视频单文件上传
     */
    @POST("/api/v1/vehiclebox/upload")
    fun upload(@Url url: String, @Body body: UploadBody): Observable<BaseResult<UploadResponseModel>>

    /**
     * 初始化分片上传
     */
    @POST
    fun postInitPartUpload(@Url url: String, @Body body: InitPartUploadBody): Observable<BaseResult<InitPartUploadResponseModel>>

    /**
     * 上传分片
     */
    @POST("/api/v1/vehiclebox/partupload/upload")
    fun postPartUpload(@Url url: String, @Body body: PartUploadBody): Observable<BaseResult<PartUploadResponseModel>>

    /**
     * 完成分片上传
     */
    @POST
    fun postCompletePartUpload(@Url url: String, @Body body: CompletePartUploadBody): Observable<BaseResult<PartUploadResponseModel>>

    /**
     * 查询已上传分片
     */
    @GET("/api/v1/vehiclebox/partupload/query")
    fun getQueryPartUpload(@Url url: String, @Query(value = "uploadId") uploadId: String): Observable<BaseResult<QueryPartUploadModel>>
}