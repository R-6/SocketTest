package com.example.elon.sockettest;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by admin on 2018/8/3.
 */

public class RxSocket {
    static RxSocket rxSocket;
    OutputStream outputStream = null;
    Socket socket = null;
    InputStream inputStream = null;
    /** 读取数据超时时间 */
    final int READ_TIMEOUT = 15 * 1000;
    /** 连接超时时间 */
    final int CONNECT_TIMEOUT = 5 * 1000;
    final String IP = "218.29.74.138";
    final int PORT = 11274;
    final String TAG = "RxSocket-->",SUCCEED="初始化成功",TIMEOUT="连接超时",SEND_ERROR="发送数据异常";
    /** 网络返回的监听 */
//    SocketListener observer;
    public boolean isCancle = false;

    private RxSocket() {
//        Observable.just("")
//                .subscribeOn(Schedulers.io())
//                .subscribeOn(AndroidSchedulers.mainThread())
//                .doOnNext(s -> initSocket(IP, PORT))
//                .subscribe(s -> Log.d(TAG, SUCCEED));
    }

    public static RxSocket getInstance() {
        if (rxSocket == null) {
            rxSocket = new RxSocket();
        }
        return rxSocket;
    }


    /**
     * 初始化蓝牙通信，需要放在子线程
     * @param ip {@link RxSocket#IP}  ip地址
     * @param port {@link RxSocket#PORT} 端口号
     */
    private void initSocket(String ip,int port) throws Exception{
        try {
            socket = new Socket();
            socket.setSoTimeout(READ_TIMEOUT);
            Log.d(TAG, ip+":"+port);
            socket.connect(new InetSocketAddress(InetAddress.getByName(ip),port),CONNECT_TIMEOUT);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
        } catch (IOException e) {
            Log.d(TAG, TIMEOUT);
            e.printStackTrace();
            throw new Exception(TIMEOUT);
        }
    }

    /**
     * 发送数据
     * @return 接口返回的数据
     */
    private String sendData(String data) throws Exception{
        StringBuilder result = new StringBuilder("");
        try {
            outputStream.write(data.getBytes("UTF-8"));
            byte[] b = new byte[1024];
            int reads = inputStream.read(b);
            while (reads > 0) {
                byte[] bytes = Arrays.copyOfRange(b, 8, reads);
                String temp = new String(bytes);
                result.append(temp);
                reads = 0;
                b = new byte[1024];
                reads = inputStream.read(b);
            }
            Log.d(TAG, result.toString());
            return result.toString();
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
            throw new Exception(SEND_ERROR);
        }
    }

    /**
     * 取消所有的请求
     * @param isCancle true:取消访问  false:允许访问
     */
//    public void cancleAll(boolean isCancle) {
//        this.isCancle = isCancle;
//        observer.cancleListen();
//    }

    /**
     * socket 发送数据，并返回数据
     * @param baseRequestBean  可以一次发送多个请求
     *                         （后期可以添加重试机制）
     */
//    public void request(BaseRequestBean ...baseRequestBean) {
//        if (observer!=null)
//            Observable.fromArray(baseRequestBean)
//                    .subscribeOn(Schedulers.io())
//                    .filter(baseRequestBean1 -> isCancle)
//                    .map(baseRequestBean1 -> {
//                        String result = sendData(baseRequestBean1.get());
//                        if (result.length()>0 && isCancle)
//                            baseRequestBean1.parseData(result);
//                        return baseRequestBean1;
//                    })
//                    .filter(baseRequestBean1 -> isCancle)
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(observer);
//    }

    /**
     * 设置网络返回的监听
     * @param listener
     */
//    public void setResultListener(SocketListener listener) {
//        this.observer = listener;
//    }
}