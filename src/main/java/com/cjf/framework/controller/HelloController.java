package com.cjf.framework.controller;

import com.cjf.framework.annotation.*;
import com.cjf.framework.service.HelloService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @Descpription
 * @Author CJF
 * @Date 2019/5/26 10:03
 **/
@CJFController()
public class HelloController {

    @CJFAutowired()
    HelloService service;

    @CJFValue("xxx")
    String msg;

    @CJFValue
    Integer score;


    @CJFRequestMapping("/test")
    public void hello(HttpServletRequest request, HttpServletResponse response,
                      @CJFRequestParam("name") String name,
                      @CJFRequestParam("age")Integer age) throws IOException {
        String x = service.sendMsg("name["+name+"],age["+age+"],score["+score+"]msg["+msg+"].");
        response.getWriter().write(x);
    }


}
