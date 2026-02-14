package server.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 修复 Flyway 校验失败（例如：数据库已应用某些迁移，但本地迁移文件已被删除/未同步）。
 *
 * 默认关闭：避免每次启动都对 schema_history 做 repair。
 * 需要时可临时开启：FLYWAY_REPAIR_ON_STARTUP=true
 */
@Configuration
public class FlywayRepairOnStartupConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayRepairOnStartupConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(
            @Value("${app.flyway.repair-on-startup:false}") boolean repairOnStartup
    ) {
        return flyway -> {
            if (repairOnStartup) {
                log.warn("[Flyway] repair-on-startup enabled. Running flyway.repair() before migrate()");
                flyway.repair();
            }
            flyway.migrate();
        };
    }
}

