package com.mini.spring.core;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class ClassPathScanner {

    public static Set<Class<?>> scan(String basePackage){

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader(); //获得当前线程的上下文类加载器
        String path = basePackage.replace(".", "/");
        Set<Class<?>> classSet = new LinkedHashSet<>();  //去重,防止target/classes和target/test-classes各有一份类时返回重复的Class对象

        Enumeration<URL> resources = null;
        try {
            resources = classLoader.getResources(path);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if(!"file".equals(url.getProtocol())){
                    throw new UnsupportedOperationException("暂不支持非file协议(jar):" + url);
                }
                File dir = new File(url.toURI());
                collect(dir, basePackage, classSet, classLoader);
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("扫描失败: " + basePackage, e);
        }
        return classSet;
    }

    //用于递归处理路径和文件,并放入classSet中
    private static void collect(File dir, String pkg, Set<Class<?>> out, ClassLoader cl){
        File[] files = dir.listFiles();
        if(files == null) return;
        for(File f : files){
            if(f.isDirectory()){
                collect(f, pkg + "." + f.getName(), out, cl);  //递归扫当前包及其子包
            }else if(f.getName().endsWith(".class")){  //只处理类,其它接口或者jar包不做处理
                String name = pkg + "." + f.getName().replace(".class","");
                try{
                    Class<?> clazz = Class.forName(name, false, cl);
                    if(!clazz.isInterface() && !clazz.isAnnotation() && !clazz.isEnum()
                            && !clazz.isAnonymousClass() && !clazz.isSynthetic()){
                        out.add(clazz);
                    }
                }catch (ClassNotFoundException | LinkageError e){
                    System.err.println("跳过" + name + ":" + e);
                }
            }
        }
    }
}
