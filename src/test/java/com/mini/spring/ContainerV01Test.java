package com.mini.spring;

import com.mini.demo.UserRepository;
import com.mini.demo.UserService;
import com.mini.spring.core.BeanDefinition;
import com.mini.spring.core.BeanDefinitionReader;
import com.mini.spring.core.BeanDefinitionRegistry;
import com.mini.spring.exception.ConflictingBeanDefinitionException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContainerV01Test {

    @Test
    void scan_should_register_all_component_beans() {
        BeanDefinitionRegistry registry = new BeanDefinitionRegistry();

        List<BeanDefinition> definitions = BeanDefinitionReader.scanComponents("com.mini.demo");
        definitions.forEach(registry::register);

        definitions.forEach(bd -> System.out.println(bd.getBeanName() + " -> " + bd.getType()));

        assertEquals(2, definitions.size(), "com.mini.demo 下应有 2 个 @Component");

        Set<String> names = definitions.stream()
                .map(BeanDefinition::getBeanName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("userRepository", "userService"), names, "Bean 名应为类名首字母小写");

        assertSame(UserRepository.class, registry.get("userRepository").getType());
    }

    @Test
    void duplicate_bean_name_should_throw() {
        BeanDefinitionRegistry registry = new BeanDefinitionRegistry();
        registry.register(new BeanDefinition(UserService.class, "userService"));

        assertThrows(ConflictingBeanDefinitionException.class,
                () -> registry.register(new BeanDefinition(UserRepository.class, "userService")));
    }
}
