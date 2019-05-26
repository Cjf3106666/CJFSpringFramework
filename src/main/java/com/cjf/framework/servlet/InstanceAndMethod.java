package com.cjf.framework.servlet;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descpription
 * @Author CJF
 * @Date 2019/5/26 13:54
 **/
public class InstanceAndMethod {

    private Object instance;
    private Method method;
    public List<Object> paramList=new ArrayList<>();

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }



    public InstanceAndMethod(Object instance, Method method) {
        this.instance = instance;
        this.method = method;
    }

    @Override
    public String toString() {
        return "InstanceAndMethod{" +
                "instance=" + instance +
                ", method=" + method +
                '}';
    }
}
