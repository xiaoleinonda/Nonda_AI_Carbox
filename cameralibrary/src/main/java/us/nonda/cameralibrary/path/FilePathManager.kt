package us.nonda.cameralibrary.path

import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.DateUtils
import us.nonda.commonibrary.utils.PathUtils

class FilePathManager private constructor() {
    private var path_main = "nonda"
    private var path_picture_folder = "picture"
    private var path_video_folder = "video"

    private var path_picture_front_folder = "emotion/PictureFront"
    private var path_picture_back_folder = "emotion/PictureBack"
    private var path_picture_face_folder = "pass/PictureFace"


    private var path_video_front_folder = "VideoFront"
    private var path_video_back_folder = "VideoBack"

    var sdcard: String = PathUtils.getSDCardPath(AppUtils.context)


    companion object {
        private var instance: FilePathManager? = null

        fun get(): FilePathManager {
            if (instance == null) {
                instance = FilePathManager()
            }

            return instance!!
        }
    }


    fun getFrontEmotionPictureFolderPath(): String {
        val nowDateShort = DateUtils.getStringDateShort()
        return "$sdcard/$path_main/$nowDateShort/image/emotion/front/"
    }

    fun getBackEmotionPictureFolderPath(): String {
        val nowDateShort = DateUtils.getStringDateShort()
        return "$sdcard/$path_main/$nowDateShort/image/emotion/back/"
    }

    fun getFrontVideoPath(): String {
        val nowDateShort = DateUtils.getStringDateShort()
        return "$sdcard/$path_main/$nowDateShort/video/front/"
    }

    fun getBackVideoPath(): String {
        val nowDateShort = DateUtils.getStringDateShort()
        return "$sdcard/$path_main/$nowDateShort/video/back/"
    }

    fun getFacePictureFolderPath(): String {
        val nowDateShort = DateUtils.getStringDateShort()
        return "$sdcard/$path_main/$nowDateShort/image/face/back/"
    }


}