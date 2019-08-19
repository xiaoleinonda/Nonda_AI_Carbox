package us.nonda.facelibrary.manager

interface IFaceModelListener {

    fun onInitSucceed()
    fun onInitFailed(msg:String)
}