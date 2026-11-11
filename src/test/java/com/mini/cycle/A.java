package com.mini.cycle;

import com.mini.spring.annotation.Autowired;
import com.mini.spring.annotation.Component;

@Component
public class A {

    @Autowired
    private B b;

    public B getB() {
        return b;
    }
}
