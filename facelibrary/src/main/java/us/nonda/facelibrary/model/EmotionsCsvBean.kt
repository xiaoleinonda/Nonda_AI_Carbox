package us.nonda.facelibrary.model

import com.baidu.idl.facesdk.model.FaceInfo

data class EmotionsCsvBean(
    val name: String,
    val age: Float,
    val race: String,
    val glasses: String,
    val gender: String,
    val emotionThree: String,
    val emotionSeven: String,
    val score: String,
    val scores: FloatArray?,
    val faceInfo: FaceInfo?
) {
}