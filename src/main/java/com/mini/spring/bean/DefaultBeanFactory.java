package com.mini.spring.bean;

import com.mini.spring.core.BeanDefinition;
import com.mini.spring.core.BeanDefinitionRegistry;
import com.mini.spring.exception.BeanCreationException;
import com.mini.spring.exception.NoSuchBeanDefinitionException;
import com.mini.spring.exception.NoUniqueBeanDefinitionException;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultBeanFactory implements BeanFactory{

    private final BeanDefinitionRegistry registry;
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();  //一级缓存

    /*private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();  //二级缓存
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>();
    private final Set<String> singletonsCurrentlyInCreation = ConcurrentHashMap.newKeySet();*/

    public DefaultBeanFactory(BeanDefinitionRegistry registry) {
        this.registry = Objects.requireNonNull(registry,"registry不能为null");
    }

    @Override
    public Object getBean(String name){
        Object singleton = singletonObjects.get(name);
        Object bean = null;

        if(singleton != null){
            return singleton;
        }

        try {
            BeanDefinition bd = registry.get(name);
            if(bd == null) throw new NoSuchBeanDefinitionException("未找到对应的bean");
            bean = bd.getType().getDeclaredConstructor().newInstance();
            singletonObjects.put(name, bean);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new BeanCreationException("bean初始化异常:" + name, e);
        }
        return bean;
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
}
