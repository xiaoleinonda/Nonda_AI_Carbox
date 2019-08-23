package us.nonda.commonibrary.status

import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.SPUtils

class CarboxCacheManager private constructor(){

    private val SP_KEY_ACC_STATUS = "sp_key_acc_status"
    companion object{
        val instance:CarboxCacheManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED){
            CarboxCacheManager()
        }
    }

  /*  var status_acc:Int = -1
    var status_face_sdk_active:Int = -1
    var status_face_sdk_init:Int = -1
    var status_open_front_camera:Int = -1
    var status_open_back_camera:Int = -1
    var status_record_front_camera:Int = -1
    var status_record_back_camera:Int = -1
    var status_regist_face_image:Int = -1
    var status_regist_face_feature:Int = -1
    var status_mqtt_connect:Int = -1
    var status_new_apk:Int = -1*/




}