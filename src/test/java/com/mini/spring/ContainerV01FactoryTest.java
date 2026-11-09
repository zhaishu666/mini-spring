package com.mini.spring;

import com.mini.broken.BrokenService;
import com.mini.demo.UserRepository;
import com.mini.demo.UserService;
import com.mini.spring.bean.BeanFactory;
import com.mini.spring.bean.DefaultBeanFactory;
import com.mini.spring.core.BeanDefinition;
import com.mini.spring.core.BeanDefinitionReader;
import com.mini.spring.core.BeanDefinitionRegistry;
import com.mini.spring.exception.BeanCreationException;
import com.mini.spring.exception.NoSuchBeanDefinitionException;
import com.mini.spring.exception.NoUniqueBeanDefinitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("v0.1 容器：实例化与缓存")
class ContainerV01FactoryTest {

    private BeanDefinitionRegistry registry;
    private BeanFactory factory;

    @BeforeEach
    void setUp() {
        registry = new BeanDefinitionRegistry();
        BeanDefinitionReader.scanComponents("com.mini.demo").forEach(registry::register);
        factory = new DefaultBeanFactory(registry);
    }

    @Test
    @DisplayName("按名字取：类型正确，且两次取到同一对象（单例缓存命中）")
    void get_bean_by_name_returns_singleton() {
        Object first = factory.getBean("userService");
        assertNotNull(first);
        assertSame(UserService.class, first.getClass());

        Object second = factory.getBean("userService");
        assertSame(first, second, "同一 beanName 两次 getBean 必须返回同一实例");
    }

    @Test
    @DisplayName("按类型取：免强转，且与按名字取到的是同一对象")
    void get_bean_by_type_returns_same_instance() {
        UserService byType = factory.getBean(UserService.class);
        Object byName = factory.getBean("userService");

        assertSame(byType.getClass(), UserService.class);
        assertSame(byName, byType, "按类型与按名字必须命中同一个单例");
    }

    @Test
    @DisplayName("按类型取 Repository 同样能命中")
    void get_bean_by_type_resolves_repository() {
        UserRepository repository = factory.getBean(UserRepository.class);

        assertSame(UserRepository.class, repository.getClass());
    }

    @Test
    @DisplayName("名字不存在：抛 NoSuchBeanDefinitionException")
    void unknown_name_should_throw() {
        assertThrows(NoSuchBeanDefinitionException.class,
                () -> factory.getBean("notExist"));
    }

    @Test
    @DisplayName("类型不存在：抛 NoSuchBeanDefinitionException")
    void unknown_type_should_throw() {
        assertThrows(NoSuchBeanDefinitionException.class,
                () -> factory.getBean(Runnable.class));
    }

    @Test
    @DisplayName("一个类型匹配多个 Bean：抛 NoUniqueBeanDefinitionException")
    void duplicate_type_should_throw_no_unique() {
        registry.register(new BeanDefinition(UserService.class, "userServiceA"));
        registry.register(new BeanDefinition(UserService.class, "userServiceB"));

        assertThrows(NoUniqueBeanDefinitionException.class,
                () -> factory.getBean(UserService.class));
    }

    @Test
    @DisplayName("Bean 创建失败：包装成 BeanCreationException 并保留根因")
    void bean_creation_failure_should_keep_cause() {
        registry.register(new BeanDefinition(BrokenService.class, "brokenService"));

        BeanCreationException ex = assertThrows(BeanCreationException.class,
                () -> factory.getBean("brokenService"));

        assertTrue(ex.getMessage().contains("brokenService"), "异常消息必须带 beanName");
        assertNotNull(ex.getCause(), "必须保留根因，否则看不到构造器里真正抛了什么");
        assertTrue(ex.getCause() instanceof InvocationTargetException,
                "根因应是反射调用包装的 InvocationTargetException");
    }
}
