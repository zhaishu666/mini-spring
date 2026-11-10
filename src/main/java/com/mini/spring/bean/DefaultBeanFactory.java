package com.mini.spring.bean;

import com.mini.spring.annotation.Autowired;
import com.mini.spring.core.BeanDefinition;
import com.mini.spring.core.BeanDefinitionRegistry;
import com.mini.spring.exception.BeanCreationException;
import com.mini.spring.exception.NoSuchBeanDefinitionException;
import com.mini.spring.exception.NoUniqueBeanDefinitionException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultBeanFactory implements BeanFactory{

    private final BeanDefinitionRegistry registry;
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();  //一级缓存

    /*private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();  //二级缓存
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>(); //三级缓存
    private final Set<String> singletonsCurrentlyInCreation = ConcurrentHashMap.newKeySet();*/

    public DefaultBeanFactory(BeanDefinitionRegistry registry) {
        this.registry = Objects.requireNonNull(registry,"registry不能为null");
    }

    @Override
    public Object getBean(String name){
        Object singleton = singletonObjects.get(name);

        if(singleton != null){
            return singleton;
        }
        return createBean(name);
    }

    @Override
    public <T> T getBean(Class<T> type) {
        List<BeanDefinition> beanDefinitionList = registry.getBeanDefinitions();
        List<String> beanNames = new ArrayList<>();
        for (BeanDefinition bd : beanDefinitionList) {
            if (type.isAssignableFrom(bd.getType())) {  //判断type能否接收当前遍历到的bd的type
                beanNames.add(bd.getBeanName());
            }
        }
        int beanCount = beanNames.size();
        if(beanCount == 0){
            throw new NoSuchBeanDefinitionException("未查找到这个bean: " + type.getName());
        } else if (beanCount >= 2) {
            throw new NoUniqueBeanDefinitionException("一个类型匹配到多个bean:" + beanNames.get(0));  //查找阶段出错
        }
        Object bean = getBean(beanNames.get(0));
        return (T) bean;
    }

    private Object createBean(String name){
        BeanDefinition bd = registry.get(name);
        if(bd == null) throw new NoSuchBeanDefinitionException("未找到对应的beanDefinition");
        Object instance;
        try {
            instance = bd.getType().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) { //直接抛出父异常
            throw new BeanCreationException("bean初始化异常:" + name, e);
        }

        //注册三级缓存工厂(v0.3),必须在populateBean()之前完成

        populateBean(instance);
        singletonObjects.put(name, instance);
        return instance;
    }

    private void populateBean(Object instance){
        Field[] declaredFields = instance.getClass().getDeclaredFields();  //获取所有声明的字段,包括私有
        for(Field field : declaredFields){
            if(field.getAnnotation(Autowired.class) == null) continue; //跳过未加注解的
            if(Modifier.isStatic(field.getModifiers())) continue;  //跳过static静态字段

            field.setAccessible(true);
            Object dependency = getBean(field.getType());  //可能会出现循环依赖
            try {
                field.set(instance, dependency); //将依赖对象交给字段
            } catch (IllegalAccessException e) {
                throw new BeanCreationException("字段无法注入: " + instance.getClass().getName() + "#" + field.getName(), e);
            }
        }
    }
}
