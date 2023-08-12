package com.zzs.loadbalance;

import com.zzs.dto.RpcRequest;

import java.util.List;

public abstract class AbstractLoadBalance implements LoadBalance{
    @Override
    public String selectServiceAddress(List<String> serviceAddresses, RpcRequest rpcRequest) {
        if (serviceAddresses == null || serviceAddresses.isEmpty()) {
            return null;
        }
        if (serviceAddresses.size() == 1) {
            return serviceAddresses.get(0);
        }
        return doSelect(serviceAddresses, rpcRequest);
    }

    /**
     * 一致性哈希算法
     * @param serviceAddresses 服务地址列表
     * @param rpcRequest
     * @return
     */
    protected abstract String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest);

}
