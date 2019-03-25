package com.example.elon.sockettest;


import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;


public class LoadingDialog extends AlertDialog {

    private ImageView progressView;
    private TextView message;
    private String text = "";

    public LoadingDialog(Context context) {
        super(context, R.style.loadingDialogStyle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loadingdialog);
        Window win = getWindow();
        progressView = (ImageView) findViewById(R.id.progressBar);
        message = (TextView) findViewById(R.id.message);
        message.setText(text);
        if (text.equals(""))
            message.setVisibility(View.GONE);
        else {
            message.setVisibility(View.VISIBLE);
        }
        RotateAnimation animation = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        animation.setInterpolator(new LinearInterpolator());
        animation.setDuration(1500);
        animation.setRepeatCount(Integer.MAX_VALUE);
        animation.setRepeatMode(Animation.RESTART);
        progressView.setImageResource(R.drawable.ic_svstatus_loading);
        progressView.startAnimation(animation);
        animation.start();
    }

    public void setMessage(String message) {
        text = message;
        if (this.message != null && !TextUtils.isEmpty(text)) {
            this.message.setVisibility(View.VISIBLE);
            this.message.setText(text);
        }
    }

    @Override
    public void setCancelable(boolean flag) {
        super.setCancelable(flag);
    }
}