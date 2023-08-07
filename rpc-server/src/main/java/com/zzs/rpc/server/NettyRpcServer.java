package com.zzs.rpc.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyRpcServer {

    public void start() {
        // NioEventLoopGroup 任务细分
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();


        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap
                    .group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
        }

    }
}
