package us.nonda.commonibrary.model

class CompletePartUploadBody(var imei: String, var uploadId: String, var chunks: Int, var fileMD5: String) {
}