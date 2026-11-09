package com.mini.spring.core;

import com.mini.spring.annotation.Component;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BeanDefinitionReader {

    public static List<BeanDefinition> scanComponents(String basePackage){
        List<BeanDefinition> beans = new ArrayList<>();

        Set<Class<?>> classes = ClassPathScanner.scan(basePackage);
        for(Class<?> clazz : classes){
            Component c = clazz.getAnnotation(Component.class);
            if(c == null) continue;
            //如果当前注解为空就decapitalize创建一个名字,否则直接用原本的名字,这里因为初始化为"",所以不能用null进行比较
            String name = c.value().isEmpty() ? Introspector.decapitalize(clazz.getSimpleName()) : c.value();
            beans.add(new BeanDefinition(clazz, name));
        }

        return beans;
    }
}
