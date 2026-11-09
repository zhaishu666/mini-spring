package com.mini.spring.bean;

/**
 * 容器门面。v0.1 只要求能按名字/类型取到 Bean。
 */
public interface BeanFactory {

    Object getBean(String beanName);

    <T> T getBean(Class<T> type);
}
