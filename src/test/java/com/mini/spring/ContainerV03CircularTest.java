package com.mini.spring;

import com.mini.cycle.A;
import com.mini.cycle.B;
import com.mini.spring.bean.BeanFactory;
import com.mini.spring.bean.DefaultBeanFactory;
import com.mini.spring.core.BeanDefinitionReader;
import com.mini.spring.core.BeanDefinitionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("v0.3 三级缓存解决循环依赖")
class ContainerV03CircularTest {

    private BeanFactory factory;

    @BeforeEach
    void setUp() {
        BeanDefinitionRegistry registry = new BeanDefinitionRegistry();
        BeanDefinitionReader.scanComponents("com.mini.cycle").forEach(registry::register);
        factory = new DefaultBeanFactory(registry);
    }

    @Test
    @DisplayName("循环依赖：双向引用都指向容器中的同一实例")
    void circular_dependency_is_resolved() {
        A a = factory.getBean(A.class);
        B b = factory.getBean(B.class);

        assertSame(b, a.getB(), "A 里的 B 应是容器中的那个 B");
        assertSame(a, b.getA(), "B 里的 A 应是容器中的那个 A");
        assertSame(a, a.getB().getA(), "a.getB().getA() 应等于 a");
    }

    @Test
    @DisplayName("解完循环后双方仍是单例")
    void both_are_singletons() {
        assertSame(factory.getBean(A.class), factory.getBean(A.class));
        assertSame(factory.getBean(B.class), factory.getBean("b"));
    }

    @Test
    @DisplayName("创建完成后二级、三级缓存必须清空")
    void caches_are_clean_after_creation() throws Exception {
        factory.getBean(A.class);

        Map<?, ?> level2 = readMap("earlySingletonObjects");
        Map<?, ?> level3 = readMap("singletonFactories");

        assertTrue(level2.isEmpty(), "二级缓存应已清空，实际残留: " + level2.keySet());
        assertTrue(level3.isEmpty(), "三级缓存应已清空，实际残留: " + level3.keySet());
    }

    private Map<?, ?> readMap(String fieldName) throws Exception {
        Field field = DefaultBeanFactory.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Map<?, ?>) field.get(factory);
    }
}
