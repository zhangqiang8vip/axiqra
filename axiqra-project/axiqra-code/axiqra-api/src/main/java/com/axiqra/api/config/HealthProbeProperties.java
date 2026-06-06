package com.axiqra.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "axiqra.health.probes")
public class HealthProbeProperties {

    private static final String DEFAULT_POSTGRES_HOST = "localhost";
    private static final int DEFAULT_POSTGRES_PORT = 5432;
    private static final String DEFAULT_MINIO_HEALTH_URL = "http://localhost:9000/minio/health/live";

    private String postgresHost = DEFAULT_POSTGRES_HOST;
    private int postgresPort = DEFAULT_POSTGRES_PORT;
    private String minioHealthUrl = DEFAULT_MINIO_HEALTH_URL;

    public String getPostgresHost() {
        return postgresHost;
    }

    public void setPostgresHost(String postgresHost) {
        this.postgresHost = postgresHost;
    }

    public int getPostgresPort() {
        return postgresPort;
    }

    public void setPostgresPort(int postgresPort) {
        this.postgresPort = postgresPort;
    }

    public String getMinioHealthUrl() {
        return minioHealthUrl;
    }

    public void setMinioHealthUrl(String minioHealthUrl) {
        this.minioHealthUrl = minioHealthUrl;
    }
}
