package us.nonda.ai.app.ui.debug

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.yaoxiaowen.download.utils.ToastUtils
import kotlinx.android.synthetic.main.activity_debug.*
import us.nonda.ai.R
import us.nonda.commonibrary.BuildConfig
import us.nonda.commonibrary.MyLog
import us.nonda.commonibrary.config.CarboxConfigRepostory
import us.nonda.commonibrary.utils.DeviceUtils
import us.nonda.commonibrary.utils.NetworkUtil
import us.nonda.facelibrary.manager.FaceSDKManager2
import us.nonda.mqttlibrary.mqtt.MqttManager

class DebugActivity : AppCompatActivity() {

    private val TAG = "调试"

    companion object {
        fun starter(context: Context) {
            context.startActivity(Intent(context, DebugActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)


        val httpUrl = CarboxConfigRepostory.instance.getHttpUrl()
        if (httpUrl.isNullOrBlank()) {
            edt_url.setText("https://api-clouddrive.nondagroup.com")
        } else {
            edt_url.setText(httpUrl)
        }
        edt_url.setSelection(edt_url.length())


        val imeiCode = DeviceUtils.getIMEICode(this)
        if (imeiCode.isNullOrBlank()) {
            tv_imei.text = "设备IMEI：获取失败"
        } else {
            tv_imei.text = "设备IMEI：$imeiCode"
        }

        val authID = FaceSDKManager2.instance.getLicence()
        if (authID.isNullOrBlank()) {
            tv_auth_id.text = "硬件指纹：获取失败"
        } else {
            tv_auth_id.text = "硬件指纹：$authID"
        }

        val faceLicence = FaceSDKManager2.instance.getFaceLicence()
        tv_license
        if (faceLicence.isNullOrBlank()) {
            tv_license.text = "百度序列号：还未激活"
        } else {
            tv_license.text = "百度序列号：$faceLicence"
        }

        tv_active_state.text =
            "激活状态： ${FaceSDKManager2.instance.isActiveSucceed()}  模型初始化状态： ${FaceSDKManager2.instance.isInitSucceed()}"


        val simState = DeviceUtils.getSimState(this)
        val simNumber = DeviceUtils.getSimNumber(this)

        tv_sim.text = "sim卡状态：$simState       iccid: $simNumber   isConnected：${NetworkUtil.isConnected(this)}"
        tv_mqtt.text = "mqtt连接状态：${MqttManager.getInstance().isConnected()}"
        initListener()
    }

    private fun initListener() {
        btn_log.setOnClickListener {
            CarboxConfigRepostory.instance.putLogSwitch("1")
            MyLog.initSwitch()
            showToast("开启成功")
        }

        btn_url.setOnClickListener {
            val toString = edt_url.text.toString()
            if (!toString.isNullOrBlank()) {
                CarboxConfigRepostory.instance.putHttpUrl(toString)
                showToast("切换成功")
            }
        }

        tv_license.setOnClickListener {
            Log.d(TAG, tv_license.text.toString())
        }

        tv_imei.setOnClickListener {
            Log.d(TAG, tv_imei.text.toString())
        }
        tv_auth_id.setOnClickListener {
            Log.d(TAG, tv_auth_id.text.toString())
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
