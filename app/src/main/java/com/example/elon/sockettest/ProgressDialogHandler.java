package com.example.elon.sockettest;


import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by Luther on 2017/4/4.
 */
public class ProgressDialogHandler extends Handler {

    public static final int SHOW_PROGRESS_DIALOG = 1;
    public static final int DISMISS_PROGRESS_DIALOG = 2;

    public static LoadingDialog proDialog;

    private Context context;
    private boolean cancelable;
    private ProgressCancelListener mProgressCancelListener;
    private String message;

    public ProgressDialogHandler(Context context, ProgressCancelListener mProgressCancelListener,
                                 boolean cancelable,String message) {
        super();
        this.context = context;
        this.mProgressCancelListener = mProgressCancelListener;
        this.cancelable = cancelable;
        this.message = message;
    }

    private void initProgressDialog() {

        if (proDialog == null) {
            Log.d("Handler", "false");
            proDialog = new LoadingDialog(context);

            proDialog.setCancelable(cancelable);
            proDialog.setMessage(message);

            if (cancelable) {
                proDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        mProgressCancelListener.onCancelProgress();
                    }
                });
            }

            if (!proDialog.isShowing()) {
                proDialog.show();
            }
        } else {
            Log.d("Handler", "true");
            proDialog.setCancelable(cancelable);

            if (cancelable) {
                proDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        mProgressCancelListener.onCancelProgress();
                    }
                });
            }
            if (!proDialog.isShowing()) {
                proDialog.show();
            }
        }
    }

    private void dismissProgressDialog() {
        try {
            if (proDialog != null && proDialog.isShowing()) {
                proDialog.dismiss();
                proDialog = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case SHOW_PROGRESS_DIALOG:
                initProgressDialog();
                break;
            case DISMISS_PROGRESS_DIALOG:
                dismissProgressDialog();
                break;
        }
    }

    public interface ProgressCancelListener{
        void onCancelProgress();
    }
}

