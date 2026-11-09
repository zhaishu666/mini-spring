package com.mini.spring.bean;

@FunctionalInterface
public interface ObjectFactory<T> {

    T getObject();
}
