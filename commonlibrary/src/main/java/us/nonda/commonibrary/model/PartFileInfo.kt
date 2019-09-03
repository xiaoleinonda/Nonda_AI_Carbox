package us.nonda.commonibrary.model

class PartFileInfo constructor(
    var imei: String,
    var uploadId: String,
    var chunks: Int,
    var fileMD5: String,
    var filePath: String
) {
}