package com.fulltopic.singletonservice.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class SingleServerNormalHandler extends SimpleChannelInboundHandler<Object>
{
    private final static Logger LOG = LoggerFactory.getLogger(SingleServerNormalHandler.class);
    private static final int RANDOMSIZE = 5000;
    private static AtomicInteger sequer = new AtomicInteger(0);

    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception
    {
//        LOG.info("Received request " + sequer.getAndIncrement());
        int randNum = (int)(Math.random() * RANDOMSIZE);
        SingletonService.GetInstance().sendRequest(randNum);
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        cause.printStackTrace();
        ctx.close();
    }
}