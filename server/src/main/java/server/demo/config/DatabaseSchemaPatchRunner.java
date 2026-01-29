package server.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 数据库结构兼容补丁（启动时自动补齐缺失字段），避免实体字段新增后老库启动失败。
 *
 * 说明：本项目开启了 spring.jpa.hibernate.ddl-auto=update，但在某些环境下可能不会自动补齐列。
 * 因此这里用一个更早执行的 Runner，确保关键列存在，避免后续 migration/业务查询直接报错。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseSchemaPatchRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaPatchRunner.class);

    private final DataSource dataSource;

    public DatabaseSchemaPatchRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        ensureRoomTypesMaxGuestsColumn();
    }

    private void ensureRoomTypesMaxGuestsColumn() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            String schema = getCurrentSchema(conn);
            if (schema == null || schema.isBlank()) {
                log.warn("[DbSchemaPatch] current schema is empty; skip patch");
                return;
            }

            if (!tableExists(conn, schema, "room_types")) {
                log.info("[DbSchemaPatch] table not found, skip. schema={}, table=room_types", schema);
                return;
            }

            if (columnExists(conn, schema, "room_types", "max_guests")) {
                return;
            }

            log.warn("[DbSchemaPatch] missing column, applying patch. schema={}, table=room_types, column=max_guests", schema);
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE room_types ADD COLUMN max_guests INT NOT NULL DEFAULT 4");
            }
            log.info("[DbSchemaPatch] patch applied. schema={}, table=room_types, column=max_guests", schema);
        }
    }

    private static String getCurrentSchema(Connection conn) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT DATABASE()")) {
            if (!rs.next()) {
                return null;
            }
            return rs.getString(1);
        }
    }

    private static boolean tableExists(Connection conn, String schema, String table) throws Exception {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = ? AND table_name = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        }
    }

    private static boolean columnExists(Connection conn, String schema, String table, String column) throws Exception {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = ? AND table_name = ? AND column_name = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            ps.setString(3, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        }
    }
}

