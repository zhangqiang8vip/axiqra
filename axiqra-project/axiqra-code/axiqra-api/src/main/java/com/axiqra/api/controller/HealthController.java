package com.axiqra.api.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/internal/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;

    public HealthController(JdbcTemplate jdbcTemplate,
                            StringRedisTemplate stringRedisTemplate,
                            RabbitTemplate rabbitTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "service", "axiqra-core",
                "status", "UP",
                "timestamp", OffsetDateTime.now().toString()
        );
    }

    @GetMapping("/verify")
    public Map<String, Object> verify() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", "axiqra-core");
        result.put("timestamp", OffsetDateTime.now().toString());
        result.put("cockroachDb", jdbcTemplate.queryForObject("SELECT 1", Integer.class));
        result.put("redis", stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.ping()));
        result.put("rabbitMq", rabbitTemplate.execute(channel -> channel.isOpen() ? "CONNECTED" : "CLOSED"));
        result.put("postgresAuditTcp", probeTcp("localhost", 5432));
        result.put("minioHttp", probeHttp("http://localhost:9000/minio/health/live"));
        result.put("status", "UP");
        return result;
    }

    private String probeTcp(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            return socket.isConnected() ? "CONNECTED" : "DISCONNECTED";
        } catch (IOException ex) {
            return ex.getClass().getSimpleName();
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
            return ex.getClass().getSimpleName();
        }
    }
}
