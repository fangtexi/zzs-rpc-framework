package com.zzs.spring;

import com.zzs.annotation.RpcScan;
import com.zzs.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.stereotype.Component;
@Slf4j
public class CustomScannerRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    private ResourceLoader resourceLoader;
    private static final String BASE_PACKAGE_ATTRIBUTE_NAME = "basePackage";
    private static final String SPRING_BEAN_BASE_PACKAGE = "com.zzs";

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {
        // 获取自定义包扫描注解的所有属性信息
        // RpcScan.class.getName() 获取注解的全限定类名
        AnnotationAttributes rpcScanAnnotationAttributes = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(RpcScan.class.getName()));
        // 获取 RpcScan 注解中 basePackage 属性的值
        String[] rpcScanBasePackages = new String[0];
        if (rpcScanAnnotationAttributes != null) {
            rpcScanBasePackages = rpcScanAnnotationAttributes.getStringArray(BASE_PACKAGE_ATTRIBUTE_NAME);
        }
        // 如果没有为 RpcScan 的 basePackage 属性赋值，则以当前类的包名作为 rpcScanBasePackages 的值
        if(rpcScanBasePackages.length == 0) {
            rpcScanBasePackages = new String[]{((StandardAnnotationMetadata) annotationMetadata).getIntrospectedClass().getPackage().getName()};
        }
        // 创建扫描指定注解的包扫描器
        CustomScanner rpcServiceScanner = new CustomScanner(registry, RpcService.class);
        CustomScanner springBeanScanner = new CustomScanner(registry, Component.class);
        if (resourceLoader != null) {
            rpcServiceScanner.setResourceLoader(resourceLoader);
            springBeanScanner.setResourceLoader(resourceLoader);
        }
        // 扫描注解
        int springBeanAmount = springBeanScanner.scan(SPRING_BEAN_BASE_PACKAGE);
        log.info("springBeanScanner扫描的数量 [{}]", springBeanAmount);
        int rpcServiceCount = rpcServiceScanner.scan(rpcScanBasePackages);
        log.info("rpcServiceScanner扫描的数量 [{}]", rpcServiceCount);

    }
}