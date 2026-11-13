# mini-spring

一个手写的 IoC 容器，主代码 485 行、18 个类。目标不是造框架，是把 Spring 的核心机制——**扫描、实例化、注入、循环依赖、生命周期、代理**——变成能跑起来、能被测试钉住的代码。

字段名与方法名刻意与 Spring 保持一致，便于对照源码阅读。

## 它现在能做到什么

| 能力 | 实现位置 |
| --- | --- |
| 扫包并注册 Bean 定义 | `core/ClassPathScanner` + `core/BeanDefinitionReader` |
| 反射实例化 + 单例缓存 | `bean/DefaultBeanFactory#createBean` |
| `@Autowired` 字段注入 | `bean/DefaultBeanFactory#populateBean` |
| 三级缓存解循环依赖 | `earlySingletonObjects` / `singletonFactories` / `getSingleton` |
| `BeanPostProcessor` + `@PostConstruct` | `bean/DefaultBeanFactory#initializeBean` |
| 基于 JDK 动态代理的 AOP | `bean/DefaultBeanFactory#createProxyIfNeeded` |

## 快速验证

```bash
mvn -B test
```

24 个用例，全部通过。每个测试类对应一个能力：

| 测试类 | 用例数 | 覆盖内容 |
| --- | --- | --- |
| `ContainerV01Test` | 2 | 包扫描、Bean 名计算、重名冲突 |
| `ContainerV01FactoryTest` | 7 | 实例化、单例缓存、按类型查找、各类查找失败 |
| `ContainerV02InjectionTest` | 4 | 字段注入、注入对象与容器单例一致、依赖缺失 |
| `ContainerV03CircularTest` | 3 | A→B→A 解循环、仍是单例、临时缓存已清空 |
| `ContainerV04LifecycleTest` | 4 | BPP 调用顺序、`@PostConstruct`、对象替换、原有链路回归 |
| `ContainerV05AopTest` | 4 | 代理生成、语义不变、无切点不代理、代理与自引用一致 |

## 五个版本

| 版本 | 内容 | 关键点 |
| --- | --- | --- |
| v0.1 | 扫描 + 实例化 | `Class.forName(name, false, loader)` 不触发静态初始化；扫描结果用 `Set` 去重 |
| v0.2 | 字段注入 | `@Autowired` 只标字段；`setAccessible(true)` 是跨包注入 private 字段的必要条件 |
| v0.3 | 循环依赖 | 三级缓存的工厂注册必须在属性填充**之前**，这是"提前暴露"的时机 |
| v0.4 | 生命周期 | 两轮遍历：所有 `before` → `@PostConstruct` → 所有 `after` |
| v0.5 | AOP | 两处生成代理（提前引用 / 初始化后），靠 `earlyProxyReferences` 保证只发生一次 |

## 核心设计

### `createBean` 的六步

```
registry.get(name) 拿定义
  → 反射实例化            → instance
  → 注册三级缓存工厂        → () -> getEarlyReference(name, instance)
  → populateBean            → 填充 @Autowired 字段
  → initializeBean          → BPP.before → @PostConstruct → BPP.after → 可能的代理
  → 对齐二级缓存            → 若曾被提前引用，用二级缓存里的对象覆盖
  → 入一级缓存
```

第 2 步与第 4 步的顺序是循环依赖成立的前提；第 5 步的对齐是"容器里存的对象"与"提前注入给别人的对象"保持同一性的保证。

### 三级缓存的职责

| 层级 | 存什么 | 作用 |
| --- | --- | --- |
| 一级 `singletonObjects` | 装配完成的成品 | `getBean` 直接命中 |
| 二级 `earlySingletonObjects` | 已经暴露过的早期引用 | 保证工厂只被调用一次 |
| 三级 `singletonFactories` | 生成引用的工厂 | 把"要不要生成代理"推迟到真正被借阅时 |

