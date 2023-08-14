package com.zzs.dto;

import lombok.*;

@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RpcRequest {
    private static final long serialVersionUID = 1905122041950251207L;
    private String requestId;
    private String interfaceName;
    private String methodName;
    private Object[] parameters;
    private Class<?>[] paramTypes;
    private String version;
    // 用于处理一个接口有多个实现类的情况
    private String group;

    public String getRpcServiceName() {
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }
}
