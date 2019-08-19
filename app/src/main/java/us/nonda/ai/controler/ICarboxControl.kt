package us.nonda.ai.controler

import android.content.Context

interface ICarboxControl {

    fun initFace()

    fun openCamera()

    fun closeCamera()

    fun openRecord()

    fun acc(status:Int)

    fun updateApp()

    fun addSystemListener()

    fun removeSystemListener()

    fun mode(mode:Int)


}