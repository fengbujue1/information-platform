package com.informationplatform.hub.recommendation.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.informationplatform.hub.testing.DatabaseIntegrationTestSafety;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** 使用真实 V3 数据图验证 V4 的复制、回填与无损拆表。 */
@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_TEST_DB_URL",
        matches = "jdbc:mysql://.+")
class Phase4GeneralizationMigrationIntegrationTest {

    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);

    @Test
    void migratesExistingV3JobDataIntoGenericCoreAndJobExtensions() throws SQLException {
        String databaseUrl = DatabaseIntegrationTestSafety.requireTestDatabase(
                required("INFORMATION_HUB_TEST_DB_URL"));
        String username = required("INFORMATION_HUB_TEST_DB_USERNAME");
        String password = required("INFORMATION_HUB_TEST_DB_PASSWORD");
        Flyway flyway = Flyway.configure()
                .dataSource(databaseUrl, username, password)
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .validateOnMigrate(true)
                .load();
        MigrationInfo current = flyway.info().current();
        assumeTrue(
                current != null && "3".equals(current.getVersion().getVersion()),
                "该数据迁移场景只在专用测试库仍处于真实 V3 时执行");

        LegacyGraph graph;
        try (Connection connection = DriverManager.getConnection(databaseUrl, username, password)) {
            graph = insertLegacyGraph(connection);
        }

        MigrateResult result = flyway.migrate();

        assertTrue(result.success);
        assertEquals(1, result.migrationsExecuted);
        assertEquals("4", flyway.info().current().getVersion().getVersion());

        try (Connection connection = DriverManager.getConnection(databaseUrl, username, password)) {
            verifyMigratedGraph(connection, graph);
            deleteMigratedGraph(connection, graph);
        }
    }

    /** 创建包含 Profile 偏好、JOB disposition 与 Run snapshot 的最小合法 V3 数据图。 */
    private LegacyGraph insertLegacyGraph(Connection connection) throws SQLException {
        String marker = UUID.randomUUID().toString();
        long userId = insertAndReturnId(
                connection,
                "INSERT INTO user_account (username, password_hash, timezone) VALUES (?, ?, ?)",
                "task034a-" + marker,
                "$2a$12$task034a-not-a-real-secret",
                "Asia/Shanghai");
        long promptProfileId = insertAndReturnId(
                connection,
                """
                INSERT INTO ai_prompt_profile (user_id, name, analysis_definition_key)
                VALUES (?, ?, 'JOB_USER_RELEVANCE')
                """,
                userId,
                "TASK-034A " + marker);
        long promptVersionId = insertAndReturnId(
                connection,
                """
                INSERT INTO ai_prompt_version
                    (prompt_profile_id, version_no, content, content_hash)
                VALUES (?, 1, ?, ?)
                """,
                promptProfileId,
                "TASK-034A migration prompt",
                HASH_A);
        long informationId = insertAndReturnId(
                connection,
                """
                INSERT INTO information_item (
                    information_type, source, source_item_id, title,
                    collected_at, first_seen_time, last_seen_time,
                    content_hash, current_version_no, raw_payload,
                    schema_version, collector_id, collector_version
                ) VALUES ('JOB', 'TASK034A_TEST', ?, 'V3 Job', ?, ?, ?, ?, 1,
                          JSON_OBJECT('test', true), 1, 'task034a-test', '1.0')
                """,
                marker,
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now()),
                HASH_A);
        long profileId = insertAndReturnId(
                connection,
                """
                INSERT INTO user_recommendation_profile (
                    user_id, analysis_prompt_profile_id, window_days, top_n,
                    target_roles, preferred_skills, preferred_cities,
                    preferred_remote_types, salary_min_monthly_yuan,
                    excluded_keywords, content_hash
                ) VALUES (?, ?, 7, 50, JSON_ARRAY('Java 后端'),
                          JSON_ARRAY('Java', 'Spring Boot'), JSON_ARRAY('成都'),
                          JSON_ARRAY('REMOTE'), 15000, JSON_ARRAY('纯销售'), ?)
                """,
                userId,
                promptProfileId,
                HASH_B);
        long interactionId = insertAndReturnId(
                connection,
                """
                INSERT INTO user_information_interaction (
                    user_id, information_id, feedback_state, feedback_updated_at,
                    job_disposition, disposition_updated_at
                ) VALUES (?, ?, 'INTERESTED', ?, 'CONTACTED_NOT_SUITABLE', ?)
                """,
                userId,
                informationId,
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now()));
        LocalDateTime windowEnd = LocalDateTime.now();
        long runId = insertAndReturnId(
                connection,
                """
                INSERT INTO recommendation_run (
                    user_id, trigger_type, profile_id, profile_content_hash,
                    profile_snapshot_json, prompt_profile_id, prompt_version_id,
                    algorithm_key, algorithm_version, window_start, window_end
                ) VALUES (?, 'MANUAL', ?, ?,
                          JSON_OBJECT('windowDays', 7, 'targetRoles', JSON_ARRAY('Java 后端')),
                          ?, ?, 'JOB_RECOMMENDATION', 1, ?, ?)
                """,
                userId,
                profileId,
                HASH_B,
                promptProfileId,
                promptVersionId,
                Timestamp.valueOf(windowEnd.minusDays(7)),
                Timestamp.valueOf(windowEnd));
        return new LegacyGraph(
                userId,
                promptProfileId,
                promptVersionId,
                informationId,
                profileId,
                interactionId,
                runId);
    }

    private void verifyMigratedGraph(Connection connection, LegacyGraph graph) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT core.information_type,
                       core.content_hash,
                       JSON_UNQUOTE(JSON_EXTRACT(job.target_roles, '$[0]')) AS target_role,
                       job.salary_min_monthly_yuan
                FROM user_recommendation_profile core
                JOIN job_recommendation_profile job ON job.profile_id = core.id
                WHERE core.id = ?
                """)) {
            statement.setLong(1, graph.profileId());
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals("JOB", resultSet.getString("information_type"));
                assertEquals(HASH_B, resultSet.getString("content_hash"));
                assertEquals("Java 后端", resultSet.getString("target_role"));
                assertEquals(15000, resultSet.getInt("salary_min_monthly_yuan"));
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT interaction_state.feedback_state,
                       disposition.job_disposition,
                       disposition.disposition_updated_at
                FROM user_information_interaction interaction_state
                JOIN user_job_disposition disposition
                    ON disposition.interaction_id = interaction_state.id
                WHERE interaction_state.id = ?
                """)) {
            statement.setLong(1, graph.interactionId());
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals("INTERESTED", resultSet.getString("feedback_state"));
                assertEquals("CONTACTED_NOT_SUITABLE", resultSet.getString("job_disposition"));
                assertTrue(resultSet.getTimestamp("disposition_updated_at") != null);
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT information_type,
                       JSON_UNQUOTE(JSON_EXTRACT(profile_snapshot_json, '$.informationType'))
                           AS snapshot_information_type
                FROM recommendation_run
                WHERE id = ?
                """)) {
            statement.setLong(1, graph.runId());
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals("JOB", resultSet.getString("information_type"));
                assertEquals("JOB", resultSet.getString("snapshot_information_type"));
            }
        }
    }

    /** 按 V4 RESTRICT 依赖反序清理本测试写入的数据，不清理或重建数据库。 */
    private void deleteMigratedGraph(Connection connection, LegacyGraph graph) throws SQLException {
        delete(connection, "DELETE FROM recommendation_run WHERE id = ?", graph.runId());
        delete(connection, "DELETE FROM user_job_disposition WHERE interaction_id = ?", graph.interactionId());
        delete(connection, "DELETE FROM user_information_interaction WHERE id = ?", graph.interactionId());
        delete(connection, "DELETE FROM job_recommendation_profile WHERE profile_id = ?", graph.profileId());
        delete(connection, "DELETE FROM user_recommendation_profile WHERE id = ?", graph.profileId());
        delete(connection, "DELETE FROM ai_prompt_version WHERE id = ?", graph.promptVersionId());
        delete(connection, "DELETE FROM ai_prompt_profile WHERE id = ?", graph.promptProfileId());
        delete(connection, "DELETE FROM information_item WHERE id = ?", graph.informationId());
        delete(connection, "DELETE FROM user_account WHERE id = ?", graph.userId());
    }

    private long insertAndReturnId(Connection connection, String sql, Object... arguments)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int index = 0; index < arguments.length; index++) {
                statement.setObject(index + 1, arguments[index]);
            }
            assertEquals(1, statement.executeUpdate());
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                assertTrue(generatedKeys.next());
                return generatedKeys.getLong(1);
            }
        }
    }

    private void delete(Connection connection, String sql, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    private record LegacyGraph(
            /** V3 用户主键。 */ long userId,
            /** V3 Prompt Profile 主键。 */ long promptProfileId,
            /** V3 Prompt Version 主键。 */ long promptVersionId,
            /** V3 JOB Information 主键。 */ long informationId,
            /** V3 Recommendation Profile 主键。 */ long profileId,
            /** V3 Interaction 主键。 */ long interactionId,
            /** V3 Recommendation Run 主键。 */ long runId) {
    }
}
