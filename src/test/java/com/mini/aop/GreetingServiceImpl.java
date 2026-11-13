package com.mini.aop;

import com.mini.spring.annotation.Component;
import com.mini.spring.annotation.Timed;

@Component
public class GreetingServiceImpl implements GreetingService{

    @Timed
    @Override
    public String greet(String name) {
        return "hi " + name;
    }
}
