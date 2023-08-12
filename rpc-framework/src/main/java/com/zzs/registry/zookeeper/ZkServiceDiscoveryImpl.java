package com.zzs.registry.zookeeper;

import com.zzs.dto.RpcRequest;
import com.zzs.enums.LoadBalanceEnum;
import com.zzs.enums.RpcErrorMessageEnum;
import com.zzs.exception.RpcException;
import com.zzs.extension.ExtensionLoader;
import com.zzs.loadbalance.LoadBalance;
import com.zzs.registry.ServiceDiscovery;
import com.zzs.utils.CuratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 基于 zookeeper 的服务发现
 *
 * @author zzs
 */
@Slf4j
public class ZkServiceDiscoveryImpl implements ServiceDiscovery {
    private final LoadBalance loadBalance;

    public ZkServiceDiscoveryImpl() {
        this.loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(LoadBalanceEnum.LOADBALANCE.getName());
    }

    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        // 获取调用的服务名称
        String rpcServiceName = rpcRequest.getRpcServiceName();
        // 获取该服务对应的所有服务节点
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        List<String> serviceUrlList = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
        if (serviceUrlList == null || serviceUrlList.isEmpty()) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
        }
        // 通过负载均衡选取服务节点
        String targetServiceUrl = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        log.info("Successfully found the service address:[{}]", targetServiceUrl);
        String[] socketAddressArray = targetServiceUrl.split(":");
        String host = socketAddressArray[0];
        int port = Integer.parseInt(socketAddressArray[1]);
        return new InetSocketAddress(host,port);
    }
}
