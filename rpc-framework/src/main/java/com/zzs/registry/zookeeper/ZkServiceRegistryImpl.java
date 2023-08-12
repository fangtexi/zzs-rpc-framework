package com.zzs.registry.zookeeper;

import com.zzs.registry.ServiceRegistry;
import com.zzs.utils.CuratorUtils;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;

public class ZkServiceRegistryImpl implements ServiceRegistry {
    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        String servicePath = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString();
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        // 持久节点
        CuratorUtils.createPersistentNode(zkClient, servicePath);
    }
}
