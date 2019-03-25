package com.example.elon.sockettest

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import me.leolin.shortcutbadger.ShortcutBadger
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Toast
import com.blankj.utilcode.util.*
import java.util.*


class MainActivity : AppCompatActivity(), ProgressDialogHandler.ProgressCancelListener {

    var count =0    //外部消息数
    var mProgressDialogHandler : ProgressDialogHandler? = null
    var serviceConnection:ServiceConnection?=null
    var binder:SocketService.SocketBinder? = null
    var socketService :SocketService? = null
    var wifiConnector:WifiConnector?= null
    var wifiManager:WifiManager?= null
    var timerTask :TimerTask? = null
    var handler : Handler? = null
    var timer = Timer()
    val WIFI_NAME = "ZWKJ"
    var net_state = ""
    var message = ""
    var reConnectCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

//        RxJavaPlugins.setErrorHandler {
//            //异常处理
//        }

        wifiManager = getApplicationContext().getSystemService(WIFI_SERVICE) as WifiManager

        handler = object :Handler(){
            override fun handleMessage(msg: Message?) {
                super.handleMessage(msg)
                when(msg?.what){
                    1 -> tv_log.setText(message)
                }
            }
        }

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }


        bt5.setOnClickListener { view ->
            Snackbar.make(view, NetUtils.getNetworkTypeName(applicationContext)+"--" +NetUtils.getWifiConnectionInfo(applicationContext).ssid + intToIp(NetUtils.getWifiConnectionInfo(applicationContext).ipAddress), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        bt6.setOnClickListener { view ->
            Snackbar.make(view, if (NetUtils.ping()) "success" else "error", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show()
        }

        bt7.setOnClickListener { view ->
            Snackbar.make(view, if (NetUtils.isNetworkOnline()) "success" else "error", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show()
        }

        bt8.setOnClickListener { view ->
            count++
            tv_num.setTextColor(Color.parseColor("#57C976"))
            tv_num.setText("$count")
//            tv_num.setText(SpanUtils().append("$count").setUnderline().create())
            ShortcutBadger.applyCount(applicationContext,count)
        }

        bt9.setOnClickListener { view ->
            if (count>0){
                count--
            }
            tv_num.setTextColor(Color.parseColor("#FF8080"))
            tv_num.setText("$count")

//            tv_num.setText(SpanUtils().append("$count").setUnderline().create())
            ShortcutBadger.applyCount(applicationContext,count)
        }

        bt_connect.setOnClickListener {
//            connectSocket(it)
//            startService(Intent(this, SocketService::class.java))
            binder?.startSocketConnect()
        }


        bt_disconnet.setOnClickListener {
            binder?.stopConnect()
        }

        bt_disconnet_wifi.setOnClickListener {
            //断开当前wifi(忘记网络)
            wifiManager?.disableNetwork(wifiManager?.connectionInfo!!.getNetworkId())
        }

        //一键连接
        bt_one_connect.setOnClickListener {
            net_state = if (NetworkUtils.getWifiEnabled()) "wifi" else "mobile"
            startConnect()
        }

        //清空文本
        bt_clean_text.setOnClickListener {
            message = ""
            logMsg(0,"")
        }

        //绑定服务
        bindSocketService()

    }

    private fun logMsg(i :Int ,data:String){
        //弹窗消失
        when (i) {
            //重连
            1, 2, 3, 4401 -> {
                if (reConnectCount<2){
                    binder?.startSocketConnect()
                    reConnectCount++
                }
                else{
                    reConnectCount = 0
                    mProgressDialogHandler?.obtainMessage(ProgressDialogHandler.DISMISS_PROGRESS_DIALOG)?.sendToTarget()
                    mProgressDialogHandler = null
                    ToastUtils.showShort(data)
                }
            }
            4,200,201, 202, 203 -> {
                reConnectCount = 0
                mProgressDialogHandler?.obtainMessage(ProgressDialogHandler.DISMISS_PROGRESS_DIALOG)?.sendToTarget()
                mProgressDialogHandler = null
                ToastUtils.showShort(data)
            }
        }
//        if (reConnectCount<2){
//            when(i){
//                1,2,3,4401 ->binder?.startSocketConnect()
//                else -> reConnectCount = 0
//            }
//        }

        //打印log
        message = message+data+"\n"
        val handlerMsg = Message()
        handlerMsg.what = 1
        handler?.sendMessage(handlerMsg)
    }

    override fun onCancelProgress() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    //一键连接
    private fun startConnect(){
        if (!checkPermission()){
            requestPermission()
        }else {

            //弹出dialog框
            mProgressDialogHandler = ProgressDialogHandler(this, this, false, "正在读卡")
            mProgressDialogHandler?.obtainMessage(ProgressDialogHandler.SHOW_PROGRESS_DIALOG)?.sendToTarget()

            //当前网络状态
            if (!NetworkUtils.getWifiEnabled()){
                NetworkUtils.setWifiEnabled(true)
                Log.d("wifi","open_wifi")
                Handler().postDelayed({startConnect()},500)
//                startConnect()
                return
            }

            //是否已连接上局域网
            val ssid = wifiManager!!.connectionInfo.ssid
            if (ssid.startsWith("\"") && ssid.endsWith("\"")){
                if (ssid.removeRange(0,1).startsWith(WIFI_NAME)){
                    binder?.startSocketConnect()
                    return
                }
            }else{
                if (ssid.startsWith(WIFI_NAME)){
                    binder?.startSocketConnect()
                    return
                }
            }

            wifiConnector =object :WifiConnector(wifiManager!!){
                override fun onSuccess() {
                    super.onSuccess()
                    logMsg(0,"已连接到"+WIFI_NAME)
//                    Thread.sleep(500)
                    binder?.startSocketConnect()
//                    Handler().postDelayed({binder?.startSocketConnect()},1000)
                }
            }

            //扫描wifi网络，有延迟，使用休眠线程方法，0.2扫描一次,3s超时
            //!!Android 8.0后2分钟内只能请求4次，否则返回false
//            while (!wifiManager!!.startScan()){
//                Log.d("wifi","scan_once")
//                Thread.sleep(200)
//            }
            wifiManager?.startScan()
            Thread.sleep(300)
            var scanResults = wifiManager!!.getScanResults()

            var count = 0
            while (scanResults == null || scanResults.size == 0){
                Thread.sleep(200)
                scanResults = wifiManager!!.getScanResults()
                Log.d("wifi_info:",scanResults.toString())
                count++
                if (count>=20){
                    break
                }
            }

            var has_wifi = false
            for (i in 0 until scanResults.size) {
                if (scanResults[i].SSID.startsWith(WIFI_NAME)) {
                    has_wifi = true
                    wifiConnector?.connect(scanResults[i].SSID,"", WifiConnector.WifiCipherType.WIFICIPHER_NOPASS)
                    break
                }
            }
            if (!has_wifi){
//                ToastUtils.showShort("未搜索到读卡器")
                logMsg(4,"未搜索到读卡器")
//                startConnect()
                return
            }
        }
    }

    /**
     * 检查是否已经授予权限
     * @return
     */
    private fun checkPermission():Boolean{
        val NEEDED_PERMISSIONS = listOf( Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION)
        for (per in NEEDED_PERMISSIONS){
            if (ActivityCompat.checkSelfPermission(this, per)
                    != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    /**
     * 申请权限
     */
    private fun requestPermission() {
        val NEEDED_PERMISSIONS = arrayOf( Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION)
        ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, 0)
    }

    /**
     * 申请权限结果回调
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("Permission",requestCode.toString()+permissions)
        var hasAllPermission = true
        if (requestCode == 0) {
            var j= 1
            for (i in grantResults) {
                if (i != PackageManager.PERMISSION_GRANTED) {
                    hasAllPermission = false   //判断用户是否同意获取权限
                    Log.d("Permission","fail"+j.toString())
                    j++
                    break
                }
            }
            //如果同意权限
            if (hasAllPermission) {
//                mHasPermission = true
                if (NetUtils.isConnected(this@MainActivity)) {  //如果wifi开关是开 并且 已经获取权限
//                    sortScaResult()
                    Toast.makeText(this@MainActivity, "已授权", Toast.LENGTH_SHORT).show()
                    startConnect()
                } else {
                    Toast.makeText(this@MainActivity, "WIFI处于关闭状态或权限获取失败", Toast.LENGTH_SHORT).show()
                }
            } else {  //用户不同意权限
//                mHasPermission = false
                Toast.makeText(this@MainActivity, "获取权限失败", Toast.LENGTH_SHORT).show()
            }
        }
    }


    //绑定服务，创建实例
    private fun bindSocketService() {

        /*通过binder拿到service*/
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
                binder = iBinder as SocketService.SocketBinder
                socketService = binder?.service
                socketService?.setCallback(object : SocketService.Callback {
                    override fun onSuccess(data: String?) {
                        disConnect(data!!)
                        logMsg(0,data)
                    }
                    override fun onFail(code: Int, msg: String?) {
                        var failinfo = msg
                        when(code){
                            1 -> failinfo = "连接超时，请重试"
                            2 -> failinfo = "找不到读卡器，请检查"
                            3 -> failinfo = "连接异常或被拒绝，请尝试手动连接WiFi"
                            201 -> failinfo = "未识别到卡"
                            202 -> failinfo = "卡有密码，无法读取"
                            203 -> failinfo = "硬件初始化失败"
                            4401 ->failinfo =  "失败 请重试"
                        }
                        logMsg(code,failinfo!!)
                    }
                })
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
            }
        }

        val intent = Intent(this, SocketService::class.java)
//        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    //断开局域网，根据之前的网络状态连接外网
    private fun disConnect(data : String){
        when(net_state){
            "wifi" -> {
                wifiManager?.disableNetwork(wifiManager?.connectionInfo!!.getNetworkId())
            }
            "mobile" -> {
                wifiManager?.setWifiEnabled(false)
            }
        }

        timerTask?.cancel()
        count = 0
        timerTask = object : TimerTask() {
            override fun run() {
                count++
                if (NetUtils.ping()){
//                    ToastUtils.showShort("已连上外网"+data)
                    logMsg(200,"已连上外网"+data)
                    //todo()
                    timerTask?.cancel()
                }
                if (count == 10) {
                    timerTask?.cancel()
                }
            }
        }
        timer.schedule(timerTask, 1000, 2000)
    }

    //int转IP地址
    fun intToIp(i:Int):String{
        return "${(i and 0xFF)}.${(i shr 8 and 0xFF)}.${(i shr 16 and 0xFF)}.${(i shr 24 and 0xFF)}"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        if (serviceConnection != null && socketService != null){
            unbindService(serviceConnection)
        }
        if (timerTask != null) {
            timerTask!!.cancel()
            timerTask = null
        }
        timer.purge()
        timer.cancel()
        super.onDestroy()
    }
}
