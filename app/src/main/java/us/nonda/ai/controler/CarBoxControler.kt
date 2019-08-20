package us.nonda.ai.controler

import android.content.Context
import android.content.Intent
import us.nonda.ai.app.ui.VideoRecordActivity
import us.nonda.commonibrary.utils.FinishActivityManager
import us.nonda.facelibrary.manager.FaceSDKManager
import us.nonda.facelibrary.status.FaceStatusCache

class CarBoxControler constructor(private var context: Context) : ICarboxControl {

    companion object{
        val MODE_ACC_ON = 0
        val MODE_ACC_OFF = 1
    }

    init {
//        initFace()
    }


    /**
     * 切换模式
     */
    override fun mode(mode: Int) {
        when (mode) {
            MODE_ACC_ON -> {
//                FaceSDKManager.instance.init(context, "LY77-J8DW-8YCZ-5X6L")
//
//                initFace()
                VideoRecordActivity.starter(context)
            }
            MODE_ACC_OFF->{
                FaceSDKManager.instance.registFace()

//                VideoRecordActivity.finish()
//                addSystemListener()

            }
            else -> {
            }
        }
    }


    override fun initFace() {
//        FaceSDKManager.instance.initConfig(null)
//        FaceSDKManager.instance.init(context, "")
    }

    override fun closeCamera() {
        FinishActivityManager.getManager().finishActivity(VideoRecordActivity::class.java)
    }

    override fun openCamera() {
        context.startActivity(Intent(context, VideoRecordActivity::class.java))

    }

    override fun openRecord() {
    }

    override fun acc(status: Int) {
    }

    override fun updateApp() {
    }

    override fun addSystemListener() {
    }

    override fun removeSystemListener() {
    }




}