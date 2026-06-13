package com.axiqra.context;

import org.springframework.context.ApplicationContext;

/**
 * Spring ApplicationContext 持有者
 *
 * <p>仅用于极少数无法通过构造函数注入的遗留集成点。
 * 日常业务逻辑优先使用构造函数注入，不要引用此类。
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
public class ApplicationContextHolder {

    private static volatile ApplicationContext context;

    public static void setApplicationContext(ApplicationContext ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("ApplicationContext must not be null");
        }
        if (context != null) {
            throw new IllegalStateException("ApplicationContext 已经初始化，不能重复设置");
        }
        synchronized (ApplicationContextHolder.class) {
            if (context != null) {
                throw new IllegalStateException("ApplicationContext 已经初始化，不能重复设置");
            }
            context = ctx;
        }
    }

    public static <T> T getBean(Class<T> clazz) {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext 未初始化，请确认 AxiqraApplication 已启动");
        }
        return context.getBean(clazz);
    }

    public static <T> T getBean(String name, Class<T> clazz) {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext 未初始化，请确认 AxiqraApplication 已启动");
        }
        return context.getBean(name, clazz);
    }
}
