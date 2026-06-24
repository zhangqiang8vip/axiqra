package com.axiqra.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(security = @SecurityRequirement(name = OpenApiConfig.SA_TOKEN_BEARER))
@SecurityScheme(
        name = OpenApiConfig.SA_TOKEN_BEARER,
        type = SecuritySchemeType.HTTP,
        in = SecuritySchemeIn.HEADER,
        scheme = "bearer",
        bearerFormat = "Sa-Token",
        description = "Use the Sa-Token value in the Authorization header, for example: Bearer <token>."
)
public class OpenApiConfig {

    public static final String SA_TOKEN_BEARER = "saTokenBearer";
}
