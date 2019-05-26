package com.cjf.framework.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface CJFController {
    String value() default "";
}
