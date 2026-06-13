package com.axiqra;

import com.axiqra.context.ApplicationContextHolder;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Axiqra S1 启动类。
 */
@EnableAsync
@EnableTransactionManagement
@ConfigurationPropertiesScan(basePackages = "com.axiqra")
@MapperScan(basePackages = "com.axiqra.core.mapper")
@SpringBootApplication(scanBasePackages = "com.axiqra")
public class AxiqraApplication {

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(AxiqraApplication.class, args);
        ApplicationContextHolder.setApplicationContext(ctx);
    }
}
