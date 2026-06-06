package com.axiqra.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS 配置属性
 * <p>
 * 绑定 security.cors.* 配置项。
 */
@Data
@ConfigurationProperties(prefix = "security.cors")
public class CorsProperties {

    /** 允许的来源列表（生产环境用精确域名） */
    private List<String> allowedOrigins;

    /** 是否允许携带凭证（credentials） */
    private Boolean allowCredentials;

    /** 允许的方法 */
    private List<String> allowedMethods;

    /** 允许的请求头 */
    private List<String> allowedHeaders;

    /** 暴露给前端的响应头 */
    private List<String> exposedHeaders;

    /** 预检请求缓存时间（秒） */
    private Long maxAge;
}
