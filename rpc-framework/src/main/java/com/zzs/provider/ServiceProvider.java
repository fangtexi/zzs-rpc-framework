package com.zzs.provider;

import com.zzs.config.RpcServiceConfig;

public interface ServiceProvider {
    /**
     * 添加服务为
     * @param rpcServiceConfig rpc service related attributes
     */
    void addService(RpcServiceConfig rpcServiceConfig);

    /**
     * 获取服务
     * @param rpcServiceName rpc service name
     * @return service object
     */
    Object getService(String rpcServiceName);

    /**
     * 发布服务到注册中心
     * @param rpcServiceConfig rpc service related attributes
     */
    void publishService(RpcServiceConfig rpcServiceConfig);
}
