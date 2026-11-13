package com.mini.aop;

import com.mini.spring.annotation.Autowired;
import com.mini.spring.annotation.Component;
import com.mini.spring.annotation.Timed;

@Component
public class NodeImpl implements Node{

    @Autowired
    private Node next;  //自引用

    @Override
    public Node next() {
        return next;
    }

    @Timed
    @Override
    public String label() {
        return "";
    }
}
