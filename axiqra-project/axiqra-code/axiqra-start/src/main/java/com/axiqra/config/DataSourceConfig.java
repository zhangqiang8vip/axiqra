package com.axiqra.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

        private static final String SENTINEL_VALUE = "change_me_before_production";

        @Value("${axiqra.audit-datasource.url}")
        private String auditUrl;

        @Value("${axiqra.audit-datasource.username}")
        private String auditUsername;

        @Value("${axiqra.audit-datasource.password}")
        private String auditPassword;

        @Bean("dataSource")
        @Primary
        @ConfigurationProperties(prefix = "spring.datasource.druid")
        public DruidDataSource dataSource(DataSourceProperties dataSourceProperties) {
                DruidDataSource dataSource = new DruidDataSource();
                dataSource.setUrl(dataSourceProperties.getUrl());
                dataSource.setUsername(dataSourceProperties.getUsername());
                dataSource.setPassword(dataSourceProperties.getPassword());
                dataSource.setDriverClassName(dataSourceProperties.getDriverClassName());
                return dataSource;
        }

        @Bean("auditDataSource")
        @ConditionalOnMissingBean(name = "auditDataSource")
        @ConditionalOnProperty(prefix = "axiqra.audit-datasource", name = "enabled", havingValue = "true")
        public DataSource auditDataSource() {
                if (auditPassword == null || auditPassword.isBlank()
                                || SENTINEL_VALUE.equals(auditPassword)) {
                        throw new IllegalStateException(
                                "Audit DB password not configured or still at default sentinel value. "
                                        + "Set environment variable AUDIT_APP_DB_PASSWORD.");
                }
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(auditUrl);
                config.setUsername(auditUsername);
                config.setPassword(auditPassword);
                config.setDriverClassName("org.postgresql.Driver");
                config.setMaximumPoolSize(2);
                config.setMinimumIdle(1);
                config.setConnectionTimeout(5000);
                config.setValidationTimeout(3000);
                config.setPoolName("audit-pool");
                return new HikariDataSource(config);
        }

        @Bean("auditJdbcTemplate")
        @ConditionalOnProperty(prefix = "axiqra.audit-datasource", name = "enabled", havingValue = "true")
        public JdbcTemplate auditJdbcTemplate(@Qualifier("auditDataSource") DataSource auditDataSource) {
                return new JdbcTemplate(auditDataSource);
        }
}
