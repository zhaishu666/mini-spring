package com.mini.spring.core;

import com.mini.spring.exception.ConflictingBeanDefinitionException;

import java.util.*;

public class BeanDefinitionRegistry {

    private final Map<String, BeanDefinition> registeredBeans = new LinkedHashMap<>();

    public void register(BeanDefinition bd){
        if(registeredBeans.containsKey(bd.getBeanName())){
            throw new ConflictingBeanDefinitionException("重复bean命名冲突:" + bd.getBeanName());
        }
        registeredBeans.put(bd.getBeanName(), bd);
    }

    public BeanDefinition get(String beanName){
        return registeredBeans.get(beanName);
    }

    public List<BeanDefinition> getBeanDefinitions(){

        return new ArrayList<>(registeredBeans.values());
    }

}
