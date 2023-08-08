package com.zzs.dto;

import lombok.*;

/**
 * @author zzs
 */
@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RpcMessage {
    /**
     * rpc message type
     */
    private byte messageType;
    /**
     * serialization type
     */
    private byte codec;
    /**
     * compress type
     */
    private byte compress;
    /**
     * request id
     */
    private int requestId;
    /**
     * request data
     */
    private Object data;
}
