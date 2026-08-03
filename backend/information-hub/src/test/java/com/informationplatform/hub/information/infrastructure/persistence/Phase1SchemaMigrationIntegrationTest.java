package com.informationplatform.hub.information.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.informationplatform.hub.testing.DatabaseIntegrationTestSafety;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.function.Executable;

@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_TEST_DB_URL",
        matches = "jdbc:mysql://.+")
class Phase1SchemaMigrationIntegrationTest {

    private static final String DB_URL_ENV = "INFORMATION_HUB_TEST_DB_URL";
    private static final String DB_USERNAME_ENV = "INFORMATION_HUB_TEST_DB_USERNAME";
    private static final String DB_PASSWORD_ENV = "INFORMATION_HUB_TEST_DB_PASSWORD";
    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);

    private static String databaseUrl;
    private static String databaseUsername;
    private static String databasePassword;

    @BeforeAll
    static void migrateSchema() {
        databaseUrl = DatabaseIntegrationTestSafety.requireTestDatabase(
                requiredEnvironmentVariable(DB_URL_ENV));
        databaseUsername = requiredEnvironmentVariable(DB_USERNAME_ENV);
        databasePassword = requiredEnvironmentVariable(DB_PASSWORD_ENV);

        Flyway flyway = Flyway.configure()
                .dataSource(databaseUrl, databaseUsername, databasePassword)
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .validateOnMigrate(true)
                .load();

        MigrateResult result = flyway.migrate();

        assertTrue(result.success, "Flyway migration must succeed");
        assertEquals(2, flyway.info().current().getVersion().getMajor().intValue());
    }

    @Test
    void migrationCreatesRequiredTablesIndexesAndForeignKeys() throws SQLException {
        try (Connection connection = openConnection()) {
            Set<String> tables = queryStrings(
                    connection,
                    """
                    SELECT table_name
                    FROM information_schema.tables
                    WHERE table_schema = DATABASE()
                      AND table_name IN ('information_item', 'job_information', 'information_snapshot')
                    """);
            assertEquals(
                    Set.of("information_item", "job_information", "information_snapshot"),
                    tables);

            assertContainsAllColumns(
                    connection,
                    "information_item",
                    Set.of(
                            "id",
                            "information_type",
                            "source",
                            "source_item_id",
                            "current_version_no",
                            "raw_payload",
                            "content_hash"));
            assertContainsAllColumns(
                    connection,
                    "job_information",
                    Set.of(
                            "information_id",
                            "source_recruiter_id",
                            "salary_source",
                            "detail_status",
                            "detail_collected_at",
                            "source_tags",
                            "source_skill_tags"));
            assertContainsAllColumns(
                    connection,
                    "information_snapshot",
                    Set.of(
                            "information_id",
                            "version_no",
                            "content_hash",
                            "standardized_payload",
                            "raw_payload"));

            Set<String> indexes = queryStrings(
                    connection,
                    """
                    SELECT DISTINCT index_name
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name IN ('information_item', 'job_information', 'information_snapshot')
                    """);
            assertTrue(indexes.containsAll(Set.of(
                    "uk_information_item_identity",
                    "uk_information_snapshot_version",
                    "idx_information_snapshot_hash",
                    "idx_job_information_source_company_id",
                    "idx_job_information_source_recruiter_id",
                    "idx_job_information_city_name",
                    "idx_job_information_salary_range",
                    "idx_job_information_status_updated_at")));

            Map<String, String> deleteRules = new HashMap<>();
            try (PreparedStatement statement = connection.prepareStatement(
                            """
                            SELECT constraint_name, delete_rule
                            FROM information_schema.referential_constraints
                            WHERE constraint_schema = DATABASE()
                            """);
                    ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    deleteRules.put(resultSet.getString(1), resultSet.getString(2));
                }
            }
            assertEquals("CASCADE", deleteRules.get("fk_job_information_information_item"));
            assertEquals("RESTRICT", deleteRules.get("fk_information_snapshot_information_item"));
        }
    }

    @Test
    void jsonAndSpecialEncryptIdRoundTripWithoutLoss() throws SQLException {
        withRollback(connection -> {
            String sourceItemId = "encrypt+/=_中文-" + UUID.randomUUID();
            long informationId = insertInformationItem(
                    connection,
                    sourceItemId,
                    "{\"source\":\"BOSS\",\"nested\":{\"value\":\"原始数据\"}}");

            try (PreparedStatement statement = connection.prepareStatement(
                            """
                            SELECT source_item_id,
                                   JSON_UNQUOTE(JSON_EXTRACT(raw_payload, '$.nested.value'))
                            FROM information_item
                            WHERE id = ?
                            """)) {
                statement.setLong(1, informationId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    assertTrue(resultSet.next());
                    assertEquals(sourceItemId, resultSet.getString(1));
                    assertEquals("原始数据", resultSet.getString(2));
                }
            }
        });
    }

    @Test
    void snapshotsAllowIncreasingVersionsAndContentReturningFromAToBToA() throws SQLException {
        withRollback(connection -> {
            long informationId = insertInformationItem(
                    connection,
                    uniqueSourceItemId(),
                    "{\"state\":\"A\"}");
            insertSnapshot(connection, informationId, 1, HASH_A, "{\"state\":\"A\"}");
            insertSnapshot(connection, informationId, 2, HASH_B, "{\"state\":\"B\"}");
            insertSnapshot(connection, informationId, 3, HASH_A, "{\"state\":\"A\"}");

            try (PreparedStatement statement = connection.prepareStatement(
                            """
                            SELECT COUNT(*), COUNT(DISTINCT version_no)
                            FROM information_snapshot
                            WHERE information_id = ?
                            """)) {
                statement.setLong(1, informationId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    assertTrue(resultSet.next());
                    assertEquals(3, resultSet.getInt(1));
                    assertEquals(3, resultSet.getInt(2));
                }
            }

            assertConstraintViolation(
                    () -> insertSnapshot(connection, informationId, 3, HASH_B, "{\"state\":\"B\"}"));
        });
    }

    @Test
    void informationIdentityIsIdempotentAtDatabaseBoundary() throws SQLException {
        withRollback(connection -> {
            String sourceItemId = uniqueSourceItemId();
            insertInformationItem(connection, sourceItemId, "{\"attempt\":1}");

            assertConstraintViolation(
                    () -> insertInformationItem(connection, sourceItemId, "{\"attempt\":2}"));
        });
    }

    @Test
    void snapshotRestrictsParentDeletionWhileJobExtensionCascades() throws SQLException {
        withRollback(connection -> {
            long snapshotParentId = insertInformationItem(
                    connection,
                    uniqueSourceItemId(),
                    "{\"kind\":\"snapshot-parent\"}");
            insertSnapshot(connection, snapshotParentId, 1, HASH_A, "{\"state\":\"A\"}");
            assertConstraintViolation(() -> deleteInformationItem(connection, snapshotParentId));

            long jobParentId = insertInformationItem(
                    connection,
                    uniqueSourceItemId(),
                    "{\"kind\":\"job-parent\"}");
            insertJobInformation(connection, jobParentId);
            deleteInformationItem(connection, jobParentId);

            assertEquals(0, countByInformationId(connection, "job_information", jobParentId));
        });
    }

    @Test
    void explicitTransactionRollbackDoesNotPersistWrites() throws SQLException {
        String sourceItemId = uniqueSourceItemId();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            insertInformationItem(connection, sourceItemId, "{\"rollback\":true}");
            connection.rollback();
        }

        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM information_item WHERE source_item_id = ?")) {
            statement.setString(1, sourceItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(0, resultSet.getInt(1));
            }
        }
    }

    private static void assertContainsAllColumns(
            Connection connection, String tableName, Set<String> expectedColumns) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """)) {
            statement.setString(1, tableName);
            Set<String> actualColumns = new HashSet<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    actualColumns.add(resultSet.getString(1));
                }
            }
            assertTrue(
                    actualColumns.containsAll(expectedColumns),
                    () -> tableName + " is missing columns: "
                            + difference(expectedColumns, actualColumns));
        }
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

    private static long insertInformationItem(
            Connection connection, String sourceItemId, String rawPayload) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO information_item (
                    information_type,
                    source,
                    source_item_id,
                    source_url,
                    title,
                    content,
                    publish_time,
                    collected_at,
                    first_seen_time,
                    last_seen_time,
                    content_hash,
                    current_version_no,
                    raw_payload,
                    schema_version,
                    collector_id,
                    collector_version,
                    collection_context,
                    status
                ) VALUES (
                    'JOB',
                    'BOSS_ZHIPIN',
                    ?,
                    'https://www.zhipin.com/job_detail/example.html',
                    'Java developer',
                    'Example content',
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    1,
                    ?,
                    1,
                    'boss-zhipin-scraper',
                    'test',
                    '{"test":true}',
                    'ACTIVE'
                )
                """,
                Statement.RETURN_GENERATED_KEYS)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            statement.setString(1, sourceItemId);
            statement.setTimestamp(2, now);
            statement.setTimestamp(3, now);
            statement.setTimestamp(4, now);
            statement.setTimestamp(5, now);
            statement.setString(6, HASH_A);
            statement.setString(7, rawPayload);
            assertEquals(1, statement.executeUpdate());
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                assertTrue(generatedKeys.next());
                return generatedKeys.getLong(1);
            }
        }
    }

    private static void insertJobInformation(Connection connection, long informationId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO job_information (
                    information_id,
                    source_company_id,
                    source_recruiter_id,
                    company_name,
                    city_name,
                    salary_min_monthly_yuan,
                    salary_max_monthly_yuan,
                    source_tags,
                    source_skill_tags
                ) VALUES (?, 'company-id', 'recruiter-id', 'Example company', 'Beijing',
                          20000, 30000, '["Java"]', '["Spring"]')
                """)) {
            statement.setLong(1, informationId);
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static void insertSnapshot(
            Connection connection,
            long informationId,
            int versionNo,
            String contentHash,
            String rawPayload)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO information_snapshot (
                    information_id,
                    version_no,
                    content_hash,
                    title,
                    content,
                    standardized_payload,
                    raw_payload,
                    collected_at,
                    collector_id,
                    collector_version
                ) VALUES (?, ?, ?, 'Java developer', 'Example content',
                          '{"informationType":"JOB"}', ?, ?, 'boss-zhipin-scraper', 'test')
                """)) {
            statement.setLong(1, informationId);
            statement.setInt(2, versionNo);
            statement.setString(3, contentHash);
            statement.setString(4, rawPayload);
            statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static void deleteInformationItem(Connection connection, long informationId)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement("DELETE FROM information_item WHERE id = ?")) {
            statement.setLong(1, informationId);
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static int countByInformationId(
            Connection connection, String tableName, long informationId) throws SQLException {
        if (!Set.of("job_information", "information_snapshot").contains(tableName)) {
            throw new IllegalArgumentException("Unexpected table: " + tableName);
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM " + tableName + " WHERE information_id = ?")) {
            statement.setLong(1, informationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getInt(1);
            }
        }
    }

    private static void assertConstraintViolation(Executable executable) {
        SQLException exception = assertThrows(SQLException.class, executable);
        assertNotNull(exception.getSQLState());
        assertTrue(
                exception.getSQLState().startsWith("23"),
                () -> "Expected integrity constraint SQL state but was " + exception.getSQLState());
    }

    private static void withRollback(SqlWork work) throws SQLException {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                work.execute(connection);
            } finally {
                connection.rollback();
            }
        }
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

    private static String uniqueSourceItemId() {
        return "encrypt-job-" + UUID.randomUUID();
    }

    private static Set<String> difference(Set<String> expected, Set<String> actual) {
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(actual);
        return missing;
    }

    @FunctionalInterface
    private interface SqlWork {

        void execute(Connection connection) throws SQLException;
    }
}
