package us.nonda.commonibrary.http

class BaseResult<T> {
    var code:Int = -1

    var msg :String =""

    var data:T ?=null
}