package com.axiqra.api.config;

import com.axiqra.api.controller.HealthController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.HttpEncodingAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = SecurityConfigTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("should allow anonymous access to internal health endpoint")
    void shouldAllowAnonymousAccessToInternalHealthEndpoint() throws Exception {
        mockMvc.perform(get("/internal/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.service").value("axiqra-api"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("should allow anonymous access to internal health verify endpoint")
    void shouldAllowAnonymousAccessToInternalHealthVerify() throws Exception {
        mockMvc.perform(get("/internal/health/verify").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.service").value("axiqra-api"));
    }

    @Test
    @DisplayName("should allow anonymous get to all whitelisted patterns")
    void shouldAllowAnonymousGetToAllWhitelistedPatterns() throws Exception {
        mockMvc.perform(get("/api/internal/health")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/info")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should block non get methods to health endpoints")
    void shouldBlockNonGetMethodsToHealthEndpoints() throws Exception {
        mockMvc.perform(post("/internal/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should require authentication for non-whitelisted endpoints")
    void shouldRequireAuthenticationForNonWhitelistedEndpoints() throws Exception {
        mockMvc.perform(get("/secured").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should reject invalid basic auth on non-whitelisted endpoints")
    void shouldRejectInvalidBasicAuthOnNonWhitelistedEndpoints() throws Exception {
        mockMvc.perform(get("/secured")
                        .with(httpBasic("user", "bad-password"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should allow valid credentials to access protected endpoint")
    void shouldAllowValidCredentialsToAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/secured")
                        .with(httpBasic("user", "password"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("secured"));
    }

    @Configuration(proxyBeanMethods = false)
    @Import({SecurityConfig.class, HealthController.class, TestSecuredController.class})
    @ImportAutoConfiguration({
            WebMvcAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            HttpEncodingAutoConfiguration.class,
            SecurityAutoConfiguration.class,
            SecurityFilterAutoConfiguration.class,
            UserDetailsServiceAutoConfiguration.class
    })
    static class TestApplication {

        @Bean
        UserDetailsManager userDetailsManager() {
            UserDetails user = User.withUsername("user")
                    .password("{noop}password")
                    .roles("USER")
                    .build();
            return new InMemoryUserDetailsManager(user);
        }
    }

    @RestController
    static class TestSecuredController {

        @GetMapping("/secured")
        String secured() {
            return "secured";
        }

        @PostMapping("/internal/health")
        String blockedHealthPost() {
            return "blocked";
        }
    }
}
