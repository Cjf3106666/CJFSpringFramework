package com.cjf.framework.service.imp;

import com.cjf.framework.annotation.CJFService;
import com.cjf.framework.service.HelloService;

/**
 * @Descpription
 * @Author CJF
 * @Date 2019/5/26 10:07
 **/
@CJFService
public class HelloServiceImp implements HelloService {
    public String sendMsg(String msg) {
        return "msg is "+ msg;
    }
}
