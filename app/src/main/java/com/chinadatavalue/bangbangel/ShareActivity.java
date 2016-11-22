package com.chinadatavalue.bangbangel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.sina.weibo.sdk.api.WebpageObject;
import com.sina.weibo.sdk.api.WeiboMessage;
import com.sina.weibo.sdk.api.share.BaseResponse;
import com.sina.weibo.sdk.api.share.IWeiboHandler;
import com.sina.weibo.sdk.api.share.IWeiboShareAPI;
import com.sina.weibo.sdk.api.share.SendMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.WeiboShareSDK;
import com.sina.weibo.sdk.constant.WBConstants;
import com.sina.weibo.sdk.utils.Utility;
import com.tencent.connect.share.QQShare;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

/**
 * Created by Fred on 2016/8/18.
 */
public class ShareActivity extends Activity implements View.OnClickListener, IWeiboHandler.Response{
    //初始化Tencent
    public Tencent mTencent = null;
    //微博share
    private IWeiboShareAPI mWeiboShareAPI;
    //初始化微信
    private IWXAPI mIWxAPI;
    //自定义分享内容实体
    ShareContentEntity mShareContentEntity = new ShareContentEntity();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        //重算宽度,横向全屏
        WindowManager windowManager = getWindowManager();
        Display d = windowManager.getDefaultDisplay();
        WindowManager.LayoutParams p = getWindow().getAttributes();
        p.width = (int) (d.getWidth() * 1);
        getWindow().setAttributes(p);


        if (null != getIntent().getStringExtra("flag")){
            Intent intent = getIntent();
            mShareContentEntity.setTitle(intent.getStringExtra("shareTitle"));
            mShareContentEntity.setSummary(intent.getStringExtra("shareSummary"));
            mShareContentEntity.setImgUrl(intent.getStringExtra("shareImgUrl"));
            mShareContentEntity.setTargetUrl(intent.getStringExtra("shareTargetUrl"));
            mShareContentEntity.setAppName(intent.getStringExtra("shareAppName"));
        }else {
            mShareContentEntity.setTitle("临时标题");
            mShareContentEntity.setSummary("临时摘要");
            mShareContentEntity.setImgUrl("https://raw.githubusercontent.com/sunxlfree/RES/4e02bcbe474bd40933d405a1563095b113090d63/logo.png");
            mShareContentEntity.setTargetUrl("https://www.baidu.com");
            mShareContentEntity.setAppName("帮帮天使");
        }

                //微信初始化
        mIWxAPI = WXAPIFactory.createWXAPI(this, ThirdpartyConstants.WX_APP_KEY, true);
        mIWxAPI.registerApp(ThirdpartyConstants.WX_APP_KEY);

        //微博初始化
        // 创建微博分享接口实例
        mWeiboShareAPI = WeiboShareSDK.createWeiboAPI(this, ThirdpartyConstants.WEIBO_APP_KEY);

        // 注册第三方应用到微博客户端中，注册成功后该应用将显示在微博的应用列表中。
        // 但该附件栏集成分享权限需要合作申请，详情请查看 Demo 提示
        // NOTE：请务必提前注册，即界面初始化的时候或是应用程序初始化时，进行注册
        mWeiboShareAPI.registerApp();

        // 当 Activity 被重新初始化时（该 Activity 处于后台时，可能会由于内存不足被杀掉了），
        // 需要调用 {@link IWeiboShareAPI#handleWeiboResponse} 来接收微博客户端返回的数据。
        // 执行成功，返回 true，并调用 {@link IWeiboHandler.Response#onResponse}；
        // 失败返回 false，不调用上述回调
        if (savedInstanceState != null) {
            mWeiboShareAPI.handleWeiboResponse(getIntent(), this);
        }

        TextView tv_share_qq = (TextView) findViewById(R.id.tv_share_qq);
        tv_share_qq.setOnClickListener(this);

        TextView tv_share_wechat = (TextView) findViewById(R.id.tv_share_wechat);
        tv_share_wechat.setOnClickListener(this);

        TextView tv_share_weibo = (TextView) findViewById(R.id.tv_share_weibo);
        tv_share_weibo.setOnClickListener(this);