第三级存的是**函数而不是对象**，所以没有循环依赖的 Bean 永远不会触发代理生成——这是三级相对两级的唯一意义：把决策从"无条件执行"变成"按需执行"。

### AOP 两个入口

| 场景 | 代理在哪生成 | 去重手段 |
| --- | --- | --- |
| 无循环依赖 | `initializeBean` 的 `after` 阶段 | 第一次生成，登记 `earlyProxyReferences` |
| 有循环依赖 | `getEarlyReference`（被借阅时） | `after` 阶段发现已登记，返回原始对象，最后由二级缓存对齐 |

## 与 Spring 的对应关系

| 本项目的类 / 结构 | Spring 对应 |
| --- | --- |
| `BeanDefinition` / `BeanDefinitionRegistry` | `BeanDefinition` / `BeanDefinitionRegistry` |
| `DefaultBeanFactory` | `DefaultListableBeanFactory` |
| 三级缓存字段 | `DefaultSingletonBeanRegistry` 的 `singletonObjects` / `earlySingletonObjects` / `singletonFactories` |
| `earlyProxyReferences` | `AbstractAutoProxyCreator#earlyProxyReferences` |
| `BeanPostProcessor` | 同名接口 |
| `getEarlyReference` | `AbstractAutoProxyCreator#getEarlyBeanReference` |
| `createProxyIfNeeded` | `AbstractAutoProxyCreator#wrapIfNecessary` |

## 已知边界

主动不做的部分，写清楚是为了说明"知道边界在哪"，而不是没看见：

- **只支持 `file:` 协议**。打成 jar 后目录扫描会抛 `UnsupportedOperationException`（`File` 抽象不了 jar 条目，需要 `JarFile` + `getEntry`）
- **不支持构造器注入与 setter 注入**。这直接导致 `singletonsCurrentlyInCreation` 没有被启用——没有构造器注入就不存在"三级缓存也救不了"的路径，写进去是一段永远跑不到的死代码
- **`BeanPostProcessor` 不参与扫描**，需 `addBeanPostProcessor()` 手动注册。避免"创建 BPP 时又要找 BPP"的自举问题
- **BPP 返回 null 的中断语义未实现**。Spring 会在某个 BPP 返回 null 时中断后续处理，本项目不处理
- **只处理本类声明的字段与方法**，不含父类（`getDeclaredFields` / `getDeclaredMethods` 的边界）
- **`@Autowired` 跳过 static 字段**，不支持 final 字段
- **AOP 只支持 JDK 动态代理**，目标类必须实现接口；无接口时抛异常而非回退 CGLIB
- **切点只支持方法级 `@Timed`**，没有表达式、没有 `@Aspect`、不区分通知类型
- 循环依赖与 AOP 的组合只覆盖了自引用场景（`Node` 夹具），未覆盖"A→B→A 且 A 被代理"
- 并发未加锁,不能保证多线程下的安全性

## 包结构

```
src/main/java/com/mini/spring/
├─ annotation/   @Component @Autowired @PostConstruct @Timed
├─ core/         扫描器、BeanDefinition、注册表、Reader
├─ bean/         BeanFactory、DefaultBeanFactory、BeanPostProcessor、ObjectFactory
└─ exception/    BeanCreationException、NoSuchBeanDefinitionException 等
```

## 构建

```bash
mvn -B compile
mvn -B test
```

JDK 17，无第三方依赖（AOP 用 JDK 自带的 `java.lang.reflect.Proxy`）。

## 写这个项目时卡最久的地方

在写initializeBean()的时候将返回值遗忘了,没有接收返回值;
导致BeanPostProcessor的方法完全没有生效,关键是代码完全不报错,等到我写单元测试测BPP.after返回时容器里是不是新对象爆红才发现有问题;
然后一直在查看是不是BPP或者invokeInitMethod的逻辑写错了,后面才发现是返回值没有接收;包括后面Proxy.newProxyInstance()的返回值开始时也忘记接收了导致出错
