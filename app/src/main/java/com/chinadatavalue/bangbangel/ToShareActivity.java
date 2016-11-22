package com.chinadatavalue.bangbangel;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * Created by Administrator on 2016/11/22.
 */

public class ToShareActivity extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toshare);
    }

    public void btn_share(View view) {
        startActivity(new Intent(this,ShareActivity.class));
    }
}
