package us.nonda.facelibrary.manager

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import com.baidu.idl.facesdk.FaceDetect
import com.baidu.idl.facesdk.FaceFeature
import com.baidu.idl.facesdk.model.FaceInfo
import com.baidu.idl.facesdk.model.Feature
import us.nonda.cameralibrary.status.CameraStatus
import us.nonda.commonibrary.MyLog
import us.nonda.facelibrary.db.DBManager
import us.nonda.facelibrary.db.FaceApi
import us.nonda.facelibrary.model.FaceImage
import us.nonda.facelibrary.model.LivenessModel
import us.nonda.facelibrary.status.FaceStatusCache
import us.nonda.facelibrary.utils.DeleteFaceUtil
import us.nonda.facelibrary.utils.FileUtils
import us.nonda.facelibrary.utils.ImageUtils
import us.nonda.mqttlibrary.mqtt.MqttManager
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class FaceRegister constructor(
    private var faceDetect: FaceDetect,
    private var faceFeature: FaceFeature
) {

    private val TAG = "FaceSDKManager"
    private var future: Future<*>? = null
    private val es: ExecutorService = Executors.newSingleThreadExecutor()

    private var userId: String = ""

    private var imageArray: ByteArray? = null
    /**
     * 宽高反转
     */
    private var width = 480
    private var height = 640


    fun registFace(faceImage: FaceImage) {
        if (CameraStatus.instance.getAccStatus() == 0) {
            return
        }
        val imageArray = Base64.decode(faceImage.image, Base64.DEFAULT)
        val userId = faceImage.userId

        //特征提取中...
        mqttPulish("特征提取中...")

        removeFace {
        mqttPulish("start register face")
        MyLog.d(TAG, "start register face")
        this.userId = userId
        val serverBitmap = BitmapFactory.decodeByteArray(imageArray, 0, imageArray.size)
        val matrix = Matrix()
        matrix.postScale(
            width.toFloat() / serverBitmap.width,
            height.toFloat() / serverBitmap.height
        )
        val bitmap = Bitmap.createBitmap(serverBitmap, 0, 0, serverBitmap.width, serverBitmap.height, matrix, false)

        if (bitmap != null) {
            val rgbArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(
                rgbArray, 0, bitmap.width, 0, 0,
                bitmap.width, bitmap.height
            )

            checkFace(rgbArray, bitmap.width, bitmap.height, faceImage.image)


        } else {
            MyLog.d(TAG, "解析图片失败")

            mqttPulish("解析图片失败")
        }

        }

    }

    /**
     * 检测人脸
     */
    private fun checkFace(imageArray: IntArray, width: Int, height: Int, facePic: String) {

        if (future != null && !future!!.isDone) {
            return
        }

        future = es.submit {
            val maxFace = trackMaxFace(imageArray, width, height)

            mqttPulish("注册的人脸maxFace=" + maxFace)
            MyLog.d(TAG, "注册的人脸maxFace=" + maxFace)


            val livenessModel = LivenessModel()
            livenessModel.imageFrame.argb = imageArray
            livenessModel.imageFrame.width = width
            livenessModel.imageFrame.height = height

            if (maxFace != null && maxFace.size > 0) {
                livenessModel.trackFaceInfo = maxFace
                val faceInfo = maxFace[0]
                livenessModel.landmarks = faceInfo.landmarks
                livenessModel.faceInfo = faceInfo
                livenessModel.faceID = faceInfo.face_id


                registFace(livenessModel, facePic)

            } else {
                MyLog.d(TAG, "注册的人脸未提取到人脸")

                mqttPulish("注册的人脸未提取到人脸")

            }
        }

    }

    private fun registFace(livenessModel: LivenessModel, facePicture: String) {
        val visFeature = ByteArray(512)
        val lenght = livenessModel.run {
            extractFeature(
                imageFrame.argb,
                imageFrame.height,
                imageFrame.width,
                visFeature,
                landmarks
            )

        }
        if (lenght == 128f) {
            MqttManager.getInstance().publishEventData(1013, "1")

            val feature = Feature()
            feature.ctime = System.currentTimeMillis()
            feature.feature = visFeature
            feature.userName = userId
            val uid = UUID.randomUUID().toString()
            feature.userId = uid
            feature.groupId = "0"

            // TODO:增加图片
            val imgWidth = livenessModel.imageFrame.width
            val imgHeight = livenessModel.imageFrame.height
            val registBmp = Bitmap.createBitmap(
                imgWidth,
                imgHeight, Bitmap.Config.ARGB_8888
            )
            registBmp.setPixels(livenessModel.imageFrame.argb, 0, imgWidth, 0, 0, imgWidth, imgHeight)
            val logBuilder = StringBuilder()
            logBuilder.append("姓名\t图片名\t成功/失败\t失败原因\n")
            // 保存图片
            // 保存图片到新目录中
            val facePicDir = FileUtils.getFacePicDirectory()
            // 保存抠图图片到新目录中
            val faceCropDir = FileUtils.getFaceCropPicDirectory()
            val picFile = "regist_" + uid + "_rgb.png"

            if (facePicDir != null) {
                val savePicPath = File(facePicDir, picFile)
                if (FileUtils.saveFile(savePicPath, registBmp)) {
                    Log.i("注册", "图片保存成功")
                    feature.imageName = picFile
                }
            }

            var cropBitmap: Bitmap? = null
            var cropImgName: String? = null
            // 人脸抠图
            val landmarks = livenessModel.landmarks
            if (landmarks != null) {
                cropBitmap = ImageUtils.noBlackBoundImgCrop(
                    landmarks,
                    livenessModel.imageFrame.height, livenessModel.imageFrame.width,
                    livenessModel.imageFrame.argb
                )

                if (cropBitmap == null) {
                    cropBitmap = registBmp
                }

                cropImgName = "crop_$picFile"
            }
            if (faceCropDir != null && cropBitmap != null) {
                val saveCropPath = File(faceCropDir, cropImgName!!)
                if (FileUtils.saveFile(saveCropPath, cropBitmap)) {
                    Log.i("注册", "抠图图片保存成功")
                    feature.cropImageName = cropImgName
                }
            }


            logBuilder.append(userId + "\t" + picFile + "\t" + "成功\n")

            /**
             * 向数据库中插入人脸特征
             */
            if (FaceApi.getInstance().featureAdd(feature)) {
                if (CameraStatus.instance.getAccStatus() == 0) {
                    return
                }

                FaceSDKManager.instance.setFeature()
                FaceSDKManager.instance.isRegisted = true
                FaceStatusCache.instance.facePicture = facePicture
                mqttPulish("注册成功")
                MyLog.d(TAG, "注册成功")

            } else {
                mqttPulish("注册特征提取失败")
                MyLog.d(TAG, "注册特征提取失败")

            }
            userId = ""
        } else {
            MqttManager.getInstance().publishEventData(1013, "2")

        }

    }

    /**
     * 人脸特征提取
     *
     * @param argb
     * @param landmarks
     * @param height
     * @param width
     * @param feature
     * @return
     */

    private fun extractFeature(
        argb: IntArray,
        height: Int,
        width: Int,
        feature: ByteArray,
        landmarks: IntArray
    ): Float {
        return faceFeature.feature(FaceFeature.FeatureType.FEATURE_VIS, argb, height, width, landmarks, feature)
    }


    private fun trackMaxFace(rgbArray: IntArray, width: Int, height: Int): Array<FaceInfo>? {
        val minFaceSize = FaceSDKManager.instance.getMinFaceSize()
        if (width < minFaceSize || height < minFaceSize) {
            return null
        }

        return faceDetect.trackMaxFace(rgbArray, height, width)
    }

    private fun removeFace(deleteSuccessCallback: () -> Unit) {

        val listFeatures = DBManager.getInstance().queryFeature()

        if (listFeatures!=null &&listFeatures.size > 0) {
            DeleteFaceUtil.deleteFace(deleteSuccessCallback)
        } else {
            deleteSuccessCallback.invoke()
        }
    }

    private fun mqttPulish(msg: String) {
        Log.d("注册", msg)
    }
}