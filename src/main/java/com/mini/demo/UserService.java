package com.mini.demo;

import com.mini.spring.annotation.Autowired;
import com.mini.spring.annotation.Component;

@Component
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public String greet(long id){
        return "hello " + userRepository.findName(id);
    }
}
