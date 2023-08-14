package com.zzs.annotation;

import java.lang.annotation.*;

/**
 * 用于标识需要被注册的服务
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcService {
    /**
     * 服务的版本号，默认为空
     * @return
     */
    String version() default "";

    /**
     * 服务的组别，默认为空
     * @return
     */
    String group() default "";
}
