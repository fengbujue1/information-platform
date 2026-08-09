package com.informationplatform.hub.recommendation.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** 验证既有测试库升级至 V3 后的 Recommendation 物理结构与查询索引。 */
@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_TEST_DB_URL",
        matches = "jdbc:mysql://.+")
class Phase4SchemaMigrationIntegrationTest {

    private static String databaseUrl;
    private static String databaseUsername;
    private static String databasePassword;

    @BeforeAll
    static void migrateExistingSchema() {
        databaseUrl = DatabaseIntegrationTestSafety.requireTestDatabase(
                required("INFORMATION_HUB_TEST_DB_URL"));
        databaseUsername = required("INFORMATION_HUB_TEST_DB_USERNAME");
        databasePassword = required("INFORMATION_HUB_TEST_DB_PASSWORD");
        Flyway flyway = Flyway.configure()
                .dataSource(databaseUrl, databaseUsername, databasePassword)
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .validateOnMigrate(true)
                .load();

        MigrateResult result = flyway.migrate();

        assertTrue(result.success, "既有测试库升级至 V3 必须成功");
        assertEquals("3", flyway.info().current().getVersion().getVersion());
    }

    @Test
    void createsAcceptedTablesDefaultsAndIndexes() throws SQLException {
        try (Connection connection = openConnection()) {
            assertEquals(
                    Set.of(
                            "user_recommendation_profile",
                            "user_information_interaction",
                            "recommendation_run",
                            "recommendation_item"),
                    queryStrings(
                            connection,
                            """
                            SELECT table_name
                            FROM information_schema.tables
                            WHERE table_schema = DATABASE()
                              AND table_name IN (
                                  'user_recommendation_profile',
                                  'user_information_interaction',
                                  'recommendation_run',
                                  'recommendation_item'
                              )
                            """));

            Map<String, ColumnFact> profileDefaults = queryColumnFacts(
                    connection,
                    "user_recommendation_profile",
                    Set.of("window_days", "top_n"));
            assertEquals("7", profileDefaults.get("window_days").defaultValue());
            assertEquals("50", profileDefaults.get("top_n").defaultValue());

            Map<String, ColumnFact> interactionDefaults = queryColumnFacts(
                    connection,
                    "user_information_interaction",
                    Set.of("view_count", "feedback_state", "job_disposition"));
            assertEquals("0", interactionDefaults.get("view_count").defaultValue());
            assertEquals("NONE", interactionDefaults.get("feedback_state").defaultValue());
            assertEquals("NONE", interactionDefaults.get("job_disposition").defaultValue());

            Map<String, ColumnFact> runDefaults = queryColumnFacts(
                    connection,
                    "recommendation_run",
                    Set.of("candidate_count", "eligible_count", "result_count", "status"));
            assertEquals("0", runDefaults.get("candidate_count").defaultValue());
            assertEquals("0", runDefaults.get("eligible_count").defaultValue());
            assertEquals("0", runDefaults.get("result_count").defaultValue());
            assertEquals("PENDING", runDefaults.get("status").defaultValue());

            Set<String> indexes = queryStrings(
                    connection,
                    """
                    SELECT DISTINCT index_name
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name IN (
                          'user_recommendation_profile',
                          'user_information_interaction',
                          'recommendation_run',
                          'recommendation_item'
                      )
                    """);
            assertTrue(indexes.containsAll(Set.of(
                    "uk_user_recommendation_profile_user",
                    "idx_user_recommendation_profile_prompt",
                    "uk_user_information_interaction_identity",
                    "idx_user_information_interaction_state",
                    "idx_user_information_interaction_information",
                    "uk_recommendation_run_source_batch",
                    "idx_recommendation_run_user_status_created",
                    "idx_recommendation_run_user_completed",
                    "uk_recommendation_item_information",
                    "uk_recommendation_item_rank",
                    "idx_recommendation_item_information",
                    "idx_recommendation_item_analysis")));

            Map<String, String> uniqueColumns = queryUniqueIndexColumns(connection);
            assertEquals("user_id", uniqueColumns.get("uk_user_recommendation_profile_user"));
            assertEquals(
                    "user_id,information_id",
                    uniqueColumns.get("uk_user_information_interaction_identity"));
            assertEquals(
                    "source_analysis_batch_id",
                    uniqueColumns.get("uk_recommendation_run_source_batch"));
            assertEquals(
                    "run_id,information_id",
                    uniqueColumns.get("uk_recommendation_item_information"));
            assertEquals("run_id,rank_no", uniqueColumns.get("uk_recommendation_item_rank"));
        }
    }

    @Test
    void allRecommendationForeignKeysUseRestrictIncludingLastItemAttribution()
            throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        SELECT constraint_name, delete_rule
                        FROM information_schema.referential_constraints
                        WHERE constraint_schema = DATABASE()
                          AND (
                              constraint_name LIKE 'fk_user_recommendation_profile_%'
                              OR constraint_name LIKE 'fk_user_information_interaction_%'
                              OR constraint_name LIKE 'fk_recommendation_run_%'
                              OR constraint_name LIKE 'fk_recommendation_item_%'
                          )
                        """);
                ResultSet resultSet = statement.executeQuery()) {
            Map<String, String> deleteRules = new HashMap<>();
            while (resultSet.next()) {
                deleteRules.put(resultSet.getString(1), resultSet.getString(2));
            }
            assertEquals(14, deleteRules.size());
            assertTrue(deleteRules.values().stream().allMatch("RESTRICT"::equals));
            assertEquals(
                    "RESTRICT",
                    deleteRules.get("fk_user_information_interaction_last_item"));
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
                """.formatted(placeholders))) {
            statement.setString(1, tableName);
            int parameter = 2;
            for (String columnName : columnNames) {
                statement.setString(parameter++, columnName);
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

    private static Map<String, String> queryUniqueIndexColumns(Connection connection)
            throws SQLException {
        Map<String, String> indexes = new HashMap<>();
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        """
                        SELECT index_name,
                               GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')
                        FROM information_schema.statistics
                        WHERE table_schema = DATABASE()
                          AND table_name IN (
                              'user_recommendation_profile',
                              'user_information_interaction',
                              'recommendation_run',
                              'recommendation_item'
                          )
                          AND non_unique = 0
                          AND index_name <> 'PRIMARY'
                        GROUP BY index_name
                        """)) {
            while (resultSet.next()) {
                indexes.put(resultSet.getString(1), resultSet.getString(2));
            }
        }
        return indexes;
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
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
