package com.axiqra.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(prefix = "axiqra.audit-datasource", name = "enabled", havingValue = "true")
public class DataSourceConfig {

        private static final String SENTINEL_VALUE = "change_me_before_production";

        @Value("${axiqra.audit-datasource.url}")
        private String auditUrl;

        @Value("${axiqra.audit-datasource.username}")
        private String auditUsername;

        @Value("${axiqra.audit-datasource.password}")
        private String auditPassword;

        @PostConstruct
        void validateAuditPassword() {
                if (auditPassword == null || auditPassword.isBlank()
                                || SENTINEL_VALUE.equals(auditPassword)) {
                        throw new IllegalStateException(
                                "审计库密码未配置或仍为默认哨兵值。"
                                        + "请设置环境变量 AUDIT_APP_DB_PASSWORD 为实际密码。");
                }
        }

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
        public JdbcTemplate auditJdbcTemplate(@Qualifier("auditDataSource") DataSource auditDataSource) {
                return new JdbcTemplate(auditDataSource);
        }
}
