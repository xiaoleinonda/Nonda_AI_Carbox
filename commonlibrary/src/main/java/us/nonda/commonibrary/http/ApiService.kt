package us.nonda.commonibrary.http

import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query
import us.nonda.commonibrary.model.FacePictureModel

interface ApiService {


    @GET("/api/v1/vehiclebox/download/facepic")
    fun getFacepicture(@Query(value = "imei") imei:String):Observable<BaseResult<FacePictureModel>>
}