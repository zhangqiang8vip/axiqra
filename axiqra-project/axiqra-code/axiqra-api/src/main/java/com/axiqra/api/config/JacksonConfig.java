package com.axiqra.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigInteger;

/**
 * Jackson 配置：把 Long 类型 / BigInteger 序列化为字符串，避免 JavaScript Number 精度丢失
 *
 * <p>背景：
 * <ul>
 *   <li>JavaScript Number 类型只能精确表示 ≤ 2^53 (9007199254740992) 的整数</li>
 *   <li>CockroachDB / PostgreSQL 雪花算法 ID 通常 18 位（> 2^53），超过安全整数范围</li>
 *   <li>默认 Jackson 把 Long 当作 number 输出 JSON，前端 / MCP 解析时被精度破坏</li>
 *   <li>本配置把 Long / Long 包装 / BigInteger 全部转为字符串输出</li>
 * </ul>
 *
 * <p>影响范围：所有 @RestController 响应、所有 JSON 序列化输出。
 * 不影响：
 * <ul>
 *   <li>请求体反序列化（输入 Long 字段仍然接受 number，但精度已被前端截断 → 仍是问题）</li>
 *   <li>@PathVariable @RequestParam 反序列化（由 Spring MVC Binding 处理）</li>
 * </ul>
 *
 * <p>本类兼容 axios / MCP / 浏览器等 JS 客户端的常见做法（数字字段以字符串返回）。
 *
 * @author Axiqra Team
 * @date 2026-06-27
 */
@Configuration
public class JacksonConfig {

    /**
     * 自定义 Jackson Builder：注入 Long / BigInteger → String 序列化器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longAsStringCustomizer() {
        return builder -> {
            SimpleModule longAsStringModule = new SimpleModule("LongAsStringModule");
            // 把所有 Long / long 类型序列化时转为字符串
            ToStringSerializer stringSerializer = ToStringSerializer.instance;
            longAsStringModule.addSerializer(Long.class, stringSerializer);
            longAsStringModule.addSerializer(Long.TYPE, stringSerializer);
            longAsStringModule.addSerializer(BigInteger.class, stringSerializer);
            // 处理原子类型
            longAsStringModule.addSerializer(java.util.concurrent.atomic.LongAdder.class, stringSerializer);
            builder.modulesToInstall(longAsStringModule);
        };
    }

    /**
     * 提供一个 @Primary ObjectMapper，确保替换 Spring Boot 默认的 ObjectMapper bean，
     * 防止某些组件（如 SaToken 拦截器、JWT 工具）持有旧 ObjectMapper 引用。
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.build();
        SimpleModule module = new SimpleModule("LongAsStringModule");
        ToStringSerializer stringSerializer = ToStringSerializer.instance;
        module.addSerializer(Long.class, stringSerializer);
        module.addSerializer(Long.TYPE, stringSerializer);
        module.addSerializer(BigInteger.class, stringSerializer);
        mapper.registerModule(module);
        // 兼容 Java 8 time
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}