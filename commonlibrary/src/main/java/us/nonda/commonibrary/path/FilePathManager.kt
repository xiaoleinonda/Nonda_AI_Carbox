package us.nonda.commonibrary.path

import android.content.Context
import us.nonda.commonibrary.utils.PathUtils

class FilePathManager private constructor(private val context: Context) {

    private var path_picture_front = "PictureFront"
    private var path_picture_back = "PictureBack"
    private var path_video_front = "VideoFront"
    private var path_video_back = "VideoBack"
    private var path_picture_face = "PictureFace"

    var sdcard: String = PathUtils.getSDCardPath(context)

    companion object {
        private var instance: FilePathManager? = null

        fun get(context: Context): FilePathManager {
            if (instance == null) {
                instance = FilePathManager(context)
            }

            return instance!!
        }
    }

    fun getFrontPicturePath() = "$sdcard/$path_picture_front/"
    fun getBackPicturePath() = "$sdcard/$path_picture_back/"
    fun getFrontVideoPath() = "$sdcard/$path_video_front/"
    fun getBackVideoPath() = "$sdcard/$path_video_back/"
    fun getFacePicturePath() = "$sdcard/$path_picture_face/"
}