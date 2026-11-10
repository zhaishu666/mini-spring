package com.mini.spring;

import com.mini.demo.UserRepository;
import com.mini.demo.UserService;
import com.mini.orphan.OrphanService;
import com.mini.spring.bean.BeanFactory;
import com.mini.spring.bean.DefaultBeanFactory;
import com.mini.spring.core.BeanDefinition;
import com.mini.spring.core.BeanDefinitionReader;
import com.mini.spring.core.BeanDefinitionRegistry;
import com.mini.spring.exception.NoSuchBeanDefinitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("v0.2 依赖注入")
class ContainerV02InjectionTest {

    private BeanDefinitionRegistry registry;
    private BeanFactory factory;

    @BeforeEach
    void setup(){
        registry = new BeanDefinitionRegistry();
        BeanDefinitionReader.scanComponents("com.mini.demo").forEach(registry::register);
        factory = new DefaultBeanFactory(registry);
    }

    @Test
    @DisplayName("依赖注入测试,查看结果是否相同")
    public void autowired_field_is_injected(){
        assertEquals("hello user-7",factory.getBean(UserService.class).greet(7));
    }

    @Test
    @DisplayName("字段中注入的属性应与factory中getBean()相同")
    public void injected_dependency_is_the_singleton() throws ReflectiveOperationException {
        UserService service = factory.getBean(UserService.class);

        Field field = UserService.class.getDeclaredField("userRepository");
        field.setAccessible(true);
        Object injected = field.get(service);

        assertSame(factory.getBean(UserRepository.class), injected,
                "字段注入的依赖必须与容器中的单例是同一个对象");
    }

    @Test
    @DisplayName("无注解的Bean必须不受影响")
    public void bean_without_autowired_still_works(){
        assertSame(UserRepository.class, factory.getBean("userRepository").getClass());
    }

    @Test
    @DisplayName("有@Autowired但容器里面没有对应Bean 应该抛NoSuchBeanDefinitionException")
    public void autowired_field_without_bean_should_throw(){
        registry.register(new BeanDefinition(OrphanService.class,"orphanService"));

        NoSuchBeanDefinitionException ex = assertThrows(NoSuchBeanDefinitionException.class,
                () -> factory.getBean("orphanService"));

        assertTrue(ex.getMessage().contains("MissingDependency"),
                "异常消息应指出缺失的依赖类型，实际为: " + ex.getMessage());
    }
}
