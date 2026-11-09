package com.mini.spring.exception;

public class ConflictingBeanDefinitionException extends RuntimeException {
    public ConflictingBeanDefinitionException(String message) {
        super(message);
    }
}
