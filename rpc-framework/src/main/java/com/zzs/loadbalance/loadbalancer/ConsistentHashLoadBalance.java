package com.zzs.loadbalance.loadbalancer;

import com.zzs.dto.RpcRequest;
import com.zzs.loadbalance.AbstractLoadBalance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一致性哈希算法实现负载均衡，参考帧 Dubbo 中的实现：https://github.com/apache/dubbo/blob/2d9583adf26a2d8bd6fb646243a9fe80a77e65d5/dubbo-cluster/src/main/java/org/apache/dubbo/rpc/cluster/loadbalance/ConsistentHashLoadBalance.java
 * 参考链接：https://juejin.cn/post/6844904016577560583#heading-3
 */
public class ConsistentHashLoadBalance extends AbstractLoadBalance {
    private final ConcurrentHashMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();

    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        // 获取 serviceAddresses 的 hashCode
        int identityHashCode = System.identityHashCode(serviceAddresses);
        // build rpc service name by rpcRequest
        String rpcServiceName = rpcRequest.getRpcServiceName();
        ConsistentHashSelector selector = selectors.get(rpcServiceName);
        // 如果invokers是一个新的List对象，意味着服务提供者数量发生了变化，可能新增也可能减少了，此时selector.identityHashCode!=identityHashCode条件成立
        // 如果是第一次调用此时selector == null条件成立
        if (selector == null || selector.identityHashCode != identityHashCode) {
            selectors.put(rpcServiceName, new ConsistentHashSelector(serviceAddresses, 160, identityHashCode));
            selector = selectors.get(rpcServiceName);
        }
        return selector.select(rpcServiceName + Arrays.stream(rpcRequest.getParameters()));
    }

    static class ConsistentHashSelector {
        // 使用 TreeMap 存储 Invoker 的虚拟节点； hash 与 服务地址的映射
        private final TreeMap<Long, String> virtualInvokers;

        private final int identityHashCode;

        /**
         *
         * @param invokers 即服务地址列表
         * @param replicaNumber 虚拟节点数
         * @param identityHashCode 即服务地址列表的 hashCode
         */
        ConsistentHashSelector(List<String> invokers, int replicaNumber, int identityHashCode) {
            this.virtualInvokers = new TreeMap<>();
            this.identityHashCode = identityHashCode;

            // 遍历服务地址列表计算服务的虚拟节点，每个服务有 160 个虚拟节点
            for (String invoker : invokers) {
                for (int i = 0; i < replicaNumber / 4; i++) {
                    // 对 服务地址 + i 进行 md5 运算得到长度为 16 的字节数组
                    byte[] digest = md5(invoker + i);
                    // 对 digest 的部分字节进行 4 次 hash 计算得到四个不同整数，作为虚拟节点的 hash值
                    //h=0时，取digest中下标为0~3的4个字节进行位运算
                    //h=1时，取digest中下标为4~7的4个字节进行位运算
                    //h=2,h=3时过程同上
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        // 将节点与虚拟节点的映射关系存储到 TreeMap 中
                        virtualInvokers.put(m, invoker);
                    }
                }
            }
        }

        static byte[] md5(String key) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }

            return md.digest();
        }

        static long hash(byte[] digest, int idx) {
            return ((long) (digest[3 + idx * 4] & 255) << 24 | (long) (digest[2 + idx * 4] & 255) << 16 | (long) (digest[1 + idx * 4] & 255) << 8 | (long) (digest[idx * 4] & 255)) & 4294967295L;
        }

        public String select(String rpcServiceKey) {
            byte[] digest = md5(rpcServiceKey);
            return selectForKey(hash(digest, 0));
        }

        public String selectForKey(long hashCode) {
            //到TreeMap中查找第一个节点值大于或等于当前hash的Invoker
            Map.Entry<Long, String> entry = virtualInvokers.tailMap(hashCode, true).firstEntry();
            //到TreeMap中查找第一个节点值大于或等于当前hash的Invoker，需要将TreeMap的头节点赋值给entry
            if (entry == null) {
                entry = virtualInvokers.firstEntry();
            }

            return entry.getValue();
        }
    }
}
