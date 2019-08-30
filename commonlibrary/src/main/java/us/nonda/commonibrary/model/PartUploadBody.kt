package us.nonda.commonibrary.model

class PartUploadBody(
    var imei: String,
    var uploadId: String,
    var chunks: Int,
    var chunk: Int,
    var chunkMD5: String,
    var fileMD5: String,
    var file: ByteArray
) {
}