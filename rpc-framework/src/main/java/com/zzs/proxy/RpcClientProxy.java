package com.zzs.proxy;

import com.zzs.config.RpcServiceConfig;
import com.zzs.dto.RpcRequest;
import com.zzs.dto.RpcResponse;
import com.zzs.enums.RpcErrorMessageEnum;
import com.zzs.enums.RpcResponseCodeEnum;
import com.zzs.exception.RpcException;
import com.zzs.netty.client.NettyRpcClient;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RpcClientProxy implements InvocationHandler {
    private static final String INTERFACE_NAME = "interfaceName";

    private final NettyRpcClient nettyRpcClient;
    private final RpcServiceConfig rpcServiceConfig;

    public RpcClientProxy(NettyRpcClient nettyRpcClient, RpcServiceConfig rpcServiceConfig) {
        this.nettyRpcClient = nettyRpcClient;
        this.rpcServiceConfig = rpcServiceConfig;
    }

    public <T> T getProxy(Class<T> clazz) {
        return(T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz},this );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("invoked method: [{}]", method.getName());
        RpcRequest rpcRequest = RpcRequest.builder()
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .parameters(args)
                .version(rpcServiceConfig.getVersion())
                .paramTypes(method.getParameterTypes())
                .group(rpcServiceConfig.getGroup())
                .requestId(UUID.randomUUID().toString())
                .build();
        RpcResponse<Object> rpcResponse = null;
        // 向服务端发送请求
        CompletableFuture<RpcResponse<Object>> completableFuture = (CompletableFuture<RpcResponse<Object>>) nettyRpcClient.sendRpcRequest(rpcRequest);
        rpcResponse = completableFuture.get();
        this.check(rpcResponse,rpcRequest);
        return rpcResponse.getData();
    }

    private void check(RpcResponse<Object> rpcResponse, RpcRequest rpcRequest) {
        if (rpcResponse == null) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if (!rpcRequest.getRequestId().equals(rpcResponse.getRequestId())) {
            throw new RpcException(RpcErrorMessageEnum.REQUEST_NOT_MATCH_RESPONSE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if (rpcResponse.getCode() == null || !rpcResponse.getCode().equals(RpcResponseCodeEnum.SUCCESS.getCode())) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
    }
}
