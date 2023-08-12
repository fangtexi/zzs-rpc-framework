package com.zzs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RpcConfigEnum {
    RPC_CONFIG_PATH("rpc.properties"),
    ZK_ADDRESS("zookeeper.address");

    private final String propertyValue;
}
