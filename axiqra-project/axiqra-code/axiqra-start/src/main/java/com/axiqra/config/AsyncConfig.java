package com.axiqra.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 异步配置。
 *
 * @author Axiqra Team
 * @date 2026-06-10
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("axiqra-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * TaskDecorator that propagates MDC context (traceId, userId) from the submitting
     * thread into the async worker thread, and clears it afterwards.
     */
    private static class MdcTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            Map<String, String> context = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (context != null) {
                        MDC.setContextMap(context);
                    }
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        }
    }

    /**
     * 全局异步未捕获异常处理器。
     * <p>
     * 当异步方法抛出未被 try-catch 捕获的异常时，会调用此处理器。
     * 确保所有异步异常都被记录，防止静默失败。
     */
    @Bean
    public AsyncUncaughtExceptionHandler asyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) -> {
            String traceId = MDC.get("traceId");
            log.error("【异步未捕获异常】method={}.{}(), traceId={}, message={}",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(),
                    traceId,
                    ex.getMessage(),
                    ex);
        };
    }
}
