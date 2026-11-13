package com.mini.spring;

import com.mini.aop.GreetingService;
import com.mini.aop.Node;
import com.mini.spring.bean.BeanFactory;
import com.mini.spring.bean.DefaultBeanFactory;
import com.mini.spring.core.BeanDefinitionReader;
import com.mini.spring.core.BeanDefinitionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerV05AopTest {

    private BeanFactory factory;

    @BeforeEach
    void setup(){
        BeanDefinitionRegistry registry = new BeanDefinitionRegistry();
        for(String pkg : List.of("com.mini.demo","com.mini.aop")){
            BeanDefinitionReader.scanComponents(pkg).forEach(registry::register);
        }
        factory = new DefaultBeanFactory(registry);
    }

    @Test
    @DisplayName("容器返回的GreetingService是JDK代理")
    void bean_is_proxy(){
        GreetingService svc = factory.getBean(GreetingService.class);

        assertTrue(Proxy.isProxyClass(svc.getClass()));
        assertInstanceOf(GreetingService.class, svc);  //代理仍满足接口契约
    }

    @Test
    @DisplayName("代理不能改变原本的语义")
    void proxied_method_semantics_unchanged(){
        String s = factory.getBean(GreetingService.class).greet("bob");
        assertEquals("hi bob", s);
    }

    @Test
    @DisplayName("没有@Timed注解的方法不生成代理")
    void no_timed_method_no_proxy(){
        Object bean = factory.getBean("userService");
        assertFalse(Proxy.isProxyClass(bean.getClass()));
    }

    @Test
    @DisplayName("循环依赖时代理保持一致")
    void proxy_consistent_with_self_reference(){
        Node node = factory.getBean(Node.class);

        assertTrue(Proxy.isProxyClass(node.getClass()));
        assertSame(node, node.next());
    }

    @Test
    @DisplayName("业务异常穿透代理 异常不被包装")
    void business_exception_passes_through_proxy(){
        GreetingService svc = factory.getBean(GreetingService.class);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, svc::boom);
        assertEquals("boom", ex.getMessage());
    }
}
