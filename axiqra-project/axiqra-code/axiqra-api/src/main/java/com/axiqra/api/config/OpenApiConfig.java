package com.axiqra.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @io.swagger.v3.oas.annotations.info.Info(
                title = "Axiqra API",
                version = "1.0.0",
                description = "Axiqra 工程知识闭环平台 API - 提供工程记忆追踪、Case 管理、Solution 验证等核心能力"
        ),
        security = @SecurityRequirement(name = OpenApiConfig.SA_TOKEN_BEARER)
)
@SecurityScheme(
        name = OpenApiConfig.SA_TOKEN_BEARER,
        type = SecuritySchemeType.HTTP,
        in = SecuritySchemeIn.HEADER,
        scheme = "bearer",
        bearerFormat = "Sa-Token",
        description = "使用 Sa-Token 认证。在请求头中设置 Authorization: Bearer <token>"
)
public class OpenApiConfig {

    public static final String SA_TOKEN_BEARER = "saTokenBearer";

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Bean
    public OpenAPI axiqraOpenAPI() {
        String baseUrl = "http://localhost:8080" + contextPath;

        Server localServer = new Server()
                .url(baseUrl)
                .description("本地开发环境");

        Server prodServer = new Server()
                .url("https://api.axiqra.com")
                .description("生产环境");

        return new OpenAPI()
                .info(new Info()
                        .title("Axiqra API 文档")
                        .version("1.0.0")
                        .description("""
                                ## Axiqra 工程知识闭环平台

                                提供以下核心能力：

                                - **工程记忆追踪 (Trace)**：记录和管理工程决策、变更、验证过程
                                - **Case 管理**：维护 Public Case 和 Project Case，支持解决方案复用
                                - **Solution 验证**：完整的 Solution 生命周期管理
                                - **搜索能力**：全文搜索和向量搜索
                                - **贡献与审核**：社区贡献和内容审核流程

                                ### 认证方式

                                使用 Sa-Token 进行身份认证。在请求头中设置：

                                ```
                                Authorization: Bearer <your-sa-token>
                                ```
                                """)
                        .contact(new Contact()
                                .name("Axiqra Team")
                                .email("api@axiqra.com")
                                .url("https://www.axiqra.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(localServer, prodServer));
    }
}
