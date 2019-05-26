package com.cjf.framework.servlet;

import com.cjf.framework.annotation.*;
import com.cjf.framework.controller.HelloController;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.text.StyledEditorKit;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;

/**
 * @Descpription
 * @Author CJF
 * @Date 2019/5/26 9:45
 **/
public class CJFServlet extends HttpServlet {
    private static Logger logger = Logger.getLogger(CJFServlet.class);
    private Properties properties = new Properties();
    private List<String> classList = new ArrayList<>();
    public Map<String, Object> iocMap = new HashMap<>(16);
    public Map<String, InstanceAndMethod> handlerMapping = new HashMap<>(16);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);

    }

    /**
     * 反射处理用户请求
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("content-type", "text/html;charset=utf-8");
        if (handlerMapping.isEmpty()) {
            resp.getWriter().write("404 Sorry, the resources you visited do not exist!");
            return;
        }
        InstanceAndMethod iam = handlerMapping.get(req.getRequestURI());
        if (iam == null) {
            resp.getWriter().write("404 Sorry, the resources you visited do not exist!");
            return;
        } else {
            try {
                Object[] args = new Object[iam.paramList.size()];
                for (int i = 0; i < iam.paramList.size(); i++) {
                    boolean flag = false;
                    try {
                        if (((Class) (iam.paramList.get(i))).getSimpleName().equals("HttpServletRequest")) {
                            args[i] = req;
                            flag = true;
                        } else if (((Class) (iam.paramList.get(i))).getSimpleName().equals("HttpServletResponse")) {
                            args[i] = resp;
                            flag = true;
                        }
                    } catch (Exception e) {
                    }
                    if (!flag) {
                        Object parameter = req.getParameter(String.valueOf(iam.paramList.get(i)));
                        parameter = castObjet(iam.getMethod().getParameters()[i].getType(), parameter);
                        args[i] = parameter;
                    }
                }
                iam.getMethod().invoke(iam.getInstance(), args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Object castObjet(Class<?> aim, Object obj) {
        if (aim == null || obj == null) {
            return obj;
        }
        try {
            if (aim.equals(Integer.class)) {
                obj = Integer.valueOf(obj.toString());
            } else if (aim.equals(String.class)) {
                obj = String.valueOf(obj);
            } else if (aim.equals(Boolean.class)) {
                obj = Boolean.valueOf(obj.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.service(req, resp);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        logger.info("===========开始进行初始化==========");
        logger.info("===========1.加载配置文件==========");
        //1.加载配置文件
        loadConfig(config.getInitParameter("applicationConfig"));
        //2.根据配置文件扫描所有相关的参数
        logger.info("===========2.扫描相关的类==========");
        doScanPackage(properties.getProperty("ScanPackages"));
        //3.初始化所有相关的类，放入IOC容器中
        logger.info("===========3.实例化扫描的类========");
        doInstance();
        //4.实现依赖注入
        logger.info("===========4.进行依赖注入==========");
        doDI();
        //5.初始化HandlerMapping
        logger.info("===========5.初始化Mapping==========");
        initMapping();
        logger.info("====== 准备工作完成 等待请求 =======");
        //6.处理用户请求、doPost/doGet
    }

    private void initMapping() {
        if (iocMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            String url = "/";
            if (entry.getValue().getClass().isAnnotationPresent(CJFRequestMapping.class)) {
                url += entry.getValue().getClass().getAnnotation(CJFRequestMapping.class).value() + "/";
            }
            Method[] declaredMethods = entry.getValue().getClass().getDeclaredMethods();
            for (Method method : declaredMethods
                    ) {
                method.setAccessible(true);
                if (method.isAnnotationPresent(CJFRequestMapping.class)) {
                    url += method.getAnnotation(CJFRequestMapping.class).value();
                    //  handlerMapping.put(url.replaceAll("/+","/"),new InstanceAndMethod(entry.getValue(),method));
                    InstanceAndMethod iam = new InstanceAndMethod(entry.getValue(), method);
                    Parameter[] parameters = method.getParameters();
                    for (Parameter param : parameters
                            ) {
                        if (param.isAnnotationPresent(CJFRequestParam.class)) {
                            CJFRequestParam annotation = param.getAnnotation(CJFRequestParam.class);
                            if (!"".equals(annotation.value())) {
                                iam.paramList.add(annotation.value());
                            } else {
                                iam.paramList.add(param.getName());
                            }
                        } else {
                            iam.paramList.add(param.getType());
                        }
                    }
                    handlerMapping.put(url.replaceAll("/+", "/"), iam);
                }
            }

        }
    }

    private void doDI() {
        if (iocMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(CJFAutowired.class)) {
                    CJFAutowired annotation = field.getAnnotation(CJFAutowired.class);
                    if (!"".equals(annotation.value())) {
                        try {
                            field.set(entry.getValue(), iocMap.get(annotation.value()));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            Object value = iocMap.get(firstLowercas(field.getType().getSimpleName()));
                            field.set(entry.getValue(), value);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (field.isAnnotationPresent(CJFValue.class)) {
                    CJFValue annotation = field.getAnnotation(CJFValue.class);
                    if (!"".equals(annotation.value())) {
                        try {
                            Object obj = properties.get(annotation.value());
                            obj = castObjet(field.getType(), obj);
                            field.set(entry.getValue(), obj);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            Object obj = properties.get(firstLowercas(field.getName()));
                            obj = castObjet(field.getType(), obj);
                            field.set(entry.getValue(), obj);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }

    private String firstLowercas(String str) {
        char[] chars = str.toCharArray();
        chars[0] = String.valueOf(chars[0]).toLowerCase().toCharArray()[0];
        return String.valueOf(chars);

    }

    private void doInstance() {
        if (classList.isEmpty()) {
            return;
        }
        for (String className : classList
                ) {
            try {
                Class<?> clazz = this.getClass().getClassLoader().loadClass(className);
                if (clazz.isAnnotationPresent(CJFController.class)) {
                    if (clazz.isInterface()) {
                        continue;
                    }
                    Object o = clazz.newInstance();
                    CJFController annotation = clazz.getAnnotation(CJFController.class);
                    if ("".equals(annotation.value())) {
                        putIOC(clazz.getSimpleName(), o, true);
                    } else {
                        putIOC(annotation.value(), o, false);
                    }
                }
                if (clazz.isAnnotationPresent(CJFComponent.class)) {
                    if (clazz.isInterface()) {
                        continue;
                    }
                    Object o = clazz.newInstance();
                    CJFComponent annotation = clazz.getAnnotation(CJFComponent.class);
                    if ("".equals(annotation.value())) {
                        putIOC(clazz.getSimpleName(), o, true);
                    } else {
                        putIOC(annotation.value(), o, false);
                    }
                }
                if (clazz.isAnnotationPresent(CJFConfiguration.class)) {
                    if (clazz.isInterface()) {
                        continue;
                    }
                    Object o = clazz.newInstance();
                    CJFConfiguration annotation = clazz.getAnnotation(CJFConfiguration.class);
                    if ("".equals(annotation.value())) {
                        putIOC(clazz.getSimpleName(), o, true);
                    } else {
                        putIOC(annotation.value(), o, false);
                    }
                }
                if (clazz.isAnnotationPresent(CJFService.class)) {
                    if (clazz.isInterface()) {
                        continue;
                    }
                    Object o = clazz.newInstance();
                    for (Class<?> clazz2 :
                            clazz.getInterfaces()) {
                        putIOC(clazz2.getSimpleName(), o, true);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    /**
     * Spring Bean命名规则 类名首字母小写
     */
    private void putIOC(String name, Object instance, boolean orChange) {
        if (orChange) {
            char[] chars = name.toCharArray();
            chars[0] = String.valueOf(chars[0]).toLowerCase().toCharArray()[0];
            iocMap.put(String.valueOf(chars), instance);
        } else {
            iocMap.put(name, instance);
        }
    }

    private void doScanPackage(String packageName) {
        URL url = this.getClass().getResource("/" + packageName.replaceAll("\\.", "/"));
        File fileDir = new File(url.getFile());
        for (File file : fileDir.listFiles()
                ) {
            if (file.isDirectory()) {
                doScanPackage(packageName + "." + file.getName());
            } else {
                classList.add(packageName + "." + file.getName().replaceAll(".class", ""));
            }
        }
    }

    private void loadConfig(String pathName) {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(pathName);
        try {
            properties.load(resourceAsStream);
            resourceAsStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
