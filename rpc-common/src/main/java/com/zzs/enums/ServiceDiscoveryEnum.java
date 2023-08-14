package com.zzs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ServiceDiscoveryEnum {
    ZK("zk");

    private final String name;
}
