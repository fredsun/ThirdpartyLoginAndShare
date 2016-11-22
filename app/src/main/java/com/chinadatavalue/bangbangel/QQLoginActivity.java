package com.chinadatavalue.bangbangel;

/**
 * Created by Administrator on 2016/11/17.
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.tencent.connect.UserInfo;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Administrator on 2016/11/4.
 */

public class QQLoginActivity extends Activity{
    //初始化Tencent
    public Tencent mTencent = null;
    private ThirdUserInfo thirdUser = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qqlogin);
        mTencent = Tencent.createInstance(ThirdpartyConstants.QQ_APP_ID, this);
    }
    public void qqLogin(View view) {
        mTencent.login(QQLoginActivity.this, "all", mIUiListener);
    }

    public void qqLogout(View view) {
    }


    /**
     * QQ的三方授权回调监听
     */
    IUiListener mIUiListener = new IUiListener() {

        @Override
        public void onCancel() {
            Log.i("qqauth","==cancel");
        }

        @Override
        public void onComplete(Object arg0) {
            //登陆成功的回调，在此处可以获取用户信息
//            AnimationUtil.showLoadingDialog(QQLoginActivity.this, "QQ登陆正在获取用户信息", false);
            initOpenidAndToken((JSONObject) arg0);
            updateUserInfo();
        }

        @Override
        public void onError(UiError arg0) {
            Log.i("qqauth","==error");
        }

    };

    /**
     * QQ初始化OPENID以及TOKEN身份验证。
     */
    private void initOpenidAndToken(JSONObject jsonObject) {
        thirdUser = new ThirdUserInfo();
        try {
            //这里的Constants类，是 com.tencent.connect.common.Constants类，下面的几个参数也是固定的
            String token = jsonObject.getString(com.tencent.connect.common.Constants.PARAM_ACCESS_TOKEN);
            String expires = jsonObject.getString(com.tencent.connect.common.Constants.PARAM_EXPIRES_IN);
            //OPENID,作为唯一身份标识
            String openId = jsonObject.getString(com.tencent.connect.common.Constants.PARAM_OPEN_ID);
            if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(expires) && !TextUtils.isEmpty(openId)) {
                //设置身份的token
                mTencent.setAccessToken(token, expires);
                mTencent.setOpenId(openId);
                thirdUser.setThirdID(openId);
            }
        } catch (Exception e) {
        }
    }

    /**
     * QQ在回调里面可以获取用户信息数据了
     */
    private void updateUserInfo() {
        if (mTencent != null && mTencent.isSessionValid()) {

            IUiListener listener = new IUiListener() {

                @Override
                public void onError(UiError e) {
//                    AnimationUtil.cancelDialog();
                }

                // 用户的信息回调在此处
                @Override
                public void onComplete(final Object response) {
                    // 返回Bitmap对象。
                    try {
                        JSONObject obj = new JSONObject(response.toString());
                        thirdUser.setNickName(obj.optString("nickname"));
                        thirdUser.setHeadimgurl(obj.optString("figureurl_qq_2"));
                        thirdUser.setGender("男".equals(obj.optString("gender")) ? "1" : "0");
                        Log.i("qqNickname", thirdUser.getNickName());
                        Log.i("qqHeadImg", thirdUser.getHeadimgurl());
                        Log.i("qqGender", thirdUser.getGender());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onCancel() {
//                    AnimationUtil.cancelDialog();
                }
            };
            UserInfo mInfo = new com.tencent.connect.UserInfo(QQLoginActivity.this, mTencent.getQQToken());
            mInfo.getUserInfo(listener);
        }
    }

    /**
     * QQ的授权回调
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("TAG", "-->onActivityResult " + requestCode + " resultCode=" + resultCode);
        if (requestCode == com.tencent.connect.common.Constants.REQUEST_LOGIN ||
                requestCode == com.tencent.connect.common.Constants.REQUEST_APPBAR) {
            Tencent.onActivityResultData(requestCode, resultCode, data, mIUiListener);
        }

    }


}
