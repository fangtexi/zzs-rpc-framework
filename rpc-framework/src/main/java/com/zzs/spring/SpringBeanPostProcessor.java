package com.zzs.spring;

import com.zzs.annotation.RpcReference;
import com.zzs.annotation.RpcService;
import com.zzs.config.RpcServiceConfig;
import com.zzs.factory.SingletonFactory;
import com.zzs.netty.client.NettyRpcClient;
import com.zzs.provider.ServiceProvider;
import com.zzs.provider.impl.ZkServiceProviderImpl;
import com.zzs.proxy.RpcClientProxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Slf4j
@Component
public class SpringBeanPostProcessor implements BeanPostProcessor {
    private final ServiceProvider serviceProvider;
    private final NettyRpcClient nettyRpcClient;

    public SpringBeanPostProcessor() {
        this.serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
        this.nettyRpcClient = new NettyRpcClient();
    }

    /**
     * 在 bean 初始化之前，把被 @RpcService 注解修饰的 bean 进行注册
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean.getClass().isAnnotationPresent(RpcService.class)) {
            log.info("[{}] is annotated with  [{}]", bean.getClass().getName(), RpcService.class.getCanonicalName());
            RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);
            RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                    .group(rpcService.group())
                    .version(rpcService.version())
                    .service(bean)
                    .build();
            // 发布服务到注册中心
            serviceProvider.publishService(rpcServiceConfig);
        }
        return bean;
    }

    /**
     * bean 初始化完成后为被 @RpcReference 注解修饰的属性设置代理对象
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Field[] declaredFields = bean.getClass().getDeclaredFields();
        for (Field field:declaredFields) {
            RpcReference rpcReference = field.getAnnotation(RpcReference.class);
            if (rpcReference != null) {
                RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                        .version(rpcReference.version())
                        .group(rpcReference.group())
                        .build();
                RpcClientProxy rpcClientProxy = new RpcClientProxy(nettyRpcClient, rpcServiceConfig);
                Object proxy = rpcClientProxy.getProxy(field.getType());
                field.setAccessible(true);
                try {
                    // 为 bean 对象设置代理对象，后续再使用 bean 对象调用方法时就会使用代理 对象进行调用
                    field.set(bean, proxy);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return bean;
    }
}
