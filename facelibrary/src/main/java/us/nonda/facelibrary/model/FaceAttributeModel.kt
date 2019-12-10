package us.nonda.facelibrary.model

import com.baidu.idl.facesdk.FaceAttributes
import com.baidu.idl.facesdk.model.BDFaceSDKAttribute
import com.baidu.idl.facesdk.model.BDFaceSDKEmotions
import com.baidu.idl.facesdk.model.FaceInfo

data class FaceAttributeModel(
    var faceInfo: FaceInfo,
    var emotions: String?,
    var attributes: BDFaceSDKAttribute,
    var imageFrame: ImageFrame,
    var score: String,
    val scores: FloatArray
) {
}