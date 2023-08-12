package com.zzs.codec;


import com.zzs.compress.Compress;
import com.zzs.constants.RpcConstants;
import com.zzs.dto.RpcMessage;
import com.zzs.dto.RpcRequest;
import com.zzs.dto.RpcResponse;
import com.zzs.enums.CompressTypeEnum;
import com.zzs.enums.SerializationTypeEnum;
import com.zzs.extension.ExtensionLoader;
import com.zzs.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class RpcMessageDecoder extends LengthFieldBasedFrameDecoder {

    public RpcMessageDecoder() {
        this(RpcConstants.MAX_FRAME_LENGTH, 12, 4, 0, 0);
    }

    public RpcMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                             int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        // 解析数据
        Object decode = super.decode(ctx, in);
        if (decode instanceof ByteBuf) {
            ByteBuf frame = (ByteBuf) decode;
            // 说明含有 body 数据
            if (frame.readableBytes() >= RpcConstants.TOTAL_LENGTH) {
                try {
                    return decodeFrame(frame);
                } catch (Exception e) {
                    log.error("Decode frame error!", e);
                    throw e;
                } finally {
                    frame.release();
                }
            }
        }
        return decode;
    }

    private Object decodeFrame(ByteBuf in) {
        // 检查魔数和版本是否正确
        checkMagicNumber(in);
        checkVersion(in);
        // 获取 head 信息
        byte messageType = in.readByte();
        byte codecType = in.readByte();
        byte compressType = in.readByte();
        int requestId = in.readInt();
        int bodyLength = in.readInt();
        // 构造 RpcMessage
        RpcMessage rpcMessage = RpcMessage.builder()
                .messageType(messageType)
                .codec(codecType)
                .compress(compressType)
                .requestId(requestId)
                .build();
        // 如果是心跳请求则返回PING 心跳响应则返回 PONG
        if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
            rpcMessage.setData(RpcConstants.PING);
            return rpcMessage;
        }
        if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
            rpcMessage.setData(RpcConstants.PONG);
            return rpcMessage;
        }

        if (bodyLength > 0) {
            // 读取 body
            byte[] bytes = new byte[bodyLength];
            in.readBytes(bytes);
            // 解压 body
            String compressName = CompressTypeEnum.getName(compressType);
            Compress compress = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
            bytes = compress.decompress(bytes);
            log.debug("decompress body...");
            // 反序列化 body
            String codecName = SerializationTypeEnum.getName(codecType);
            Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codecName);
            if (messageType == RpcConstants.REQUEST_TYPE) {
                RpcRequest rpcRequest = serializer.deserialize(bytes, RpcRequest.class);
                rpcMessage.setData(rpcRequest);
            }
            if (messageType == RpcConstants.RESPONSE_TYPE) {
                RpcResponse rpcResponse = serializer.deserialize(bytes, RpcResponse.class);
                rpcMessage.setData(rpcResponse);
            }
        }

        return rpcMessage;
    }

    /**
     * 检查魔数是否正确
     *
     * @param in
     */
    private void checkMagicNumber(ByteBuf in) {
        // read the first 4 bit, which is the magic number, and compare
        int len = RpcConstants.MAGIC_NUMBER.length;
        byte[] tmp = new byte[len];
        in.readBytes(tmp);
        for (int i = 0; i < len; i++) {
            if (tmp[i] != RpcConstants.MAGIC_NUMBER[i]) {
                throw new IllegalArgumentException("Unknown magic code: " + Arrays.toString(tmp));
            }
        }
    }

    /**
     * 检查版本是否正确
     *
     * @param in
     */
    private void checkVersion(ByteBuf in) {
        // read the version and compare
        byte version = in.readByte();
        if (version != RpcConstants.VERSION) {
            throw new RuntimeException("version isn't compatible" + version);
        }
    }
}
