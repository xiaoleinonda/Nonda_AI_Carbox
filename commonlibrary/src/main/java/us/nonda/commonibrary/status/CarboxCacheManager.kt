package us.nonda.commonibrary.status

import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.SPUtils

class CarboxCacheManager private constructor() {


    private val SP_KEY_ACC_STATUS = "sp_key_acc_status"
    private val SP_KEY_SLEEP_TIME = "sp_key_sleep_time"

    companion object {
        val instance: CarboxCacheManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            CarboxCacheManager()
        }
    }

    fun putTime(currentTimeMillis: Long) {
        SPUtils.put(AppUtils.context, SP_KEY_SLEEP_TIME, currentTimeMillis)
    }

    fun getSleepTime() = SPUtils.get(AppUtils.context, SP_KEY_SLEEP_TIME, 0)

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