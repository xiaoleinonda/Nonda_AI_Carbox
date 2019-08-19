package us.nonda.facelibrary.model;

/**
 * @Time: 2019/5/24
 * @Author: v_zhangxiaoqing01

 */

public class GlobalSet {

    // 模型在asset 下path 为空
    public static final String PATH = "";
    // 模型在SD 卡下写对应的绝对路径
    // public static final String PATH = "/storage/emulated/0/baidu_face/model/";

    public static final int FEATURE_SIZE = 512;

    public static final String TIME_TAG = "face_time";

    // 遮罩比例
    public static final float SURFACE_RATIO = 0.6f;

    public static final String ACTIVATE_KEY = "faceexample-face-android-1";
    public static final String LICENSE_FILE_NAME = "idl-license.face-android";

    public static final String DETECT_VIS_MODEL = PATH
            + "detect-rgb-015_2.lite_anakin.bin";
    public static final String DETECT_NIR_MODE = PATH
            + "detect_nir_2.0.2.model";
    public static final String ALIGN_MODEL = PATH
            + "align_2.0.2.anakin.bin";
    public static final String LIVE_VIS_MODEL = PATH
            + "liveness_rgb_anakin_2.0.2.bin";
    public static final String LIVE_NIR_MODEL = PATH
            + "liveness_nir_anakin_2.0.2.bin";
    public static final String LIVE_DEPTH_MODEL = PATH
            + "liveness_depth_anakin_2.0.2.bin";
    public static final String RECOGNIZE_VIS_MODEL = PATH
            + "recognize_rgb_live_pytorch_anakin_2.0.2.bin";
    public static final String RECOGNIZE_IDPHOTO_MODEL = PATH
            + "";
    public static final String OCCLUSION_MODEL = PATH
            + "occlusion/occlusion-unet-ca-anakin.model.float32-1.1.0.2";
    public static final String BLUR_MODEL = PATH
            + "blur/blur-vgg-ca-anakin.model.float32-3.0.1.1";

    public static final String ATTRIBUTE_ATTTIBUTE_MODEL = "attribute_anakin_2.0.2.bin";
    public static final String ATTRIBUTE_EMOTION_MODEL = "emotion_anakin_2.0.2.bin";

    // 图片尺寸限制大小
    public static final int PICTURE_SIZE = 1000000;

    // 摄像头类型
    public static final String TYPE_CAMERA = "TYPE_CAMERA";
    public static final int ORBBEC = 1;
    public static final int IMIMECT = 2;
    public static final int ORBBECPRO = 3;
    public static final int ORBBECPROS1 = 4;
    public static final int ORBBECPRODABAI = 5;
    public static final int ORBBECPRODEEYEA = 6;
    public static final int ORBBECATLAS = 7;

}
