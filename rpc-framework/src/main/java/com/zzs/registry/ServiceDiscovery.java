package com.zzs.registry;

import com.zzs.dto.RpcRequest;
import com.zzs.extension.SPI;

import java.net.InetSocketAddress;
@SPI
public interface ServiceDiscovery {
    /**
     * 查找服务
     * @param rpcRequest
     * @return 服务地址
     */
    InetSocketAddress lookupService(RpcRequest rpcRequest);
}
