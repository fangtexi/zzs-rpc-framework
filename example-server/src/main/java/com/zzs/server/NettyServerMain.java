package com.zzs.server;


import com.zzs.annotation.RpcScan;
import com.zzs.netty.server.NettyRpcServer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@RpcScan(basePackage = {"com.zzs"})
public class NettyServerMain {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(NettyServerMain.class);
        NettyRpcServer nettyRpcServer = (NettyRpcServer) applicationContext.getBean("nettyRpcServer");
        nettyRpcServer.start();
    }
}
