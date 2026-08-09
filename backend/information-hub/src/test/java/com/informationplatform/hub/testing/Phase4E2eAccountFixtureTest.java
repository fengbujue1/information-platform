package com.informationplatform.hub.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

/** 为 Phase 4 全链路 E2E 在真实测试库中创建一次性登录账号。 */
@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_E2E_FIXTURE_USERNAME",
        matches = ".+")
class Phase4E2eAccountFixtureTest {

    @Test
    void createsRuntimeAccountWithoutPersistingPlaintextPassword() throws Exception {
        String databaseUrl = DatabaseIntegrationTestSafety.requireTestDatabase(
                required("INFORMATION_HUB_TEST_DB_URL"));
        String username = required("INFORMATION_HUB_E2E_FIXTURE_USERNAME").trim().toLowerCase();
        String passwordHash = PasswordEncoderFactories.createDelegatingPasswordEncoder()
                .encode(required("INFORMATION_HUB_E2E_FIXTURE_PASSWORD"));

        try (Connection connection = DriverManager.getConnection(
                        databaseUrl,
                        required("INFORMATION_HUB_TEST_DB_USERNAME"),
                        required("INFORMATION_HUB_TEST_DB_PASSWORD"));
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO user_account (
                            username, password_hash, display_name, timezone, status
                        ) VALUES (?, ?, 'Phase 4 E2E', 'Asia/Shanghai', 'ACTIVE')
                        """)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
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
}
