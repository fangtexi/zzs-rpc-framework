package com.zzs.config;

import lombok.*;

/**
 * @author zzs
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RpcServiceConfig {
    /**
     * service version
     */
    private String version = "";
    /**
     * when the interface has multiple implementation classes, distinguish by group
     */
    private String group = "";

    /**
     * target service
     */
    private Object service;

    public String getRpcServiceName() {
        // 服务的全限定名称 + 组名 + 版本
        return this.getServiceName() + this.getGroup() + this.getVersion();
    }

    public String getServiceName() {
        // 返回服务的全限定名称
        return this.service.getClass().getInterfaces()[0].getCanonicalName();
    }
}
