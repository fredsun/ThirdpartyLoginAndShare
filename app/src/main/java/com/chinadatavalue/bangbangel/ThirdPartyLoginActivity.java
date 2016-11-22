package com.chinadatavalue.bangbangel;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.net.RequestListener;
import com.sina.weibo.sdk.openapi.LogoutAPI;
import com.sina.weibo.sdk.openapi.UsersAPI;
import com.sina.weibo.sdk.openapi.models.ErrorInfo;
import com.sina.weibo.sdk.openapi.models.User;
import com.sina.weibo.sdk.utils.LogUtil;
import com.tencent.connect.UserInfo;
import com.tencent.mm.sdk.modelmsg.SendAuth;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;

import static com.sina.weibo.sdk.statistic.WBAgent.TAG;


/**
 * Created by Administrator on 2016/11/15.
 */

public class ThirdPartyLoginActivity extends Activity {
    /**
     * 微博
     * 1,SSO授权拿到token
     * 2.token通过openAPI拿到用户信息
     * PS:  微博默认已授权的账号,在第二次登录时不在登录授权的确定页面停留,
     *      故如果需要退出登录,请调用LogoutAPI,这样第二次登录是重新授权,会停留
     *      如此麻烦的一步都为了"能停留在切换账号页面"的需求
     */
    private AuthInfo mWeiboAuthInfo;
    //封装了 "access_token"，"expires_in"，"refresh_token"，并提供了他们的管理功能
    private Oauth2AccessToken mWeiboAccessToken;
    //注意：SsoHandler 仅当 SDK 支持 SSO 时有效
    private SsoHandler mWeiboSsoHandler;
    //用户信息接口
    private UsersAPI mWeiboUserAPI;

