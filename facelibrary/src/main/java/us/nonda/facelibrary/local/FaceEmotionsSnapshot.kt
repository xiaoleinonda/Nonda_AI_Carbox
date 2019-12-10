package us.nonda.facelibrary.local

import android.util.Log
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import us.nonda.cameralibrary.path.FilePathManager
import us.nonda.facelibrary.model.EmotionsCsvBean
import us.nonda.commonibrary.utils.FileUtils
import us.nonda.facelibrary.model.FaceAttributeModel
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class FaceEmotionsSnapshot private constructor() {

    private var fileSubject = BehaviorSubject.create<FaceAttributeModel>()

    private var filePath: String = FilePathManager.get().getEmotionsCsvPath()

    private var disposable: Disposable? = null

    companion object {
        val instance: FaceEmotionsSnapshot by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            FaceEmotionsSnapshot()
        }
    }

    private var csvPrinter: CSVPrinter? = null

    fun initFile() {
        try {
            val emotionsCsvPath = FilePathManager.get().getEmotionsCsvPath()
            val formatter = SimpleDateFormat("yyyyMMdd_HHmmss_SSS")
            val currentTime = Date()
            val dateString = formatter.format(currentTime)
            filePath = emotionsCsvPath + dateString + "/"

            val dirFile = File(filePath)
            if (!dirFile.exists()) {
                dirFile.mkdirs()
            }

//            if (csvPrinter == null) {
                csvPrinter = initCsvPrinter(filePath, "csv$dateString.csv")
//            }

        } catch (e: Exception) {
            Log.d("csv日志", "异常：" + e.localizedMessage)
        }

        subscribe()
    }


    fun subscribe() {
        disposable = fileSubject.observeOn(Schedulers.newThread())
            ?.map {
                val attributes = it.attributes
                val formatter = SimpleDateFormat("yyyyMMdd_HHmmss_SSS")
                val currentTime = Date()
                val dateString = formatter.format(currentTime)
                val fileName = "${dateString}_${it.emotions}_${attributes.emotion}"

                FileUtils.saveBitmapToSDCard(
                    it.imageFrame.argb,
                    it.imageFrame.width,
                    it.imageFrame.height,
                    filePath,
                    fileName
                )
                Log.d("csv日志", "图片保存成功")

                EmotionsCsvBean(
                    fileName,
                    attributes.age,
                    attributes.race.toString(),
                    attributes.glasses.toString(),
                    attributes.gender.toString(),
                    attributes.emotion.toString(),
                    it.emotions ?: "",
                    it.score,
                    it.scores,
                    it.faceInfo
                )

            }
            ?.onErrorReturn {
                EmotionsCsvBean(
                    "",
                    18f,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    null,
                    null
                )

            }
            ?.doOnNext {
                printCsv(csvPrinter, it)
            }
            ?.subscribe(
                {
                    Log.d("csv日志", "结束一条")
                },
                {})

    }


    fun savePicture(model: FaceAttributeModel) {
        fileSubject.onNext(model)
    }

    fun stop() {
        Log.d("csv日志", "FaceEmotionsSnapshot  stop")

        disposable?.dispose()

        csvPrinter?.close()
    }

    private fun initCsvPrinter(filePath: String, fileName: String): CSVPrinter {
        val file = File(filePath, fileName)// 在SDcard的目录下创建图片文,以当前时间为其命名
        val writer = BufferedWriter(OutputStreamWriter(FileOutputStream(file), "UTF-8"))  // 防止出现乱码

        // 添加头部
        return CSVPrinter(
            writer,
            CSVFormat.DEFAULT.withHeader(
                "PictureName",
                "Age",
                "Race",
                "Glasses",
                "Gender",
                "EmotionThree",
                "EmotionSeven",
                "Score",
                "Score_Angry",
                "Score_Disgust",
                "Score_Fear",
                "Score_Happy",
                "Score_Sad",
                "Score_Surprise",
                "Score_Neutral",
                "illum",
                "bluriness",
                "yaw",
                "roll",
                "pitch",
                "occlu",
                "headPose",
                "_leftEyeState",
                "_rightEyeState",
                "_mouthState",
                "is_live",
                "is_live_head_up",
                "is_live_head_down",
                "is_live_head_turn_left",
                "is_live_head_turn_right"
            )
        )
    }

    private fun printCsv(printer: CSVPrinter?, model: EmotionsCsvBean) {
        if (printer == null) {
            return
        }
        Log.d("csv日志", "开始写csv")

        model.run {
            if (scores == null) {
                return
            }
            try {
                val fl0 = scores[0]
                val fl1 = scores[1]
                val fl2 = scores[2]
                val fl3 = scores[3]
                val fl4 = scores[4]
                val fl5 = scores[5]
                val fl6 = scores[6]
                val occlu = faceInfo?.occlu

                val headPose = faceInfo?.headPose

                var yaw: Float = -1f
                var pitch: Float = -1f
                var roll: Float = -1f
                if (headPose != null && headPose.isNotEmpty()) {

                    /* yaw = Math.abs(headPose[0])
                     pitch = Math.abs(headPose[1])
                     roll = Math.abs(headPose[2])*/

                    yaw = headPose[0]
                    pitch = headPose[1]
                    roll = headPose[2]
                }
                Log.d("csv日志", "yaw=$yaw pitch=$pitch roll=$roll")

                val fl00 = faceInfo?.headPose!![0]
                val fl11 = faceInfo?.headPose!![1]
                val fl22 = faceInfo?.headPose!![2]

                printer.printRecord(
                    name,
                    age,
                    race,
                    glasses,
                    gender,
                    emotionThree,
                    emotionSeven,
                    score,
                    fl0,
                    fl1,
                    fl2,
                    fl3,
                    fl4,
                    fl5,
                    fl6,
                    faceInfo?.illum,
                    faceInfo?.blur,
                    yaw,
                    roll,
                    pitch,
                    occlu?.size,
                    "$fl00-$fl11-$fl22",
                    faceInfo?._leftEyeState,
                    faceInfo._rightEyeState,
                    faceInfo?._mouthState,
                    faceInfo?.is_live(),
                    faceInfo?.is_live_head_up,
                    faceInfo?.is_live_head_down,
                    faceInfo?.is_live_head_turn_left,
                    faceInfo?.is_live_head_turn_right
                )
                printer.flush()
            } catch (e: java.lang.Exception) {
                Log.d("csv日志", " 写csv异常="+e.localizedMessage)

            }

        }

    }
}