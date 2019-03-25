package com.example.elon.sockettest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import android.widget.Toast;

import com.blankj.utilcode.util.ToastUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class SocketService extends Service {
    /*socket*/
    private Socket socket;
    /*连接线程*/
    private Thread connectThread;
    private Timer timer = new Timer();
    private OutputStream outputStream;

    private SocketBinder sockerBinder = new SocketBinder();
    private final String ip = "192.168.4.1";
    private final int port = 8080;
    private TimerTask task;

    private PendingIntent pendingIntent;
    private NotificationManager manager;
    private Callback callback;
    private int reConnectCount1 = 0;     //连接超时重连次数,每次连接成功后重置
    private int reConnectCount2 = 0;     //连接异常重连次数,每次连接成功后重置


    @Override
    public IBinder onBind(Intent intent) {
        return sockerBinder;
    }

    public class SocketBinder extends Binder {
        /*返回SocketService 在需要的地方可以通过ServiceConnection获取到SocketService  */
        public SocketService getService() {
            return SocketService.this;
        }
        public void startSocketConnect(){
            initSocket();
        }
        public void stopConnect(){
            releaseSocket(false);
        }

    }

    @Override
    public void onCreate() {
        //跳转到的Activity
        Intent intent = new Intent(this,MainActivity.class);
        pendingIntent = PendingIntent.getActivity(this,0,intent,0);
        manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        super.onCreate();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /*初始化socket*/
        initSocket();
        return super.onStartCommand(intent, flags, startId);
    }


    public void setCallback(Callback callback){
        this.callback = callback;
    }
    /*初始化socket*/
    private void initSocket() {
        //已成功连接则返回，否则创建新线程
        if (connectThread!=null){
            releaseSocket(true);
            return;
        }

        connectThread = new Thread(new Runnable() {
            @Override
            public void run() {

                socket = new Socket();
                try {
                    /*超时时间为3秒*/
                    Log.d("msg", "开始连接");
                    socket.connect(new InetSocketAddress(ip, port), 2000);
                    /*连接成功的话  发送心跳包*/
                    if (socket.isConnected()) {
                        reConnectCount1 = 0;
                        reConnectCount2 = 0;

                        /*显示状态栏通知*/
                        callback.onFail(0,"socket已连接");
                        showNotification("socket已连接");

                        /*发送连接成功的消息*/
//                            EventMsg msg = new EventMsg();
//                            msg.setTag(SyncStateContract.Constants.CONNET_SUCCESS);
//                            EventBus.getDefault().post(msg);
                        /*发送心跳数据*/
                        sendBeatData();

                        //发送获取卡号请求
                        sendOrder(1);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    if (e instanceof SocketTimeoutException) {
                        //连接超时
                        if (reConnectCount1 < 2){
                            callback.onFail(0,"连接超时，正在重新连接");
                            reConnectCount1++;
                            releaseSocket(true);
                        }else {
                            callback.onFail(1,"连接超时，请重新连接");
                            reConnectCount1=0;
                            releaseSocket(false);
                            stopSelf();
                        }

                    } else if (e instanceof NoRouteToHostException) {
                        callback.onFail(2,"该地址不存在，请检查");
//                        toastMsg("该地址不存在，请检查");
                        releaseSocket(false);
                        stopSelf();

                    } else if (e instanceof ConnectException) {
                        //连接异常
                        if (reConnectCount2 < 3){
                            callback.onFail(0,"连接异常或被拒绝，正在重新连接");
                            reConnectCount2++;
                            try {
                                connectThread.sleep(1000);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                            releaseSocket(true);
                        }else {
                            callback.onFail(3,"连接异常或被拒绝，请检查");
                            reConnectCount2=0;
                            releaseSocket(false);
                            stopSelf();
                        }
                    }
                }
            }
        });

        /*启动连接线程*/
        connectThread.start();

    }


    /*因为Toast是要运行在主线程的   所以需要到主线程哪里去显示toast*/
//    private void toastMsg(final String msg) {
//
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
//            }
//        });
////        getNotifivationManager().notify(1,getNotificatioin((msg)));
//    }


    //接收数据服务器发来的消息
    public void receive(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!socket.isClosed() && socket.isConnected()) {//如果服务器没有关闭
                        InputStream inputStream = socket.getInputStream();
                        byte[] buffer = new byte[1024];
                        inputStream.read(buffer);
//                        callback.onFail(0,new String(buffer));
//                        callback.onFail(0,String.valueOf(new String(buffer).length()));
//                        callback.onFail(0,String.valueOf(new String(buffer).trim().length()));
                        ReceviceBean result = new Gson().fromJson(new String(buffer).trim(),ReceviceBean.class);
                        if (!result.getCode().equals("200")){
                            callback.onFail(Integer.valueOf(result.getCode()),result.getMessage());
                            showNotification(result.getMessage());
                        }else {
                            callback.onSuccess(result.getData().getCardID());
                            releaseSocket(false);
                            stopSelf();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }


    /*发送数据*/
    private void sendOrder(final int order) {
        if (socket != null && socket.isConnected()) {
            /*发送指令*/
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if ((outputStream = socket.getOutputStream()) != null) {
                            CardInfoBean cardInfo = new CardInfoBean();
                            cardInfo.setOperation(order);
                            outputStream.write((new Gson().toJson(cardInfo)).getBytes("UTF-8"));
                            outputStream.flush();
                            callback.onFail(0,"发送了:"+(new Gson().toJson(cardInfo)));
                            receive();
                        }else {
                            callback.onFail(0,"outputStream为空");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }).start();

        } else {
            callback.onFail(0,"socket连接错误,请重试");
        }
    }


    /*发送心跳包数据*/
    private void sendBeatData() {
        if (timer == null) {
            timer = new Timer();
        }
        if (task == null) {
            task = new TimerTask() {
                @Override
                public void run() {
                    try {
                        outputStream = socket.getOutputStream();
                        /*这里的编码方式根据你的需求去改*/
                        outputStream.write(("android_beat").getBytes("UTF-8"));
                        outputStream.flush();
                    } catch (Exception e) {
                        /*发送失败说明socket断开了或者出现了其他错误*/
                        callback.onFail(0,"连接断开，正在重连");
                        /*重连*/
                        releaseSocket(true);
                        e.printStackTrace();
                    }
                }
            };
        }
        timer.schedule(task, 0, 3000);

    }


    /*释放资源，是否重连*/
    private void releaseSocket(Boolean isReConnect) {

        if (task != null) {
            task.cancel();
            task = null;
        }
        if (timer != null) {
            timer.purge();
            timer.cancel();
            timer = null;
        }

        if (outputStream != null) {
            try {
                outputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStream = null;
        }

        if (socket != null) {
            try {
                socket.close();

            } catch (IOException e) {
            }
            socket = null;
        }

        if (connectThread != null) {
            connectThread = null;
        }

        NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (manager!=null){
            manager.cancel(1);
        }

        /*重新初始化socket*/
        if (isReConnect) {
            initSocket();
        }

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("SocketService", "onDestroy");
        callback.onFail(0,"service已断开");
//        toastMsg("service已断开");
//        isReConnect = false;
        releaseSocket(false);
    }


    private void showNotification(String str){

        if(Build.VERSION.SDK_INT >= 26) {
            //当sdk版本大于26
            String id = "channel_1";
            String description = "143";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(id, description, importance);
//                     channel.enableLights(true);
                     channel.enableVibration(false);
            manager.createNotificationChannel(channel);
            Notification notification = new Notification.Builder(this, id)
                    .setCategory(Notification.CATEGORY_PROGRESS)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher))
                    .setContentTitle("写卡器已连接")
                    .setContentText(str)
                    .setContentIntent(pendingIntent)
//                    .setAutoCancel(true)
                    .build();
            manager.notify(1, notification);
        }
        else {
            //当sdk版本小于26
            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("写卡器已连接")
                    .setContentText(str)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher))
                    .build();
            manager.notify(1,notification);
        }
    }

    public interface Callback{
        void onSuccess(String data);
        void onFail(int code,String msg);
    }
}
