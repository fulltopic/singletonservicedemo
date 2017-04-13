package com.fulltopic.singletonservice.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * Created by zf on 17-4-1.
 */
public class SingleServer {
    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = 8009;


    public static final int BOSSTHDNUM = 2;
    public static final int WORKERTHDNUM = 4;

    public static void main(String args[]) throws Exception
    {
        final SslContext sslCtx;
        if(SSL)
        {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        }else {
            sslCtx = null;
        }

        EventLoopGroup bossGroup = new NioEventLoopGroup(BOSSTHDNUM) ;
        EventLoopGroup workerGroup = new NioEventLoopGroup(WORKERTHDNUM);

        try
        {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        public void initChannel(SocketChannel ch)
                        {
                            ChannelPipeline p = ch.pipeline();
                            if(sslCtx != null)
                            {
                                p.addLast(sslCtx.newHandler(ch.alloc()));
                            }
                            p.addLast(new SingleServerNormalHandler());
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(PORT).sync();

            SingletonChecker checker = new SingletonChecker();
            checker.check();

            f.channel().closeFuture().sync();
        }finally{
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

}

