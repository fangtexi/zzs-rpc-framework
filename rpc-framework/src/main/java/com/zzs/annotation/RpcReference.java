package com.zzs.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcReference {
    /**
     * Service version, default value is empty string
     */
    String version() default "";

    /**
     * Service group, default value is empty string
     */
    String group() default "";
}
