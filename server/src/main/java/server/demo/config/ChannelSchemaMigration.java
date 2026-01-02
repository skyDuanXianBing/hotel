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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 迁移 channels 表的唯一约束：从全局 code 唯一，调整为 (store_id, code) 复合唯一。
 *
 * <p>说明：项目未启用 Flyway，且 JPA ddl-auto=update 不会自动删除旧约束，因此用启动时 JDBC 兜底迁移。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ChannelSchemaMigration implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ChannelSchemaMigration.class);

    private final DataSource dataSource;

    public ChannelSchemaMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        try (Connection connection = dataSource.getConnection()) {
            if (!tableExists(connection, "channels")) {
                return;
            }

            List<IndexInfo> uniqueIndexes = listUniqueIndexes(connection, "channels");

            for (IndexInfo idx : uniqueIndexes) {
                String cols = idx.columnsCsvLower();
                if ("code".equals(cols)) {
                    logger.info("[ChannelSchemaMigration] Drop legacy unique index on channels.code: {}", idx.indexName);
                    try (Statement st = connection.createStatement()) {
                        st.execute("ALTER TABLE channels DROP INDEX `" + idx.indexName + "`");
                    }
                }
            }

            if (!hasUniqueIndexOnStoreAndCode(uniqueIndexes)) {
                logger.info("[ChannelSchemaMigration] Add unique index uk_channels_store_code(store_id, code)");
                try (Statement st = connection.createStatement()) {
                    st.execute("ALTER TABLE channels ADD UNIQUE INDEX uk_channels_store_code (store_id, code)");
                }
            }
        } catch (Exception e) {
            logger.warn("[ChannelSchemaMigration] Skip migration due to error: {}", e.getMessage());
        }
    }

    private static boolean tableExists(Connection connection, String tableName) throws Exception {
        String sql = """
                SELECT 1
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                LIMIT 1
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static List<IndexInfo> listUniqueIndexes(Connection connection, String tableName) throws Exception {
        String sql = """
                SELECT s.INDEX_NAME, s.NON_UNIQUE, s.SEQ_IN_INDEX, s.COLUMN_NAME
                FROM INFORMATION_SCHEMA.STATISTICS s
                WHERE s.TABLE_SCHEMA = DATABASE()
                  AND s.TABLE_NAME = ?
                ORDER BY s.INDEX_NAME, s.SEQ_IN_INDEX
                """;

        List<IndexInfo> indexes = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                IndexInfo current = null;
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    boolean nonUnique = rs.getInt("NON_UNIQUE") != 0;
                    String columnName = rs.getString("COLUMN_NAME");

                    if ("PRIMARY".equalsIgnoreCase(indexName)) {
                        continue;
                    }
                    if (nonUnique) {
                        continue;
                    }

                    if (current == null || !current.indexName.equals(indexName)) {
                        current = new IndexInfo(indexName);
                        indexes.add(current);
                    }
                    current.columns.add(columnName);
                }
            }
        }
        return indexes;
    }

    private static boolean hasUniqueIndexOnStoreAndCode(List<IndexInfo> uniqueIndexes) {
        for (IndexInfo idx : uniqueIndexes) {
            String cols = idx.columnsCsvLower();
            if ("store_id,code".equals(cols) || "code,store_id".equals(cols)) {
                return true;
            }
        }
        return false;
    }

    private static final class IndexInfo {
        private final String indexName;
        private final List<String> columns = new ArrayList<>();

        private IndexInfo(String indexName) {
            this.indexName = indexName;
        }

        private String columnsCsvLower() {
            return String.join(",", columns).toLowerCase(Locale.ROOT);
        }
    }
}

