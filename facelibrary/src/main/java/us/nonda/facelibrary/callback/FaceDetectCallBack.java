/*
 * Copyright (C) 2018 Baidu, Inc. All Rights Reserved.
 */
package us.nonda.facelibrary.callback;


import us.nonda.facelibrary.model.LivenessModel;

/**
 * 人脸检测回调接口。
 *
 * @Time: 2019/1/25
 * @Author: v_chaixiaogang
 */
public interface FaceDetectCallBack {
    void onFaceDetectCallback(boolean isDetect, int faceWidth, int faceHeight,
                              int faceCenterX, int faceCenterY, int imgWidth,
                              int imgHeight);

    void onTip(int code, String msg);


    void onEnmotionCallback(LivenessModel livenessModel);

    void onFaceFeatureCallBack(LivenessModel livenessModel);

}
