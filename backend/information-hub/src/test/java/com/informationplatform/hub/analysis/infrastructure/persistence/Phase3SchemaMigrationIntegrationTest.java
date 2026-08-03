package com.informationplatform.hub.analysis.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.informationplatform.hub.testing.DatabaseIntegrationTestSafety;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** 验证既有测试库从旧版本升级后，Phase 3 物理结构与 Accepted 设计一致。 */
@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_TEST_DB_URL",
        matches = "jdbc:mysql://.+")
class Phase3SchemaMigrationIntegrationTest {

    private static String databaseUrl;
    private static String databaseUsername;
    private static String databasePassword;

    @BeforeAll
    static void migrateExistingSchema() {
        databaseUrl = DatabaseIntegrationTestSafety.requireTestDatabase(
                requiredEnvironmentVariable("INFORMATION_HUB_TEST_DB_URL"));
        databaseUsername = requiredEnvironmentVariable("INFORMATION_HUB_TEST_DB_USERNAME");
        databasePassword = requiredEnvironmentVariable("INFORMATION_HUB_TEST_DB_PASSWORD");

        Flyway flyway = Flyway.configure()
                .dataSource(databaseUrl, databaseUsername, databasePassword)
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .validateOnMigrate(true)
                .load();

        MigrateResult result = flyway.migrate();

        assertTrue(result.success, "既有测试库升级必须成功");
        assertEquals("2", flyway.info().current().getVersion().getVersion());
    }

    @Test
    void migrationCreatesAcceptedTablesIndexesAndUniqueConstraints() throws SQLException {
        try (Connection connection = openConnection()) {
            Set<String> expectedTables = Set.of(
                    "user_account",
                    "ai_prompt_profile",
                    "ai_prompt_version",
                    "information_analysis",
                    "ai_analysis_schedule",
                    "ai_analysis_batch",
                    "ai_analysis_batch_item",
                    "ai_model_invocation");
            Set<String> actualTables = queryStrings(
                    connection,
                    """
                    SELECT table_name
                    FROM information_schema.tables
                    WHERE table_schema = DATABASE()
                      AND table_name IN (
                          'user_account',
                          'ai_prompt_profile',
                          'ai_prompt_version',
                          'information_analysis',
                          'ai_analysis_schedule',
                          'ai_analysis_batch',
                          'ai_analysis_batch_item',
                          'ai_model_invocation'
                      )
                    """);
            assertEquals(expectedTables, actualTables);

            Set<String> indexes = queryStrings(
                    connection,
                    """
                    SELECT DISTINCT index_name
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                    """);
            assertTrue(indexes.containsAll(Set.of(
                    "idx_information_item_type_first_seen_id",
                    "uk_user_account_username",
                    "uk_ai_prompt_profile_user_name",
                    "idx_ai_prompt_profile_user_status",
                    "uk_ai_prompt_version_no",
                    "uk_ai_prompt_version_content",
                    "idx_ai_prompt_version_profile_created",
                    "uk_information_analysis_identity",
                    "idx_information_analysis_user_created",
                    "idx_information_analysis_user_status_created",
                    "idx_information_analysis_information_user",
                    "idx_information_analysis_snapshot_user",
                    "idx_information_analysis_profile_created",
                    "uk_ai_analysis_schedule_user_name",
                    "idx_ai_analysis_schedule_due",
                    "idx_ai_analysis_schedule_user_enabled",
                    "idx_ai_analysis_schedule_profile",
                    "uk_ai_analysis_batch_manual",
                    "uk_ai_analysis_batch_schedule",
                    "idx_ai_analysis_batch_user_created",
                    "idx_ai_analysis_batch_user_status_created",
                    "idx_ai_analysis_batch_schedule_status",
                    "idx_ai_analysis_batch_profile_created",
                    "uk_ai_analysis_batch_item_snapshot",
                    "uk_ai_analysis_batch_item_order",
                    "idx_ai_analysis_batch_item_status",
                    "idx_ai_analysis_batch_item_analysis",
                    "idx_ai_analysis_batch_item_snapshot",
                    "uk_ai_model_invocation_attempt",
                    "idx_ai_model_invocation_user_created",
                    "idx_ai_model_invocation_analysis_created",
                    "idx_ai_model_invocation_batch_item_created",
                    "idx_ai_model_invocation_provider_request")));
        }
    }

