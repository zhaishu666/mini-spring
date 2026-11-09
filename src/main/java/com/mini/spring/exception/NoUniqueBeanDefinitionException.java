package com.mini.spring.exception;

public class NoUniqueBeanDefinitionException extends RuntimeException {
    public NoUniqueBeanDefinitionException(String message) {
        super(message);
    }
}
