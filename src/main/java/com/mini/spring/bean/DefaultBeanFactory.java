package com.mini.spring.bean;

import com.mini.spring.annotation.Autowired;
import com.mini.spring.annotation.PostConstruct;
import com.mini.spring.annotation.Timed;
import com.mini.spring.core.BeanDefinition;
import com.mini.spring.core.BeanDefinitionRegistry;
import com.mini.spring.exception.BeanCreationException;
import com.mini.spring.exception.NoSuchBeanDefinitionException;
import com.mini.spring.exception.NoUniqueBeanDefinitionException;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultBeanFactory implements BeanFactory{

    private final BeanDefinitionRegistry registry;
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();  //一级缓存

    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();  //二级缓存
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>();//三级缓存

    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();

    /* Spring里是Map<Object, Object> (cacheKey -> bean) 这里仅需要记录谁被提前代理过,简化写法 */
    private final Set<String> earlyProxyReferences = ConcurrentHashMap.newKeySet();

    public DefaultBeanFactory(BeanDefinitionRegistry registry) {
        this.registry = Objects.requireNonNull(registry,"registry不能为null");
    }

    @Override
    public Object getBean(String name){

        Object singleton = singletonObjects.get(name);  //
        if(singleton != null){
            return singleton;
        }
        Object earlySingleton = getSingleton(name);
        if(earlySingleton != null){
            return earlySingleton;
        }

        return createBean(name);
    }

    @Override
    public <T> T getBean(Class<T> type) {

        List<BeanDefinition> beanDefinitionList = registry.getBeanDefinitions();
        List<String> beanNames = new ArrayList<>();
        for (BeanDefinition bd : beanDefinitionList) {
            if (type.isAssignableFrom(bd.getType())) {  //判断type能否接收当前遍历到的bd的type
                beanNames.add(bd.getBeanName());
            }
        }
        int beanCount = beanNames.size();
        if(beanCount == 0){
            throw new NoSuchBeanDefinitionException("未查找到这个bean: " + type.getName());
        } else if (beanCount >= 2) {
            throw new NoUniqueBeanDefinitionException("一个类型匹配到多个bean:" + beanNames.get(0));  //查找阶段出错
        }
        Object bean = getBean(beanNames.get(0));
        return (T) bean;
    }

    private Object createBean(String name){

        BeanDefinition bd = registry.get(name);
        if(bd == null) throw new NoSuchBeanDefinitionException("未找到对应的beanDefinition");
        Object instance;
        Object exposed;
        try {
            try {
                instance = bd.getType().getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) { //直接抛出父异常
                throw new BeanCreationException("bean初始化异常:" + name, e);
            }

            final Object rawInstance = instance;
            singletonFactories.put(name, () -> getEarlyReference(name, rawInstance));  //此时放进去的是lambda表达式,只有在真正调用getObject时才会执行

            exposed = rawInstance;  //rawInstance和exposed在有循环依赖和BPP替换时不一致,v0.5处理

            populateBean(exposed);  //依赖注入

            exposed = initializeBean(name, exposed); //初始化

            Object early = earlySingletonObjects.get(name);
            if(early != null) exposed = early;  /* 缓存对齐, 如果不对齐在循环依赖下会出现创建了代理对象,却拿到原始对象的情况 */

            singletonObjects.put(name, exposed);
        } finally {
            singletonFactories.remove(name);
            earlySingletonObjects.remove(name);
            //删除创建中的标记
        }
        return exposed;
    }

    private void populateBean(Object instance){

        Field[] declaredFields = instance.getClass().getDeclaredFields();  //获取所有声明的字段,包括私有
        for(Field field : declaredFields){
            if(field.getAnnotation(Autowired.class) == null) continue; //跳过未加注解的
            if(Modifier.isStatic(field.getModifiers())) continue;  //跳过static静态字段

            field.setAccessible(true);
            Object dependency = getBean(field.getType());  //**可能会出现循环依赖**
            try {
                field.set(instance, dependency); //将依赖对象交给字段
            } catch (IllegalAccessException e) {
                throw new BeanCreationException("字段无法注入: " + instance.getClass().getName() + "#" + field.getName(), e);
            }
        }
    }

    private Object getSingleton(String name){
        Object earlySingleton = earlySingletonObjects.get(name);  //从二级缓存取
        if(earlySingleton != null) {
            return earlySingleton;
        }

        ObjectFactory<?> factory = singletonFactories.get(name); //二级缓存找不到,再从三级缓存找
        if(factory == null) return null;
        Object factoryObject = factory.getObject();
        if(factoryObject == null) return null;  //防御性判断,v0.3下不可达

        earlySingletonObjects.put(name, factoryObject);
        singletonFactories.remove(name);  //必须移除三级缓存中的单例
        return factoryObject;
    }

    private Object getEarlyReference(String name, Object instance){
        return createProxyIfNeeded(name, instance);
    }

    public void addBeanPostProcessor(BeanPostProcessor bpp){
        Objects.requireNonNull(bpp, "添加的BeanPostProcessor不能为null");
        beanPostProcessors.add(bpp);
    }

    private Object initializeBean(String beanName, Object bean){
        //不处理BPP返回null的中断语义
        for(BeanPostProcessor bpp : beanPostProcessors){
            bean = bpp.postProcessBeforeInitialization(bean, beanName);
        }

        invokeInitMethod(beanName, bean);

        for(BeanPostProcessor bpp : beanPostProcessors){
            bean = bpp.postProcessAfterInitialization(bean, beanName);
        }
        return createProxyIfNeeded(beanName, bean);
    }

    private void invokeInitMethod(String beanName, Object bean){
        Method[] methods = bean.getClass().getDeclaredMethods();
        for(Method method : methods){
            if(method.getAnnotation(PostConstruct.class) == null) continue;

            try {
                method.setAccessible(true);
                method.invoke(bean);  //反射调用真正的方法执行
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                throw new BeanCreationException("初始化方法执行失败: " + beanName + "#" + method.getName(), e);
            }
        }
    }

    private Object createProxyIfNeeded(String beanName, Object bean){
        Method[] methods = bean.getClass().getDeclaredMethods();
        for(Method method : methods){
            if(method.getAnnotation(Timed.class) != null){
                if(earlyProxyReferences.contains(beanName)){
                    return bean; //已经提前代理过了,交给末尾对齐
                }
                earlyProxyReferences.add(beanName);//未提前代理,登记

                Class<?>[] interfaces = bean.getClass().getInterfaces();  //单独处理没有接口的情况,不处理没有接口时会返回不含任何方法的对象
                if(interfaces.length == 0){
                    throw new BeanCreationException("无法代理(无接口) " + beanName + "JDK动态代理需要实现接口");
                }
                //创建代理对象并返回
                return Proxy.newProxyInstance(bean.getClass().getClassLoader(),
                        interfaces,
                        (proxy, method1, args) -> {
                            Object proxyBean = null;
                            long costTime = 0;
                            try {
                                long start = System.nanoTime();
                                proxyBean = method1.invoke(bean, args);
                                costTime = System.nanoTime() - start;
                            } catch (InvocationTargetException e) {
                                throw e.getTargetException();  //拆开被包装的异常,让调用放可以catch到原始异常
                            }catch (IllegalAccessException e){
                                throw new BeanCreationException("代理调用失败: " + beanName, e);
                            }
                            System.out.println(bean.getClass().getName() + "#" + method1.getName() + "耗时: " + costTime + "ms");
                            return proxyBean;
                        });
            }
        }
        return bean;  //不存在被@Timed标注的方法,直接返回bean本身
    }
}