        TextView tv_share_wechatmoments = (TextView) findViewById(R.id.tv_share_wechatmoments);
        tv_share_wechatmoments.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_share_qq:
                mTencent= Tencent.createInstance(ThirdpartyConstants.QQ_APP_ID, this);
                share2QQ();
                break;
            case R.id.tv_share_wechat:
                share2weixin(0);//0是给好友
                break;
            case R.id.tv_share_weibo:
                share2weibo();
                break;
            case R.id.tv_share_wechatmoments:
                share2weixin(1);//1是给朋友圈
                break;
        }
    }



    private void share2QQ() {
            final Bundle params = new Bundle();
            params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
            params.putString(QQShare.SHARE_TO_QQ_TITLE, mShareContentEntity.getTitle());
            params.putString(QQShare.SHARE_TO_QQ_SUMMARY,  mShareContentEntity.getSummary());
            params.putString(QQShare.SHARE_TO_QQ_TARGET_URL,  mShareContentEntity.getTargetUrl());
            params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL,mShareContentEntity.getImgUrl());
            params.putString(QQShare.SHARE_TO_QQ_APP_NAME,  mShareContentEntity.getAppName());
            mTencent.shareToQQ(ShareActivity.this, params,mIUiListener );

    }

    /**
     *QQ请求回调
     */
    IUiListener mIUiListener = new IUiListener() {
        @Override
        public void onComplete(Object o) {
            Log.i("share","==QQ分享成功");
            Toast.makeText(getApplicationContext(),"分享成功",Toast.LENGTH_LONG).show();
            exit();
        }

        @Override
        public void onError(UiError uiError) {
            Log.i("share","==QQ分享失败");
            Toast.makeText(getApplicationContext(),"分享失败",Toast.LENGTH_LONG).show();
            exit();
        }

        @Override
        public void onCancel() {
            Log.i("share","==QQ分享取消");
            Toast.makeText(getApplicationContext(),"分享取消",Toast.LENGTH_LONG).show();

        }
    };



    private void share2weixin(int flag) {
        if (!mIWxAPI.isWXAppInstalled()) {
            Toast.makeText(this, "您还未安装微信客户端",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        WXWebpageObject webpage = new WXWebpageObject();
        webpage.webpageUrl = mShareContentEntity.getTargetUrl();
        WXMediaMessage msg = new WXMediaMessage(webpage);
        msg.title = mShareContentEntity.getTitle();
        msg.description = mShareContentEntity.getSummary();
        Bitmap thumb = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);
        msg.setThumbImage(thumb);
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = String.valueOf(System.currentTimeMillis());//唯一标识
        req.message = msg;
        req.scene = flag;
        SharedPreferences sp = getApplicationContext().getSharedPreferences("config",Context.MODE_PRIVATE);
        sp.edit().putString("WXTransaction", req.transaction+"").commit();
        mIWxAPI.sendReq(req);
    }

    private void share2weibo() {
        WeiboMessage weiboMessage = new WeiboMessage();
        WebpageObject mediaObject = new WebpageObject();
        mediaObject.identify = Utility.generateGUID();
        mediaObject.title = mShareContentEntity.getTitle();
        mediaObject.description = mShareContentEntity.getSummary();
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        // 设置 Bitmap 类型的图片到视频对象里         设置缩略图。 注意：最终压缩过的缩略图大小不得超过 32kb。
        mediaObject.setThumbImage(bitmap);
        mediaObject.actionUrl =mShareContentEntity.getTargetUrl();
        mediaObject.defaultText = "帮帮Angel默认文案";
        weiboMessage.mediaObject = mediaObject;
        //分享图片
//        ImageObject imageObject = new ImageObject();
//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
//        imageObject.setImageObject(bitmap);
//        weiboMessage.mediaObject = imageObject;
        SendMessageToWeiboRequest request = new SendMessageToWeiboRequest();
        request.transaction = String.valueOf(System.currentTimeMillis());
        request.message = weiboMessage;
        mWeiboShareAPI.sendRequest(ShareActivity.this, request);
    }

    /**
     * 回调处理
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // 从当前应用唤起微博并进行分享后，返回到当前应用时，需要在此处调用该函数
        // 来接收微博客户端返回的数据；执行成功，返回 true，并调用
        // {@link IWeiboHandler.Response#onResponse}；失败返回 false，不调用上述回调
        mWeiboShareAPI.handleWeiboResponse(intent, this);
    }

    /**
     * 接收微客户端博请求的数据。
     * 当微博客户端唤起当前应用并进行分享时，该方法被调用。
     *
     * @param baseResp 微博请求数据对象
     * @see {@link IWeiboShareAPI#handleWeiboRequest}
     */
    @Override
    public void onResponse(BaseResponse baseResp) {
        if (baseResp != null) {
            switch (baseResp.errCode) {
                case WBConstants.ErrorCode.ERR_OK:
                    Toast.makeText(getApplicationContext(),"分享成功",Toast.LENGTH_LONG).show();
                    exit();
                    break;
                case WBConstants.ErrorCode.ERR_CANCEL:
                    Toast.makeText(getApplicationContext(),"分享取消",Toast.LENGTH_LONG).show();
                    break;
                case WBConstants.ErrorCode.ERR_FAIL:
                    Toast.makeText(getApplicationContext(),"分享失败",Toast.LENGTH_LONG).show();
                    exit();
                    break;
            }
        }
    }

    /**
     * 判断back键
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0) {
            exit();
            return false;
        }
        return super.dispatchKeyEvent(event);
    }

    private void exit() {
        ShareActivity.this.finish();
        overridePendingTransition(R.anim.bottom_out,0);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (null != mTencent)
            mTencent.onActivityResultData(requestCode, resultCode, data, mIUiListener);
    }

}
