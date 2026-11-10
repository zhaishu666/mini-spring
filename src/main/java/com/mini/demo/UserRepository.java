package com.mini.demo;

import com.mini.spring.annotation.Component;

@Component
public class UserRepository {

    public String findName(long id){
        return "user-" + id;
    }
}
