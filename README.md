# mini-spring

手写 IoC 容器，目的是把 Spring 的核心机制变成能跑起来的代码。

## 版本计划

| 版本 | 内容 | 验收标准 |
| --- | --- | --- |
| v0.1 | 扫描 + 实例化 | 给定包名，打印所有 `@Component` 的 Bean 名与 Class |
| v0.2 | 字段注入 | Service 中 `@Autowired` 一个 Repository，能调到它的方法 |
| v0.3 | 循环依赖 | A→B→A 创建成功，`a.getB().getA() == a` 为 true |
| v0.4 | 生命周期 | 日志 BeanPostProcessor 生效，`@PostConstruct` 被调用 |
| v0.5（可选） | AOP | 从容器取到的对象是代理，方法耗时被打印 |

每完成一版，`mvn test` 全绿后提交。

## 与 Spring 的对应关系

| 本项目的类 | Spring 对应 |
| --- | --- |
| `BeanDefinition` | `org.springframework.beans.factory.config.BeanDefinition` |
| `DefaultBeanFactory` | `DefaultListableBeanFactory` |
| 三级缓存四个字段 | `DefaultSingletonBeanRegistry` 中的 `singletonObjects` / `earlySingletonObjects` / `singletonFactories` / `singletonsCurrentlyInCreation` |
| `BeanPostProcessor` | 同名接口 |

字段名刻意与 Spring 保持一致，便于对照源码阅读。

## 构建

```bash
mvn -B compile
mvn -B test
```

## 包结构

```
src/main/java/com/mini/spring/
├─ annotation/   自定义注解：@Component @Autowired @PostConstruct
├─ core/         扫描器、BeanDefinition、注册表
├─ bean/         BeanFactory 与实现（三级缓存、生命周期）
└─ aop/          动态代理与切面（v0.5 可选）
```
