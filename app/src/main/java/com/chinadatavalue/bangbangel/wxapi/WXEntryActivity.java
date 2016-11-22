package com.chinadatavalue.bangbangel.wxapi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.chinadatavalue.bangbangel.NetUtils;
import com.chinadatavalue.bangbangel.ThirdUserInfo;
import com.chinadatavalue.bangbangel.ThirdpartyConstants;
import com.tencent.mm.sdk.modelbase.BaseReq;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.modelmsg.SendAuth;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler {
    private Bundle bundle;
    public IWXAPI mIWxAPI;
    //这个实体类是我自定义的实体类，用来保存第三方的数据的实体类
    private ThirdUserInfo info= null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIWxAPI = WXAPIFactory.createWXAPI(WXEntryActivity.this, ThirdpartyConstants.WX_APP_KEY, true);
        mIWxAPI.handleIntent(getIntent(), WXEntryActivity.this);  //必须调用此句话
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mIWxAPI.handleIntent(intent, WXEntryActivity.this);//必须调用此句话
    }

    @Override
    public void onReq(BaseReq req) {
        System. out.println();
    }



    /**
     * Title: onResp
     *
     *           API：https://open.weixin.qq.com/ cgi- bin/showdocument ?action=dir_list&t=resource/res_list&verify=1&id=open1419317853 &lang=zh_CN
     * Description:在此处得到Code之后调用https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code
     *  获取到token和openID。之后再调用https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID 获取用户个人信息
     *
     * @param arg0
     */
    @Override
    public void onResp(BaseResp arg0) {
        bundle=getIntent().getExtras();
        SendAuth.Resp resp = new SendAuth.Resp( bundle);
        if (null == arg0.transaction ){//登录操作,transaction为null
            switch (resp.errCode){
                case BaseResp.ErrCode.ERR_OK:
                    String code = resp. code;
                        try {
                            getToken(code);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    break;
                case BaseResp.ErrCode.ERR_USER_CANCEL:
                    Toast.makeText(getApplicationContext(),"微信登录取消",Toast.LENGTH_LONG).show();
            }

        }else if (getApplicationContext().getSharedPreferences("config", Context.MODE_PRIVATE).
                getString("WXTransaction","0").equals(arg0.transaction+"")){//分享操作
            switch (resp.errCode){
                case BaseResp.ErrCode.ERR_OK:
                    Toast.makeText(getApplicationContext(),"分享成功",Toast.LENGTH_LONG).show();
                    break;
                case BaseResp.ErrCode.ERR_USER_CANCEL:
                    Toast.makeText(getApplicationContext(),"分享取消",Toast.LENGTH_LONG).show();
                    break;
            }
        }

        WXEntryActivity. this.finish();//必须加关闭微信自己的页面

    }

    //这个方法会取得accesstoken  和openID
    private void getToken(String code) throws IOException {
//        AnimationUtil.showLoadingDialog(this, "微信正在获取用户信息", false);
        String access_token_url = "https://api.weixin.qq.com/sns/oauth2/access_token"
                + "?appid=" + ThirdpartyConstants.WX_APP_KEY
                + "&secret=" + ThirdpartyConstants.WX_APP_SECRET
                + "&code=" + code
                + "&grant_type=authorization_code";
        NetUtils.doGet(access_token_url, new NetUtils.HttpResponseCallBack() {
            @Override
            public void onSuccess(JSONObject response) {
                if (response == null || response.length() == 0) {
                    Log.e("wechatGetToken", "null response");
                    return;
                }

                if(response.optString("access_token") == null || response.optString("access_token").length() == 0) {
                    Log.e("wechatGetToken", "errcode=" + response.optString("errcode") + " errmsg=" + response.optString("errmsg"));
                    return;
                }

                Map<String, String> data = new HashMap<String, String>();
                String[] keys = {"access_token", "expires_in", "refresh_token", "openid", "scope"};
                for(int i=0; i<keys.length; i++) {
                    data.put(keys[i], response.optString(keys[i]));
                }
                getUserInfo(data.get("access_token"),data.get("openid"));
            }

            @Override
            public void onFailure() {
                Log.e("wechatGetToken","error net");
                WXEntryActivity.this.finish();
            }
        });
    }

    //获取到token和openID之后，调用此接口得到身份信息
    private void getUserInfo(String token,String openId) {
        String getUserInfoUrl = "https://api.weixin.qq.com/sns/userinfo"
                + "?access_token=" + token
                + "&openid=" + openId;
        NetUtils.doGet(getUserInfoUrl, new NetUtils.HttpResponseCallBack() {
            @Override
            public void onSuccess(JSONObject response) {
//                AnimationUtil.cancelDialog();
                if (response == null || response.length() == 0) {
                    Log.e("wechatGetToken", "null response");
                    return;
                }

                if(response.optString("openid") == null || response.optString("openid").length() == 0) {
                    Log.e("wechatGetToken", "errcode=" + response.optString("errcode") + " errmsg=" + response.optString("errmsg"));
                    return;
                }

                Map<String, String> data = new HashMap<String, String>();
                String[] keys = {"openid", "nickname", "sex", "province", "city", "country", "headimgurl", "unionid"};
                for(int i=0; i<keys.length; i++) {
                    data.put(keys[i], response.optString(keys[i]));
                }
                Log.i("wechatUserInfo",data.get("nickname"));
                WXEntryActivity.this.finish();
            }

            @Override
            public void onFailure() {
//                AnimationUtil.cancelDialog();
                Log.e("wechatGetToken","error net");
                WXEntryActivity.this.finish();
            }
        });

    }



 }