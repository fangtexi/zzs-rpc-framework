package com.zzs.netty.client;

import com.zzs.constants.RpcConstants;
import com.zzs.dto.RpcMessage;
import com.zzs.dto.RpcResponse;
import com.zzs.enums.CompressTypeEnum;
import com.zzs.enums.SerializationTypeEnum;
import com.zzs.factory.SingletonFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;


@Slf4j
public class NettyRpcClientHandler extends ChannelInboundHandlerAdapter {
    private final UnprocessedRequests unprocessedRequests;
    private final NettyRpcClient nettyRpcClient;

    public NettyRpcClientHandler() {
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.nettyRpcClient = SingletonFactory.getInstance(NettyRpcClient.class);
    }

    /**
     * 读取服务端返回的消息，并将返回的结果存储到 UnprocessedRequests 中，当前请求id对应的 CompletableFuture 中
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            log.info("client receive msg: [{}]", msg);
            if (msg instanceof RpcMessage) {
                RpcMessage rpcMessage = (RpcMessage) msg;
                // 消息类型
                byte messageType = rpcMessage.getMessageType();
                if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                    log.info("heart [{}]", rpcMessage.getData());
                } else if (messageType == RpcConstants.RESPONSE_TYPE) {
                    RpcResponse<Object> rpcRresponse = (RpcResponse<Object>) rpcMessage.getData();
                    // 将返回的数据存储到 UnprocessedRequests 中，当前请求id对应的 CompletableFuture 中
                    unprocessedRequests.complete(rpcRresponse);
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * 发生写空闲则向服务端发生心跳请求，服务端如果没有响应则关闭连接
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.WRITER_IDLE) {
                log.info("write idle happen [{}]", ctx.channel().remoteAddress());
                // 拿到当前服务地址对应的channel
                Channel channel = nettyRpcClient.getChannel((InetSocketAddress) ctx.channel().remoteAddress());
                // 发送 PING 命令
                RpcMessage rpcMessage = new RpcMessage();
                rpcMessage.setCodec(SerializationTypeEnum.PROTOSTUFF.getCode());
                rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
                rpcMessage.setMessageType(RpcConstants.HEARTBEAT_REQUEST_TYPE);
                rpcMessage.setData(RpcConstants.PING);
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        }else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("client catch exception：", cause);
        cause.printStackTrace();
        ctx.close();
    }
}
