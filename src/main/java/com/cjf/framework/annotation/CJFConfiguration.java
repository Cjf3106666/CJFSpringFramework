package com.cjf.framework.annotation;

import java.lang.annotation.*;

/**
 * @Descpription
 * @Author CJF
 * @Date 2019/5/26 12:21
 **/
@Target(ElementType.TYPE)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface CJFConfiguration {
    String value() default "";
}
