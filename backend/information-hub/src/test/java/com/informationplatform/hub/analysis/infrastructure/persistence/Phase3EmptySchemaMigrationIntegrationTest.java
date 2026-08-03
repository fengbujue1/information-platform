package com.informationplatform.hub.analysis.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.informationplatform.hub.testing.DatabaseIntegrationTestSafety;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** 验证专用空测试库可以一次性执行 V1 到 V2 的全部 migration。 */
@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_EMPTY_TEST_DB_URL",
        matches = "jdbc:mysql://.+")
class Phase3EmptySchemaMigrationIntegrationTest {

    @Test
    void emptySchemaMigratesFromNoVersionToVersionTwo() throws SQLException {
        String databaseUrl = DatabaseIntegrationTestSafety.requireEmptyTestDatabase(
                requiredEnvironmentVariable("INFORMATION_HUB_EMPTY_TEST_DB_URL"));
        String databaseUsername =
                requiredEnvironmentVariable("INFORMATION_HUB_EMPTY_TEST_DB_USERNAME");
        String databasePassword =
                requiredEnvironmentVariable("INFORMATION_HUB_EMPTY_TEST_DB_PASSWORD");
        Flyway flyway = Flyway.configure()
                .dataSource(databaseUrl, databaseUsername, databasePassword)
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .validateOnMigrate(true)
                .load();

        // 空库验证必须先确认没有 Flyway 历史，避免把普通升级误报为全量迁移成功。
        assertNull(flyway.info().current());

        MigrateResult result = flyway.migrate();

        assertTrue(result.success, "空测试库全量 migration 必须成功");
        assertEquals(2, result.migrationsExecuted);
        assertEquals("2", flyway.info().current().getVersion().getVersion());

        try (Connection connection =
                        DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        """
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                          AND table_name IN (
                              'information_item',
                              'job_information',
                              'information_snapshot',
                              'user_account',
                              'ai_prompt_profile',
                              'ai_prompt_version',
                              'information_analysis',
                              'ai_analysis_schedule',
                              'ai_analysis_batch',
                              'ai_analysis_batch_item',
                              'ai_model_invocation'
                          )
                        """)) {
            Set<String> tables = new HashSet<>();
            while (resultSet.next()) {
                tables.add(resultSet.getString(1));
            }
            assertEquals(11, tables.size());
        }
    }

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set for the database integration test");
        }
        return value;
    }
}
