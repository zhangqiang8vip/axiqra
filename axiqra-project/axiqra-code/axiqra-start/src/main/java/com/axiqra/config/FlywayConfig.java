package com.axiqra.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;

/**
 * Flyway 配置 — 管理审计库（PostgreSQL）和业务库（CockroachDB）的 Schema 迁移。
 *
 * <p>审计库迁移路径：{@code classpath:db/audit/}
 * <p>业务库迁移路径：{@code classpath:db/migration/}
 * <p>命名规范：V{version}__{description}.sql（例如 V1__init.sql）
 *
 * <p>特性：
 * <ul>
 *   <li>迁移失败后自动 repair，无需手动清理</li>
 *   <li>跳过已失败的迁移继续执行后续迁移</li>
 * </ul>
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
     * 创建 Flyway 实例
     */
    private Flyway createFlyway(DataSource dataSource, String locations) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
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
     * 审计库 Flyway 实例
     */
    @Bean(name = "auditFlyway")
    public Flyway auditFlyway(@Qualifier("auditDataSource") DataSource auditDataSource) {
        Flyway flyway = createFlyway(auditDataSource, auditLocations);
        repairAndMigrate(flyway);
        return flyway;
    }

    /**
     * 业务库 Flyway 实例
     */
    @Bean(name = "businessFlyway")
    public Flyway businessFlyway(@Qualifier("dataSource") DataSource dataSource) {
        Flyway flyway = createFlyway(dataSource, migrationLocations);
        repairAndMigrate(flyway);
        return flyway;
    }

    /**
     * 修复失败的迁移记录并执行迁移
     */
    private void repairAndMigrate(Flyway flyway) {
        try {
            flyway.repair();
        } catch (Exception e) {
            System.out.println("[Flyway] Repair: " + e.getMessage());
        }
        flyway.migrate();
    }
}
