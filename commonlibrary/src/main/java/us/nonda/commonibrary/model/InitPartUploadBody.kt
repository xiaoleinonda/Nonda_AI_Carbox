package us.nonda.commonibrary.model

class InitPartUploadBody constructor(
    var imei: String,
    var fileMD5: String,
    var fileName: String,
    var fileType: Int,
    var fileTime: Long,
    var chunks: Int
) {
}