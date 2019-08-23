package us.nonda.commonibrary.http

import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query
import us.nonda.commonibrary.model.FacePictureModel
import us.nonda.commonibrary.model.FaceSerialModel

interface ApiService {


    @GET("/api/v1/vehiclebox/download/facepic")
    fun getFacepicture(@Query(value = "imei") imei:String):Observable<BaseResult<FacePictureModel>>

    @GET("/api/v1/vehiclebox/getserialnum")
    fun getSerialNum(@Query(value = "imei") imei:String):Observable<BaseResult<FaceSerialModel>>
}