    @Test
    void historyForeignKeysRestrictDeletionAndTriggerCheckExists() throws SQLException {
        try (Connection connection = openConnection()) {
            Map<String, String> deleteRules = new HashMap<>();
            try (PreparedStatement statement = connection.prepareStatement(
                            """
                            SELECT constraint_name, delete_rule
                            FROM information_schema.referential_constraints
                            WHERE constraint_schema = DATABASE()
                              AND constraint_name LIKE 'fk_ai_%'
                               OR constraint_schema = DATABASE()
                              AND constraint_name LIKE 'fk_information_analysis_%'
                            """);
                    ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    deleteRules.put(resultSet.getString(1), resultSet.getString(2));
                }
            }

            assertEquals(
                    Set.of(
                            "fk_ai_prompt_profile_user",
                            "fk_ai_prompt_profile_active_version",
                            "fk_ai_prompt_version_profile",
                            "fk_information_analysis_user",
                            "fk_information_analysis_information",
                            "fk_information_analysis_snapshot",
                            "fk_information_analysis_profile",
                            "fk_information_analysis_prompt_version",
                            "fk_ai_analysis_schedule_user",
                            "fk_ai_analysis_schedule_profile",
                            "fk_ai_analysis_batch_user",
                            "fk_ai_analysis_batch_schedule",
                            "fk_ai_analysis_batch_profile",
                            "fk_ai_analysis_batch_prompt_version",
                            "fk_ai_analysis_batch_item_batch",
                            "fk_ai_analysis_batch_item_information",
                            "fk_ai_analysis_batch_item_snapshot",
                            "fk_ai_analysis_batch_item_analysis",
                            "fk_ai_model_invocation_analysis",
                            "fk_ai_model_invocation_user",
                            "fk_ai_model_invocation_batch_item"),
                    deleteRules.keySet());
            assertFalse(deleteRules.isEmpty());
            assertTrue(deleteRules.values().stream().allMatch("RESTRICT"::equals));

            Set<String> checks = queryStrings(
                    connection,
                    """
                    SELECT constraint_name
                    FROM information_schema.table_constraints
                    WHERE constraint_schema = DATABASE()
                      AND table_name = 'ai_analysis_batch'
                      AND constraint_type = 'CHECK'
                    """);
            assertTrue(checks.contains("ck_ai_analysis_batch_trigger_fields"));
        }
    }

    @Test
    void scheduleAndUsageColumnsKeepAcceptedDefaultsAndNullability() throws SQLException {
        try (Connection connection = openConnection()) {
            Map<String, ColumnFact> scheduleColumns = queryColumnFacts(
                    connection,
                    "ai_analysis_schedule",
                    Set.of(
                            "enabled",
                            "local_time",
                            "timezone",
                            "window_days",
                            "max_candidates",
                            "max_estimated_tokens"));
            assertEquals("0", scheduleColumns.get("enabled").defaultValue());
            assertEquals("02:00:00", scheduleColumns.get("local_time").defaultValue());
            assertEquals("Asia/Shanghai", scheduleColumns.get("timezone").defaultValue());
            assertEquals("3", scheduleColumns.get("window_days").defaultValue());
            assertEquals("20", scheduleColumns.get("max_candidates").defaultValue());
            assertEquals("75000", scheduleColumns.get("max_estimated_tokens").defaultValue());

            Map<String, ColumnFact> usageColumns = queryColumnFacts(
                    connection,
                    "ai_model_invocation",
                    Set.of(
                            "input_tokens",
                            "output_tokens",
                            "total_tokens",
                            "cached_input_tokens",
                            "reasoning_tokens",
                            "usage_status"));
            assertEquals("YES", usageColumns.get("input_tokens").nullable());
            assertEquals("YES", usageColumns.get("output_tokens").nullable());
            assertEquals("YES", usageColumns.get("total_tokens").nullable());
            assertEquals("YES", usageColumns.get("cached_input_tokens").nullable());
            assertEquals("YES", usageColumns.get("reasoning_tokens").nullable());
            assertEquals("UNAVAILABLE", usageColumns.get("usage_status").defaultValue());
        }
    }

    private static Map<String, ColumnFact> queryColumnFacts(
            Connection connection, String tableName, Set<String> columnNames) throws SQLException {
        Map<String, ColumnFact> facts = new HashMap<>();
        String placeholders = String.join(",", columnNames.stream().map(ignored -> "?").toList());
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT column_name, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name IN (%s)
                """
                        .formatted(placeholders))) {
            statement.setString(1, tableName);
            int index = 2;
            for (String columnName : columnNames) {
                statement.setString(index++, columnName);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    facts.put(
                            resultSet.getString("column_name"),
                            new ColumnFact(
                                    resultSet.getString("is_nullable"),
                                    resultSet.getString("column_default")));
                }
            }
        }
        assertEquals(columnNames, facts.keySet());
        return facts;
    }

    private static Set<String> queryStrings(Connection connection, String sql) throws SQLException {
        Set<String> values = new HashSet<>();
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                values.add(resultSet.getString(1));
            }
        }
        return values;
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
    }

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set for the database integration test");
        }
        return value;
    }

    private record ColumnFact(
            /** 列是否允许 NULL。 */
            String nullable,
            /** 数据库声明的列默认值。 */
            String defaultValue) {
    }
}
