package com.cjf.framework.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface CJFValue {
    String value() default "";
}
