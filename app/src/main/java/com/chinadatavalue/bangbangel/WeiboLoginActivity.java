package com.chinadatavalue.bangbangel;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;

import static com.sina.weibo.sdk.statistic.WBAgent.TAG;

/**
 * Created by Administrator on 2016/10/27.
 */

public class WeiboLoginActivity extends Activity {

    private AuthInfo mWeiboAuthInfo;
    private Button btnweibo;
    private Button btnlogout;
    private TextView tv;
    private String nickname="";

    /** 封装了 "access_token"，"expires_in"，"refresh_token"，并提供了他们的管理功能 */
    private Oauth2AccessToken mWeiboAccessToken;

    /** 注意：SsoHandler 仅当 SDK 支持 SSO 时有效 */
    private SsoHandler mWeiboSsoHandler;

    /** 用户信息接口 */
    private UsersAPI mWeiboUserAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weibologin);

        initweibologin();
        initViews();
        initData();
    }

    private void initData() {
        // 从 SharedPreferences 中读取上次已保存好 AccessToken 等信息，
        // 第一次启动本应用，AccessToken 不可用
        mWeiboAccessToken = WeiboAccessTokenKeeper.readAccessToken(this);
        if (mWeiboAccessToken.isSessionValid()) {
            updateWeiboTokenView(true);
        }
    }

    private void initViews() {

        btnweibo = (Button) findViewById(R.id.btn_weibo_login);
        btnlogout = (Button) findViewById(R.id.btnlogout);
        tv = (TextView) findViewById(R.id.content);
        // 获取 Token View，并让提示 View 的内容可滚动（小屏幕可能显示不全）
        tv.setMovementMethod(new ScrollingMovementMethod());
    }



    /**
     * 进行微博授权初始化操作
     */
    private void initweibologin() {
        // 初始化授权类对象，将应用的信息保存
        mWeiboAuthInfo = new AuthInfo(this, ThirdpartyConstants.WEIBO_APP_KEY,
                ThirdpartyConstants.WEIBO_REDIRECT_URL, ThirdpartyConstants.WEIBO_SCOPE);
        mWeiboSsoHandler = new SsoHandler(WeiboLoginActivity.this, mWeiboAuthInfo);
    }

    /**
     * 当 SSO 授权 Activity 退出时，该函数被调用。
     *
     * @see {@link Activity#onActivityResult}
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // SSO 授权回调
        // 重要：发起 SSO 登陆的 Activity 必须重写 onActivityResults
        if (mWeiboSsoHandler != null) {
            mWeiboSsoHandler.authorizeCallBack(requestCode, resultCode, data);
        }

    }

    public void weiboTestLogin(View v){
        // 如果手机安装了微博客户端则使用客户端授权,没有则进行网页授权
        mWeiboSsoHandler.authorize(new AuthListener());
    }

    public void weiboLogout(View v){
        new LogoutAPI(WeiboLoginActivity.this, ThirdpartyConstants.WEIBO_APP_KEY,
                WeiboAccessTokenKeeper.readAccessToken(WeiboLoginActivity.this)).logout(new WeiboLogOutRequestListener());
    }

    /**
     * 微博认证授权回调类。 1. SSO 授权时，需要在 {@link #onActivityResult} 中调用
     * {@link SsoHandler#authorizeCallBack} 后， 该回调才会被执行。 2. 非 SSO
     * 授权时，当授权结束后，该回调就会被执行。 当授权成功后，请保存该 access_token、expires_in、uid 等信息到
     * SharedPreferences 中。
     */
    class AuthListener implements WeiboAuthListener {

        @Override
        public void onCancel() {
            Toast.makeText(WeiboLoginActivity.this, "取消授权", Toast.LENGTH_LONG)
                    .show();
        }

        @Override
        public void onComplete(Bundle values) {
            // 从 Bundle 中解析 Token
            mWeiboAccessToken = Oauth2AccessToken.parseAccessToken(values);
            if (mWeiboAccessToken.isSessionValid()) {
                nickname = "用户名："
                        + String.valueOf(values
                        .get("com.sina.weibo.intent.extra.NICK_NAME"));
                // 显示 Token
                getWeiBoUserInfo();
                updateWeiboTokenView(false);

                // 保存 Token 到 SharedPreferences
                WeiboAccessTokenKeeper.writeAccessToken(WeiboLoginActivity.this,
                        mWeiboAccessToken);
                Toast.makeText(WeiboLoginActivity.this, "授权成功", Toast.LENGTH_SHORT)
                        .show();
                // Toast.makeText(
                // WeiboLoginActivity.this,
                // "头像地址:"
                // + String.valueOf(values
                // .get("com.sina.weibo.intent.extra.USER_ICON")),
                // Toast.LENGTH_LONG).show();

                Toast.makeText(WeiboLoginActivity.this, nickname, Toast.LENGTH_LONG)
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
                Toast.makeText(WeiboLoginActivity.this, message, Toast.LENGTH_LONG)
                        .show();
            }
        }

        private void getWeiBoUserInfo() {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    //获取用户信息的API
                    mWeiboUserAPI = new UsersAPI(WeiboLoginActivity.this, ThirdpartyConstants.WEIBO_APP_KEY, mWeiboAccessToken);
                    long uid = Long.parseLong(mWeiboAccessToken.getUid());
                    mWeiboUserAPI.show(uid, mListener);

                }
            }).start();
        }

        @Override
        public void onWeiboException(WeiboException e) {
            Toast.makeText(WeiboLoginActivity.this,
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
        tv.setText(String.format(format, mWeiboAccessToken.getToken(), date));

        String message = String.format(format, mWeiboAccessToken.getToken(), date);
        if (hasExisted) {
            message = getString(R.string.weibosdk_demo_token_has_existed)
                    + "\n" + message;

        }
        message += "\n" + nickname;
        tv.setText(message);
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
                    Toast.makeText(WeiboLoginActivity.this,
                            "获取User信息成功，用户昵称：" + user.screen_name
                                    + "用户头像：" + user.avatar_hd
                                    + "用户性别" + user.gender,
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(WeiboLoginActivity.this, response, Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        public void onWeiboException(WeiboException e) {
            LogUtil.e(TAG, e.getMessage());
            ErrorInfo info = ErrorInfo.parse(e.getMessage());
            Toast.makeText(WeiboLoginActivity.this, info.toString(), Toast.LENGTH_LONG).show();
        }
    };

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
                        WeiboAccessTokenKeeper.clear(WeiboLoginActivity.this);
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

}