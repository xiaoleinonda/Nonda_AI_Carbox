package us.nonda.facelibrary.auth

interface IFaceAuthCallback {

    fun onSucceed();

    fun onFailed(msg:String);
}