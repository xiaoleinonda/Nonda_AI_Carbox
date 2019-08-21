package us.nonda.cameralibrary.path

import us.nonda.commonibrary.utils.AppUtils
import us.nonda.commonibrary.utils.PathUtils

class FilePathManager private constructor() {

    private var path_picture_folder = "picture"
    private var path_video_folder = "video"

    private var path_picture_front_folder = "PictureFront"
    private var path_picture_back_folder = "PictureBack"
    private var path_picture_face_folder = "PictureFace"


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

    init {


    }

    fun getFrontEmotionPictureFolderPath() = "$sdcard/$path_picture_folder/$path_picture_front_folder/"
    fun getBackEmotionPictureFolderPath() = "$sdcard/$path_picture_folder/$path_picture_back_folder/"
    fun getFrontVideoPath() = "$sdcard/$path_video_folder/$path_video_front_folder/"
    fun getBackVideoPath() = "$sdcard/$path_video_folder/$path_video_back_folder/"
    fun getFacePictureFolderPath() = "$sdcard/$path_picture_folder/$path_picture_face_folder/"


}