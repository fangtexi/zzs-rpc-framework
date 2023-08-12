package com.zzs.handler;

import com.zzs.dto.RpcRequest;
import com.zzs.exception.RpcException;
import com.zzs.factory.SingletonFactory;
import com.zzs.provider.ServiceProvider;
import com.zzs.provider.impl.ZkServiceProviderImpl;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class RpcRequestHandler {
    private final ServiceProvider serviceProvider;

    public RpcRequestHandler() {
        serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
    }

    /**
     * Processing rpcRequest: call the corresponding method, and then return the method
     */
    public Object handle(RpcRequest rpcRequest) {
        Object service = serviceProvider.getService(rpcRequest.getRpcServiceName());
        return invokeTargetMethod(rpcRequest, service);
    }

    /**
     * get method execution results
     *
     * @param rpcRequest client request
     * @param service    service object
     * @return the result of the target method execution
     */
    private Object invokeTargetMethod(RpcRequest rpcRequest, Object service) {
        Object result;
        try {
            // 通过放射调用方法
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            result = method.invoke(service, rpcRequest.getParameters());
            log.info("service:[{}] successful invoke method:[{}]", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
            throw new RpcException(e.getMessage(), e);
        }
        return result;
    }
}
