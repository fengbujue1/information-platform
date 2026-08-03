package com.informationplatform.hub.analysis.prompt.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.informationplatform.hub.analysis.prompt.domain.PromptProfile;
import com.informationplatform.hub.analysis.prompt.domain.PromptProfileStatus;
import com.informationplatform.hub.analysis.prompt.domain.PromptVersion;
import com.informationplatform.hub.analysis.prompt.domain.PromptVersionChange;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.identity.domain.AuthenticatedUser;
import com.informationplatform.hub.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.informationplatform.hub.identity.infrastructure.persistence.po.UserAccountPo;
import com.informationplatform.hub.testing.DatabaseIntegrationTestSafety;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** 使用真实 MySQL 约束验证 Prompt 版本复用、激活切换和 Owner 隔离。 */
@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_TEST_DB_URL",
        matches = "jdbc:mysql://.+")
class PromptProfileServiceIntegrationTest {

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        String databaseUrl = DatabaseIntegrationTestSafety.requireTestDatabase(
                required("INFORMATION_HUB_TEST_DB_URL"));
        registry.add("spring.datasource.url", () -> databaseUrl);
        registry.add(
                "spring.datasource.username",
                () -> required("INFORMATION_HUB_TEST_DB_USERNAME"));
        registry.add(
                "spring.datasource.password",
                () -> required("INFORMATION_HUB_TEST_DB_PASSWORD"));
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private PromptProfileService service;

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @Test
    @Transactional
    void persistsMultipleProfilesImmutableVersionsAndOwnerIsolation() {
        UserAccountPo firstUser = insertUser("first");
        UserAccountPo secondUser = insertUser("second");
        useUser(firstUser);

        PromptProfile primary =
                service.createProfile("Primary", PromptProfileService.JOB_USER_RELEVANCE);
        PromptProfile secondary =
                service.createProfile("Secondary", PromptProfileService.JOB_USER_RELEVANCE);
        assertEquals(2, service.listProfiles().size());

        PromptVersionChange first = service.createVersion(primary.id(), "first prompt");
        PromptVersionChange reused = service.createVersion(primary.id(), "first prompt");
        PromptVersionChange second = service.createVersion(primary.id(), "second prompt");

        assertTrue(first.created());
        assertFalse(reused.created());
        assertEquals(first.version().id(), reused.version().id());
        assertTrue(second.created());
        assertEquals(2, second.version().versionNo());
        assertEquals(second.version().id(), service.getProfile(primary.id()).activeVersionId());

        List<PromptVersion> versions = service.listVersions(primary.id());
        assertEquals(List.of(2, 1), versions.stream().map(PromptVersion::versionNo).toList());

        PromptProfile reactivated = service.activateVersion(primary.id(), first.version().id());
        assertEquals(first.version().id(), reactivated.activeVersionId());
        jdbcTemplate.update(
                "UPDATE ai_prompt_profile "
                        + "SET updated_at = updated_at - INTERVAL 1 DAY WHERE id = ?",
                secondary.id());
        PromptProfile beforeStatusUpdate = service.getProfile(secondary.id());
        PromptProfile disabled =
                service.updateStatus(secondary.id(), PromptProfileStatus.DISABLED.name());
        assertEquals(PromptProfileStatus.DISABLED, disabled.status());
        assertTrue(disabled.updatedAt().isAfter(beforeStatusUpdate.updatedAt()));

        // 切换 Session Owner 后不得枚举或探测另一账号的 Profile。
        useUser(secondUser);
        assertTrue(service.listProfiles().isEmpty());
        PromptNotFoundException exception = assertThrows(
                PromptNotFoundException.class, () -> service.getProfile(primary.id()));
        assertEquals("PROMPT_PROFILE_NOT_FOUND", exception.getCode());
    }

    private UserAccountPo insertUser(String label) {
        UserAccountPo po = new UserAccountPo();
        po.setUsername("task022-" + label + "-" + UUID.randomUUID());
        po.setPasswordHash("$2a$12$task022-not-a-real-bootstrap-secret");
        po.setDisplayName("TASK-022 " + label);
        po.setTimezone("Asia/Shanghai");
        po.setStatus("ACTIVE");
        assertEquals(1, userAccountMapper.insert(po));
        assertNotNull(po.getId());
        return po;
    }

    private void useUser(UserAccountPo user) {
        when(currentUserProvider.requireCurrentUser()).thenReturn(new AuthenticatedUser(
                user.getId(), user.getUsername(), user.getDisplayName(), user.getTimezone()));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
