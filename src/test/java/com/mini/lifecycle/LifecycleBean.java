package com.mini.lifecycle;

import com.mini.spring.annotation.Component;
import com.mini.spring.annotation.PostConstruct;

@Component
public class LifecycleBean {

    private boolean initialized = false;

    @PostConstruct
    private void init(){
        initialized = true;
    }

    public boolean isInitialized(){
        return initialized;
    }
}
