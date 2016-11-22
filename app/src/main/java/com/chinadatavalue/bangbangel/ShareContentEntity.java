package com.chinadatavalue.bangbangel;

/**
 * Created by Administrator on 2016/11/8.
 */

public class ShareContentEntity {
    String Title;//分享标题
    String Summary;//分享摘要
    String ImgUrl;//分享图片链接
    String TargetUrl;//分享网页链接
    String AppName;//分享APP名

    public String getTitle() {
        return Title;
    }

    public void setTitle(String Title) {
        this.Title = Title;
    }

    public String getImgUrl() {
        return ImgUrl;
    }

    public void setImgUrl(String ImgUrl) {
        this.ImgUrl = ImgUrl;
    }

    public String getSummary() {
        return Summary;
    }

    public void setSummary(String Summary) {
        this.Summary = Summary;
    }

    public String getTargetUrl() {
        return TargetUrl;
    }

    public void setTargetUrl(String TargetUrl) {
        this.TargetUrl = TargetUrl;
    }

    public String getAppName() {
        return AppName;
    }

    public void setAppName(String AppName) {
        this.AppName = AppName;
    }
}
