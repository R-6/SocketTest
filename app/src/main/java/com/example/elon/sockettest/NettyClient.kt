package com.example.elon.sockettest

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class NettyClient {


    /*是否连接*/
    private var isConnect = false

    /*伴生对象*/
    companion object {
        var instance = NettyClient()
    }


    private lateinit var group: NioEventLoopGroup

    private lateinit var bootstrap: Bootstrap

    private var channel: Channel? = null


    /*连接*/
    fun connect(ip: String, port: String): Observable<Boolean> {

        return Observable.create<Boolean>({
            group = NioEventLoopGroup()
            bootstrap = Bootstrap()
                    .remoteAddress(ip, port.toInt())
                    .group(group)
                    .channel(NioSocketChannel::class.java)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                    .handler(object : ChannelInitializer<SocketChannel>() {
                        override fun initChannel(sc: SocketChannel) {
                            var pipeline = sc.pipeline()
                            pipeline.addLast(LoggingHandler(LogLevel.INFO))

                            /*byte类型编解码*/
//                        pipeline.addLast(ByteArrayDecoder())
//                        pipeline.addLast(ByteArrayEncoder())

                            /*string类型编解码*/
                            pipeline.addLast(StringDecoder())
                            pipeline.addLast(StringEncoder())
                        }
                    })


            try {
                channel = bootstrap.connect().sync().channel()
                it.onNext(channel!!.isActive)
                isConnect = channel!!.isActive
            } catch (e: Exception) {
                it.onNext(false)
                it.onError(e)
                isConnect = false
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())


    }


    /*发送命令*/
    fun sendOrder(order: String): Observable<Boolean> {


        return Observable.create<Boolean>({ emitter ->

            if (isConnect) {
                channel?.writeAndFlush(order)?.addListener {
                    emitter.onNext(it.isSuccess)
                }
            } else {
                emitter.onNext(false)
            }


        }).subscribeOn(Schedulers.io())//这里注意要在工作线程发送
                .observeOn(AndroidSchedulers.mainThread())


    }

    /*是否连接*/
    fun isConnect(): Boolean {
        return isConnect
    }

    /*重连*/
    fun reConnect(ip: String, port: String): Observable<Boolean> {
        disConnect()
        return connect(ip, port)

    }


    /*关闭连接*/
    fun disConnect() {

        isConnect = false
        group.shutdownGracefully()

    }

}