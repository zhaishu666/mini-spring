package com.mini.spring.core;

public class BeanDefinition {

    private final Class<?> type;
    private final String beanName;


    public BeanDefinition(Class<?> type, String beanName) {
        this.type = type;
        this.beanName = beanName;
    }

    /**
     * 获取
     * @return type
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * 获取
     * @return beanName
     */
    public String getBeanName() {
        return beanName;
    }

    public String toString() {
        return "BeanDefinition{type = " + type + ", beanName = " + beanName + "}";
    }
}
