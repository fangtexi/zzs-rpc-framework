package com.zzs;

import com.zzs.enums.RpcConfigEnum;
import com.zzs.utils.PropertiesFileUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Properties;

@SpringBootTest
class RpcServerApplicationTests {

    @Test
    void contextLoads() {
        Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
        String property = properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue());
        System.out.println(property);
    }

}
