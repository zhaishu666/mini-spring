package com.mini.cycle;

import com.mini.spring.annotation.Autowired;
import com.mini.spring.annotation.Component;

@Component
public class B {

    @Autowired
    private A a;

    public A getA() {
        return a;
    }
}
