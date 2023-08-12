package com.zzs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoadBalanceEnum {
    LOADBALANCE("loadBalance");

    private final String name;
}
