package com.informationplatform.hub.testing;

import java.net.URI;
import java.util.Locale;

/** 集中校验数据库集成测试目标，防止测试和 Flyway 误写开发库。 */
public final class DatabaseIntegrationTestSafety {

    private DatabaseIntegrationTestSafety() {
    }

    /**
     * 要求 JDBC URL 明确指向测试库，并拒绝任何名称中带有 dev 的数据库。
     *
     * @param jdbcUrl 待校验的 MySQL JDBC URL
     * @return 原始 JDBC URL，便于在属性注册处直接复用
     */
    public static String requireTestDatabase(String jdbcUrl) {
        String databaseName = databaseName(jdbcUrl).toLowerCase(Locale.ROOT);
        if (!databaseName.contains("test") || databaseName.contains("dev")) {
            throw new IllegalStateException(
                    "Database integration tests require a non-development test database, but got: "
                            + databaseName);
        }
        return jdbcUrl;
    }

    /**
     * 空库全量迁移必须使用名称中明确包含 empty 的专用测试库，避免误判普通升级。
     *
     * @param jdbcUrl 待校验的 MySQL JDBC URL
     * @return 原始 JDBC URL，便于 Flyway 直接复用
     */
    public static String requireEmptyTestDatabase(String jdbcUrl) {
        requireTestDatabase(jdbcUrl);
        String databaseName = databaseName(jdbcUrl).toLowerCase(Locale.ROOT);
        if (!databaseName.contains("empty")) {
            throw new IllegalStateException(
                    "Empty-schema migration requires a dedicated database containing 'empty', but got: "
                            + databaseName);
        }
        return jdbcUrl;
    }

    private static String databaseName(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:mysql://")) {
            throw new IllegalStateException("A valid MySQL JDBC URL is required");
        }
        URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
        String path = uri.getPath();
        if (path == null || path.length() <= 1) {
            throw new IllegalStateException("MySQL JDBC URL must contain a database name");
        }
        return path.substring(1);
    }
}
