package com.informationplatform.hub.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DatabaseIntegrationTestSafetyTest {

    @Test
    void acceptsDedicatedTestTargets() {
        String testUrl = "jdbc:mysql://127.0.0.1:13306/information_hub_test?serverTimezone=UTC";
        String emptyUrl =
                "jdbc:mysql://127.0.0.1:13306/information_hub_test_task024_empty?serverTimezone=UTC";

        assertEquals(testUrl, DatabaseIntegrationTestSafety.requireTestDatabase(testUrl));
        assertEquals(emptyUrl, DatabaseIntegrationTestSafety.requireEmptyTestDatabase(emptyUrl));
    }

    @Test
    void rejectsDevelopmentAndNonEmptyTargets() {
        assertThrows(
                IllegalStateException.class,
                () -> DatabaseIntegrationTestSafety.requireTestDatabase(
                        "jdbc:mysql://127.0.0.1:13306/information_hub_dev"));
        assertThrows(
                IllegalStateException.class,
                () -> DatabaseIntegrationTestSafety.requireEmptyTestDatabase(
                        "jdbc:mysql://127.0.0.1:13306/information_hub_test"));
    }
}
