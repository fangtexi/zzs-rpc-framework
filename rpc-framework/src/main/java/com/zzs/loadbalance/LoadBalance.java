package com.zzs.loadbalance;

import com.zzs.dto.RpcRequest;
import com.zzs.extension.SPI;

import java.util.List;

@SPI
public interface LoadBalance {
    /**
     * 从现有的服务列表中选择一个服务
     * @param serviceUrlList 服务地址列表
     * @param rpcRequest
     * @return 目标服务的地址
     */
    String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest);
}
