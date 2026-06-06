package com.axiqra.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(prefix = "axiqra.audit-datasource", name = "enabled", havingValue = "true")
public class DataSourceConfig {

        @Value("${axiqra.audit-datasource.url}")
        private String auditUrl;

        @Value("${axiqra.audit-datasource.username}")
        private String auditUsername;

        @Value("${axiqra.audit-datasource.password}")
        private String auditPassword;

        @Bean("auditDataSource")
        @ConditionalOnMissingBean(name = "auditDataSource")
        public DataSource auditDataSource() {
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
        public JdbcTemplate auditJdbcTemplate(DataSource auditDataSource) {
                return new JdbcTemplate(auditDataSource);
        }
}
