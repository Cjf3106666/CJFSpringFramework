package com.cjf.framework.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE,ElementType.METHOD})
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface CJFRequestMapping {
    String value() default "";

}
