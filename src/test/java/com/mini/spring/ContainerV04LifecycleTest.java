package com.mini.spring;

import com.mini.demo.UserService;
import com.mini.lifecycle.LifecycleBean;
import com.mini.spring.bean.BeanPostProcessor;
import com.mini.spring.bean.DefaultBeanFactory;
import com.mini.spring.core.BeanDefinitionReader;
import com.mini.spring.core.BeanDefinitionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("v0.4 生命周期")
class ContainerV04LifecycleTest {

    private DefaultBeanFactory beanFactory;
    private final List<String> events = new ArrayList<>();

    @BeforeEach
    void setup(){
        events.clear();
        BeanDefinitionRegistry registry = new BeanDefinitionRegistry();
        for (String pkg : List.of("com.mini.lifecycle", "com.mini.demo")) {
            BeanDefinitionReader.scanComponents(pkg).forEach(registry::register);
        }
        beanFactory = new DefaultBeanFactory(registry);
    }

    @Test
    @DisplayName("BPP 调用两次，before 必须在 @PostConstruct 之前")
    void before_runs_prior_to_post_construct(){
        registerBpp(new RecordingBpp("p", events));

        beanFactory.getBean("lifecycleBean");

        assertEquals(List.of("p:before:false", "p:after:true"), events,
                "顺序应为 before(未初始化) → @PostConstruct → after(已初始化)");
    }

    @Test
    @DisplayName("BPP.after 返回新对象时，容器里存的是新对象")
    void replaced_object_is_what_container_holds(){
        registerBpp(new ReplacingBpp());

        Object bean = beanFactory.getBean("lifecycleBean");

        assertSame(ReplacingBpp.MARKER, bean,
                "容器必须持有 BPP 返回的替换对象，而不是反射造出的原始对象");
    }

    @Test
    @DisplayName("多个 BPP 按注册顺序，且 before 全跑完才跑 after")
    void multiple_bpp_run_in_registration_order(){
        registerBpp(new RecordingBpp("A", events), new RecordingBpp("B", events));

        beanFactory.getBean("lifecycleBean");

        assertEquals(List.of(
                "A:before:false",
                "B:before:false",
                "A:after:true",
                "B:after:true"
        ), events, "不能是 A-before → A-after → B-before 这种单个 BPP 走完的交错");
    }

    @Test
    @DisplayName("不注册 BPP 时，原有注入链路不受影响")
    void existing_chain_still_works_without_bpp(){
        assertEquals("hello user-7", beanFactory.getBean(UserService.class).greet(7));
    }

    private void registerBpp(BeanPostProcessor... bpps){
        for (BeanPostProcessor bpp : bpps) {
            beanFactory.addBeanPostProcessor(bpp);
        }
    }

    /** 记录 tag:phase:initialized，只关心 LifecycleBean */
    static class RecordingBpp implements BeanPostProcessor{
        private final String tag;
        private final List<String> sink;
        RecordingBpp(String tag, List<String> sink){
            this.tag = tag;
            this.sink = sink;
        }

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) {
            record("before", bean);
            return bean;  //必须返回bean,返回null会让容器存一个null进去
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            record("after", bean);
            return bean;
        }

        private void record(String phase, Object bean){
            if(!(bean instanceof LifecycleBean)) return;  //其它bean直接放过,只记录LifecycleBean
            boolean initialized = ((LifecycleBean) bean).isInitialized();
            sink.add(tag + ":" + phase + ":" + initialized);
        }
    }

    /** 在 after 阶段把 LifecycleBean 换成另一个对象，模拟 v0.5 的代理替换 */
    static class ReplacingBpp implements BeanPostProcessor{
        static final Object MARKER = new Object();

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (bean instanceof LifecycleBean) return MARKER;
            return bean;
        }
    }
}