    //初始化QQ登录
    public Tencent mTencent = null;
    private ThirdUserInfo thirdUser = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thirdparty_login);
    }

    private void initWeibo() {
        // 初始化授权类对象，将应用的信息保存
        mWeiboAuthInfo = new AuthInfo(this, ThirdpartyConstants.WEIBO_APP_KEY,
                ThirdpartyConstants.WEIBO_REDIRECT_URL, ThirdpartyConstants.WEIBO_SCOPE);
        mWeiboSsoHandler = new SsoHandler(ThirdPartyLoginActivity.this, mWeiboAuthInfo);

        // 从 SharedPreferences 中读取上次已保存好 AccessToken 等信息，
        // 第一次启动本应用，AccessToken 不可用
        mWeiboAccessToken = WeiboAccessTokenKeeper.readAccessToken(this);
        if (mWeiboAccessToken.isSessionValid()) {
            updateWeiboTokenView(true);
        }
    }

    public void weiboLogin(View view) {
        initWeibo();
        // 如果手机安装了微博客户端则使用客户端授权,没有则进行网页授权
        mWeiboSsoHandler.authorize(new AuthListener());
    }

    public void weiboLogout(View view) {
        new LogoutAPI(ThirdPartyLoginActivity.this, ThirdpartyConstants.WEIBO_APP_KEY,
                WeiboAccessTokenKeeper.readAccessToken(ThirdPartyLoginActivity.this)).logout(new WeiboLogOutRequestListener());
    }

    /**
     *
     *
     * 微博认证授权回调类。
     * 1. SSO 授权时，需要在 {@link #onActivityResult} 中调用
     * {@link SsoHandler#authorizeCallBack} 后， 该回调才会被执行。
     * 2. 非 SSO授权时，当授权结束后，该回调就会被执行。
     * 当授权成功后，请保存该 access_token、expires_in、uid 等信息到
     * SharedPreferences 中。
     */
    class AuthListener implements WeiboAuthListener {

        @Override
        public void onCancel() {
            Toast.makeText(ThirdPartyLoginActivity.this, "取消授权", Toast.LENGTH_LONG)
                    .show();
        }

        @Override
        public void onComplete(Bundle values) {
            // 从 Bundle 中解析 Token
            mWeiboAccessToken = Oauth2AccessToken.parseAccessToken(values);
            if (mWeiboAccessToken.isSessionValid()) {
                String nickname = "用户名："
                        + String.valueOf(values
                        .get("com.sina.weibo.intent.extra.NICK_NAME"));
                // 显示 Token
                getWEIBOUserInfo();
                updateWeiboTokenView(false);

                // 保存 Token 到 SharedPreferences
                WeiboAccessTokenKeeper.writeAccessToken(ThirdPartyLoginActivity.this,
                        mWeiboAccessToken);
                Toast.makeText(ThirdPartyLoginActivity.this, "授权成功", Toast.LENGTH_SHORT)
                        .show();
                Toast.makeText(ThirdPartyLoginActivity.this, nickname, Toast.LENGTH_LONG)
                        .show();

            } else {
                // 以下几种情况，您会收到 Code：
                // 1. 当您未在平台上注册的应用程序的包名与签名时；
                // 2. 当您注册的应用程序包名与签名不正确时；
                // 3. 当您在平台上注册的包名和签名与您当前测试的应用的包名和签名不匹配时。
                String code = values.getString("code");
                String message = "授权失败";
                if (!TextUtils.isEmpty(code)) {
                    message = message + "\nObtained the code: " + code;
                }
                Toast.makeText(ThirdPartyLoginActivity.this, message, Toast.LENGTH_LONG)
                        .show();
            }
        }

        private void getWEIBOUserInfo() {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    //获取用户信息的API
                    mWeiboUserAPI = new UsersAPI(ThirdPartyLoginActivity.this, ThirdpartyConstants.WEIBO_APP_KEY, mWeiboAccessToken);
                    long uid = Long.parseLong(mWeiboAccessToken.getUid());
                    mWeiboUserAPI.show(uid, mListener);

                }
            }).start();
        }

        @Override
        public void onWeiboException(WeiboException e) {
            Toast.makeText(ThirdPartyLoginActivity.this,
                    "Auth exception : " + e.getMessage(), Toast.LENGTH_LONG)
                    .show();
        }

    }

    /**
     * 显示当前 Token 信息。
     *
     * @param hasExisted
     *            配置文件中是否已存在 token 信息并且合法
     */
    private void updateWeiboTokenView(boolean hasExisted) {
        String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                .format(new java.util.Date(mWeiboAccessToken.getExpiresTime()));
        String format = getString(R.string.weibosdk_demo_token_to_string_format_1);
        Log.i("OldWeiboToken",String.format(format, mWeiboAccessToken.getToken(), date));

        String message = String.format(format, mWeiboAccessToken.getToken(), date);
        if (hasExisted) {
            message = getString(R.string.weibosdk_demo_token_has_existed)
                    + "\n" + message;
        }
        Log.i("OldWeiboToken",message);
    }

    /**
     * 微博 OpenAPI 回调接口。
     */
    private RequestListener mListener = new RequestListener() {
        @Override
        public void onComplete(String response) {
            if (!TextUtils.isEmpty(response)) {
                LogUtil.i(TAG, response);
                // 调用 User#parse 将JSON串解析成User对象
                User user = User.parse(response);
                if (user != null) {
                    Toast.makeText(ThirdPartyLoginActivity.this,
                            "获取User信息成功，用户昵称：" + user.screen_name
                                    + "用户头像：" + user.avatar_hd
                                    + "用户性别" + user.gender,
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(ThirdPartyLoginActivity.this, response, Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        public void onWeiboException(WeiboException e) {
            LogUtil.e(TAG, e.getMessage());
            ErrorInfo info = ErrorInfo.parse(e.getMessage());
            Toast.makeText(ThirdPartyLoginActivity.this, info.toString(), Toast.LENGTH_LONG).show();
        }
    };

    /**
     * 当 SSO 授权 Activity 退出时，该函数被调用。
     *
     * @see {@link Activity#onActivityResult}
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("TAG", "-->onActivityResult " + requestCode + " resultCode=" + resultCode);
        // SSO 授权回调
        // 重要：发起 SSO 登陆的 Activity 必须重写 onActivityResults
        if (mWeiboSsoHandler != null) {
            mWeiboSsoHandler.authorizeCallBack(requestCode, resultCode, data);
        }

        if (requestCode == com.tencent.connect.common.Constants.REQUEST_LOGIN ||
                requestCode == com.tencent.connect.common.Constants.REQUEST_APPBAR) {
            Tencent.onActivityResultData(requestCode, resultCode, data, mIUiListener);
        }
    }

    /**
     * 登出按钮的监听器，接收登出处理结果。（API 请求结果的监听器）
     * 看需求,微博自带,登录过的账号直接绕过确定,无法登录时传参设置
     */
    private class WeiboLogOutRequestListener implements RequestListener {
        @Override
        public void onComplete(String response) {
            if (!TextUtils.isEmpty(response)) {
                try {
                    JSONObject obj = new JSONObject(response);
                    String value = obj.getString("result");

                    if ("true".equalsIgnoreCase(value)) {
                        WeiboAccessTokenKeeper.clear(ThirdPartyLoginActivity.this);
                        Log.i("weibologout","weibo登出成功");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onWeiboException(WeiboException e) {
            Log.i("weibologout","weibo登出异常");
        }
    }

    public void wechatLogin(View view) {
        IWXAPI mIWxAPI;
        mIWxAPI = WXAPIFactory.createWXAPI(ThirdPartyLoginActivity.this, ThirdpartyConstants.WX_APP_KEY, true);
        mIWxAPI.registerApp(ThirdpartyConstants.WX_APP_KEY);
        SendAuth.Req req = new SendAuth.Req();
        req.scope = "snsapi_userinfo";
        req.state = "wechat_sdk_demo_test";
        mIWxAPI.sendReq(req);//执行完毕这句话之后，会在WXEntryActivity回调
    }

    public void QQLogin(View view) {
        mTencent = Tencent.createInstance(ThirdpartyConstants.QQ_APP_ID, this);
        mTencent.login(ThirdPartyLoginActivity.this, "all", mIUiListener);
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
            UserInfo mInfo = new com.tencent.connect.UserInfo(ThirdPartyLoginActivity.this, mTencent.getQQToken());
            mInfo.getUserInfo(listener);
        }
    }


}
