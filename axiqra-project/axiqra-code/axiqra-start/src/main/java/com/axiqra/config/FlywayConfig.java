package com.axiqra.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Flyway 配置 — 管理审计库（PostgreSQL）和业务库（CockroachDB）的 Schema 迁移。
 *
 * <p>审计库迁移路径：{@code classpath:db/audit/}
 * <p>业务库迁移路径：{@code classpath:db/migration/}
 * <p>命名规范：V{version}__{description}.sql（例如 V1__init.sql）
 *
 * @author Axiqra Team
 * @date 2026-06-23
 * @see <a href="https://flywaydb.org/documentation/">Flyway Documentation</a>
 */
@Configuration
@ConditionalOnProperty(name = "axiqra-flyway.enabled", havingValue = "true", matchIfMissing = false)
public class FlywayConfig {

    @Value("${axiqra-flyway.locations:classpath:db/audit}")
    private String auditLocations;

    @Value("${axiqra-flyway.migration-locations:classpath:db/migration}")
    private String migrationLocations;

    @Value("${axiqra-flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    @Value("${axiqra-flyway.table:flyway_schema_history}")
    private String table;

    /**
     * Flyway 实例，绑定到审计库 DataSource。
     *
     * <p>注入点：{@code auditDataSource} 由 {@link DataSourceConfig} 提供。
     * Spring Boot 自动检测所有 {@link DataSource} Bean，Flyway 默认选取首个。
     * 此处通过方法参数显式注入审计库 DataSource，确保 Flyway 操作正确的数据库。
     */
    @Bean(initMethod = "migrate")
    public Flyway auditFlyway(@Qualifier("auditDataSource") DataSource auditDataSource) {
        return Flyway.configure()
                .dataSource(auditDataSource)
                .locations(auditLocations)
                .baselineOnMigrate(baselineOnMigrate)
                .table(table)
                .validateOnMigrate(true)
                .outOfOrder(false)
                .encoding("UTF-8")
                .group(true)
                .installedBy("axiqra-app")
                .load();
    }

    /**
     * Flyway 实例，绑定到业务库 DataSource (CockroachDB)。
     *
     * <p>虽然 CockroachDB 与 Flyway 存在一些兼容性问题，
     * 但基本迁移功能（如 ADD COLUMN、CREATE INDEX）是可用的。
     * 此配置允许在开发环境中通过 Flyway 管理业务库 Schema 变更，
     * 生产环境仍可使用 Docker init.sql。
     * 
     * <p>使用 primary dataSource (CockroachDB)
     */
    @Bean(initMethod = "migrate")
    public Flyway businessFlyway(@Qualifier("dataSource") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(migrationLocations)
                .baselineOnMigrate(baselineOnMigrate)
                .table(table)
                .validateOnMigrate(true)
                .outOfOrder(false)
                .encoding("UTF-8")
                .group(true)
                .installedBy("axiqra-app")
                .load();
    }
}
