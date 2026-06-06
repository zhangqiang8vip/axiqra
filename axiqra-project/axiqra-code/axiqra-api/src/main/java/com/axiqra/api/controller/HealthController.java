package com.axiqra.api.controller;

import com.axiqra.api.config.HealthProbeProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/internal/health")
public class HealthController {

    private static final int TCP_CONNECT_TIMEOUT_MS = 2000;

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final HealthProbeProperties probeProperties;

    public HealthController(JdbcTemplate jdbcTemplate,
                           StringRedisTemplate stringRedisTemplate,
                           RabbitTemplate rabbitTemplate,
                           HealthProbeProperties probeProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.probeProperties = probeProperties;
    }

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "service", "axiqra-api",
                "status", "UP",
                "timestamp", OffsetDateTime.now().toString()
        );
    }

    @GetMapping("/verify")
    public Map<String, Object> verify() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", "axiqra-api");
        result.put("timestamp", OffsetDateTime.now().toString());

        result.put("cockroachDb", checkDatabase());
        result.put("redis", checkRedis());
        result.put("rabbitMq", checkRabbitMq());
        result.put("postgresAuditTcp", probeTcp(
                probeProperties.getPostgresHost(),
                probeProperties.getPostgresPort()));
        result.put("minioHttp", probeHttp(probeProperties.getMinioHealthUrl()));

        boolean anyDown = "DOWN".equals(result.get("cockroachDb"))
                       || "DOWN".equals(result.get("redis"))
                       || "DOWN".equals(result.get("rabbitMq"));
        result.put("status", anyDown ? "DEGRADED" : "UP");
        return result;
    }

    private String checkDatabase() {
        try {
            return String.valueOf(jdbcTemplate.queryForObject("SELECT 1", Integer.class));
        } catch (Exception ex) {
            return "DOWN";
        }
    }

    private String checkRedis() {
        try {
            return stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
        } catch (Exception ex) {
            return "DOWN";
        }
    }

    private String checkRabbitMq() {
        try {
            return rabbitTemplate.execute(channel -> channel.isOpen() ? "CONNECTED" : "CLOSED");
        } catch (Exception ex) {
            return "DOWN";
        }
    }

    private String probeTcp(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), TCP_CONNECT_TIMEOUT_MS);
            return "CONNECTED";
        } catch (IOException ex) {
            return "DOWN";
        }
    }

    private String probeHttp(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            int status = connection.getResponseCode();
            connection.disconnect();
            return String.valueOf(status);
        } catch (IOException ex) {
            return "DOWN";
        }
    }
}
