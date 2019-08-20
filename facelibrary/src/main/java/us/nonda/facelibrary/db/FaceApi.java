package us.nonda.facelibrary.db;

import android.text.TextUtils;
import com.baidu.idl.facesdk.FaceFeature;
import com.baidu.idl.facesdk.model.BDFaceSDKAttribute;
import com.baidu.idl.facesdk.model.BDFaceSDKEmotions;
import com.baidu.idl.facesdk.model.FaceInfo;
import com.baidu.idl.facesdk.model.Feature;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库、SDK相关封装操作
 * Created by v_liujialu01 on 2018/11/28.
 */

public class FaceApi {
    private static FaceApi instance;

    public static synchronized FaceApi getInstance() {
        if (instance == null) {
            instance = new FaceApi();
        }
        return instance;
    }

    /**
     * 添加特征信息
     *
     * @param feature
     * @return
     */
    public boolean featureAdd(Feature feature) {
        if (feature == null || TextUtils.isEmpty(feature.getGroupId())) {
            return false;
        }
        Pattern pattern = Pattern.compile("^[0-9a-zA-Z_-]{1,}$");
        Matcher matcher = pattern.matcher(feature.getUserId());
        if (!matcher.matches()) {
            return false;
        }
        return DBManager.getInstance().addFeature(feature);
    }

    public List<Feature> featureQuery() {
        return DBManager.getInstance().queryFeature();
    }

    /**
     * 删除特征信息
     *
     * @param feature
     * @return
     */
    public boolean featureDelete(Feature feature) {
        if (feature == null) {
            return false;
        }
        return DBManager.getInstance().deleteFeature(feature.getUserId(), feature.getGroupId(),
                feature.getFaceToken());
    }



    /**
     * 是否是有效姓名
     *
     * @param username
     * @return
     */
    public String isValidName(String username) {
        if (username == null || "".equals(username.trim())) {
            return "姓名为空";
        }

        // 姓名过长
        if (username.length() > 10) {
            return "姓名过长";
        }

        // 含有特殊符号
        String regex = "[ _`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）—"
                + "—+|{}【】‘；：”“’。，、？]|\n|\r|\t";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(username);
        if (m.find()) {
            return "姓名中含有特殊符号";
        }

        return "0";
    }

}
