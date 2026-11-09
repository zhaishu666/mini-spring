package com.mini.broken;

/**
 * 故意在构造器里抛异常，用于验证容器创建失败时的异常包装。
 * 不在 com.mini.demo 下，避免影响扫描测试的数量断言。
 */
public class BrokenService {

    public BrokenService() {
        throw new IllegalStateException("故意在构造器里抛异常");
    }
}
