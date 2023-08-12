package com.zzs.netty.client;

import com.zzs.codec.RpcMessageDecoder;
import com.zzs.codec.RpcMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyRpcClient {
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private final LoggingHandler loggingHandler;

    // 初始化 client
    public NettyRpcClient() {
        loggingHandler = new LoggingHandler();
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap
                .group(eventLoopGroup)
                .channel(SocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,5000)
                .handler(loggingHandler)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 如果 15 秒内没有数据发送到服务端，那么就会发送一次心跳请求
                        pipeline.addLast(new IdleStateHandler(0,15,0, TimeUnit.SECONDS));
                        pipeline.addLast(new RpcMessageEncoder());
                        pipeline.addLast(new RpcMessageDecoder());

                    }
                });
    }
}
