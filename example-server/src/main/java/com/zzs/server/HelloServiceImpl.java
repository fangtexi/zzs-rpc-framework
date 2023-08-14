package com.zzs.server;

import com.zzs.Hello;
import com.zzs.HelloService;
import com.zzs.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RpcService(version = "1",group = "HelloServiceImpl")
public class HelloServiceImpl implements HelloService {

    static {
        System.out.println("HelloServiceImpl被创建");
    }
    @Override
    public String hello(Hello hello) {
        log.info("HelloServiceImpl收到: {}.", hello.getMessage());
        String result = "Hello description is " + hello.getDescription();
        log.info("HelloServiceImpl返回: {}.", result);
        return result;    }
}
