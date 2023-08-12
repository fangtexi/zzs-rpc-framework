package com.zzs.netty.server;

import com.zzs.codec.RpcMessageDecoder;
import com.zzs.codec.RpcMessageEncoder;
import com.zzs.config.RpcServiceConfig;
import com.zzs.factory.SingletonFactory;
import com.zzs.hook.CustomShutdownHook;
import com.zzs.provider.ServiceProvider;
import com.zzs.provider.impl.ZkServiceProviderImpl;
import com.zzs.utils.RuntimeUtil;
import com.zzs.utils.ThreadPoolFactoryUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
@Slf4j
public class NettyRpcServer {
    public final  static int PORT = 8566;

    private static LoggingHandler loggingHandler = new LoggingHandler();
    // 用于向注册中心注册服务
    private final ServiceProvider serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
    public void registerService(RpcServiceConfig rpcServiceConfig) {
        serviceProvider.publishService(rpcServiceConfig);
    }

    public void start(){
        // 服务关闭时清空 zookeeper 中的节点以及关闭所有线程池
        CustomShutdownHook.getCustomShutdownHook().clearAll();
        // NioEventLoopGroup 任务细分
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        // serviceHandlerGroup 负责处理业务逻辑
        DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
                RuntimeUtil.cpus() * 2,
                ThreadPoolFactoryUtil.createThreadFactory("service-handler-group", false)
        );

        try {
            // 获取网卡IP地址
            String host = InetAddress.getLocalHost().getHostAddress();

            ServerBootstrap bootstrap = new ServerBootstrap();

            ChannelFuture channelFuture = bootstrap
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 是否开启 TCP 底层心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    //表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .handler(loggingHandler)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 30 秒之内没有收到客户端请求的话就关闭连接
                            pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            // 编解码器
                            pipeline.addLast(new RpcMessageEncoder());
                            pipeline.addLast(new RpcMessageDecoder());
                            // 自定义 handler
                            pipeline.addLast(serviceHandlerGroup, new NettyRpcServerHandler());
                        }
                    }).bind(host, PORT).sync();
            // 等待服务端监听端口关闭
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("occur exception when start server:", e);
        } finally {
            log.error("shutdown bossGroup and workerGroup");
            // 关闭事件组
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            serviceHandlerGroup.shutdownGracefully();
        }
    }
}
