package com.zzs.netty.client;

import com.zzs.dto.RpcResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保存服务端未处理的消息，防止消息丢失
 */
public class UnprocessedRequests {
    // 请求id与请求CompletableFuture的映射关系
    private static final Map<String, CompletableFuture<RpcResponse<Object>>> UNPROCESSED_RESPONSE_FUTURES = new ConcurrentHashMap<>();

    public void put(String requestId,CompletableFuture<RpcResponse<Object>> future) {
        UNPROCESSED_RESPONSE_FUTURES.put(requestId, future);
    }

    public void complete(RpcResponse<Object> rpcResponse) {
        CompletableFuture<RpcResponse<Object>> future = UNPROCESSED_RESPONSE_FUTURES.remove(rpcResponse.getRequestId());
        if (null != future) {
            future.complete(rpcResponse);
        } else {
            throw new IllegalStateException();
        }
    }
}
