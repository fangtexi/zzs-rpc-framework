package com.zzs.annotation;

import com.zzs.spring.CustomScannerRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 自定义包扫描注解
 */
@Documented
@Import(CustomScannerRegistrar.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RpcScan {
    String[] basePackage();
}
