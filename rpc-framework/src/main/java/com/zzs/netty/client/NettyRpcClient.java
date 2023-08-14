package com.zzs.netty.client;

import com.zzs.codec.RpcMessageDecoder;
import com.zzs.codec.RpcMessageEncoder;
import com.zzs.constants.RpcConstants;
import com.zzs.dto.RpcMessage;
import com.zzs.dto.RpcRequest;
import com.zzs.dto.RpcResponse;
import com.zzs.enums.CompressTypeEnum;
import com.zzs.enums.SerializationTypeEnum;
import com.zzs.enums.ServiceDiscoveryEnum;
import com.zzs.extension.ExtensionLoader;
import com.zzs.factory.SingletonFactory;
import com.zzs.provider.ServiceProvider;
import com.zzs.registry.ServiceDiscovery;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyRpcClient {
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private final LoggingHandler loggingHandler;
    private final ChannelProvider channelProvider;
    private final ServiceDiscovery serviceDiscovery;
    private UnprocessedRequests unprocessedRequests;

    // 初始化 client
    public NettyRpcClient() {
        loggingHandler = new LoggingHandler();
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                // 连接的超时期限
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(loggingHandler)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 如果 15 秒内没有数据发送到服务端，那么就会发送一次心跳请求
                        pipeline.addLast(new IdleStateHandler(0, 15, 0, TimeUnit.SECONDS));
                        pipeline.addLast(new RpcMessageEncoder());
                        pipeline.addLast(new RpcMessageDecoder());
                        pipeline.addLast(new NettyRpcClientHandler());

                    }
                });
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension(ServiceDiscoveryEnum.ZK.getName());
    }

    /**
     * 连接服务端并获取 Channel
     *
     * @param inetSocketAddress 服务地址
     * @return channel
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public Channel doConnect(InetSocketAddress inetSocketAddress) throws ExecutionException, InterruptedException {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("The client has connected [{}] successful!", inetSocketAddress.toString());
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });
        return completableFuture.get();
    }

    public Object sendRpcRequest(RpcRequest rpcRequest) throws ExecutionException, InterruptedException {
        // 用于存储请求返回的数据
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture();
        // 获取服务端地址
        InetSocketAddress targetServerAddress = serviceDiscovery.lookupService(rpcRequest);
        // 获取服务地址关联的channel
        Channel channel = getChannel(targetServerAddress);
        if (channel.isActive()) {
            // 将请求id以及对应的CompletableFuture存储到 UnprocessedRequests 用于后续获取请求结果
            unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
            RpcMessage rpcMessage = RpcMessage.builder()
                    .codec(SerializationTypeEnum.KYRO.getCode())
                    .compress(CompressTypeEnum.GZIP.getCode())
                    .data(rpcRequest)
                    .messageType(RpcConstants.REQUEST_TYPE)
                    .build();
            // 发送消息给服务端
            channel.writeAndFlush(rpcMessage).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        log.info("client send message: [{}]", rpcMessage);
                    }else {
                        future.channel().close();
                        resultFuture.completeExceptionally(future.cause());
                        log.error("Send failed:", future.cause());
                    }
                }
            });
        }else {
            throw new IllegalStateException();
        }
        return resultFuture;
    }

    /**
     * 从 ChannelProvider 中获取服务对应的 channel
     *
     * @param inetSocketAddress
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public Channel getChannel(InetSocketAddress inetSocketAddress) throws ExecutionException, InterruptedException {
        Channel channel = channelProvider.get(inetSocketAddress);
        if (channel == null) {
            channel = doConnect(inetSocketAddress);
            channelProvider.set(inetSocketAddress, channel);
        }
        return channel;
    }

    public void close() {
        eventLoopGroup.shutdownGracefully();
    }
}
