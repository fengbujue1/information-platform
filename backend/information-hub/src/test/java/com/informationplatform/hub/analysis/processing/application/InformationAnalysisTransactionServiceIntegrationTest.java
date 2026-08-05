package com.informationplatform.hub.analysis.processing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptVersionMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptVersionPo;
import com.informationplatform.hub.analysis.processing.domain.AnalysisPreparation;
import com.informationplatform.hub.analysis.processing.domain.InformationAnalysisView;
import com.informationplatform.hub.analysis.provider.application.AiProviderClient;
import com.informationplatform.hub.analysis.provider.domain.AiProviderResult;
import com.informationplatform.hub.analysis.provider.domain.AiProviderUsage;
import com.informationplatform.hub.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.informationplatform.hub.identity.infrastructure.persistence.po.UserAccountPo;
import com.informationplatform.hub.information.infrastructure.persistence.mapper.InformationItemMapper;
import com.informationplatform.hub.information.infrastructure.persistence.mapper.InformationSnapshotMapper;
import com.informationplatform.hub.information.infrastructure.persistence.po.InformationItemPo;
import com.informationplatform.hub.information.infrastructure.persistence.po.InformationSnapshotPo;
import com.informationplatform.hub.testing.DatabaseIntegrationTestSafety;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** 使用真实 MySQL 验证 Analysis 幂等、失败重试、Actual Usage 和 Owner 隔离。 */
@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_TEST_DB_URL",
        matches = "jdbc:mysql://.+")
class InformationAnalysisTransactionServiceIntegrationTest {

    private static final String HASH = "c".repeat(64);

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        String databaseUrl = DatabaseIntegrationTestSafety.requireTestDatabase(
                required("INFORMATION_HUB_TEST_DB_URL"));
        registry.add("spring.datasource.url", () -> databaseUrl);
        registry.add("spring.datasource.username", () -> required("INFORMATION_HUB_TEST_DB_USERNAME"));
        registry.add("spring.datasource.password", () -> required("INFORMATION_HUB_TEST_DB_PASSWORD"));
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private InformationAnalysisTransactionService transactions;

    @Autowired
    private UserAccountMapper userMapper;

    @Autowired
    private AiPromptProfileMapper profileMapper;

    @Autowired
    private AiPromptVersionMapper versionMapper;

    @Autowired
    private InformationItemMapper informationMapper;

    @Autowired
    private InformationSnapshotMapper snapshotMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiProviderClient provider;

    @BeforeEach
    void providerMetadata() {
        when(provider.providerId()).thenReturn("FAKE");
        when(provider.modelName()).thenReturn("fake-model");
        doNothing().when(provider).validateRequest(any());
    }

    @Test
    @Transactional
    void persistsUsageRetriesFailedIdentityAndReusesSuccess() {
        UserAccountPo owner = insertUser("owner");
        UserAccountPo other = insertUser("other");
        AiPromptProfilePo profile = insertProfile(owner.getId());
        AiPromptVersionPo version = insertVersion(profile.getId());
        profile.setActiveVersionId(version.getId());
        assertEquals(1, profileMapper.updateById(profile));
        InformationItemPo information = insertInformation();
        InformationSnapshotPo snapshot = insertSnapshot(information.getId());

        AnalysisPreparation first = transactions.prepare(
                owner.getId(), information.getId(), null, profile.getId(), false);
        assertThat(first.executionPlan()).isNotNull();
        assertThat(first.executionPlan().providerRequest().messages()).hasSize(3);

        AiProviderResult providerResult = result(10L, 2L, 12L);
        InformationAnalysisView failed = transactions.completeOutputFailure(
                first.executionPlan(), providerResult, "AI_RESPONSE_INVALID_JSON", "invalid JSON");
        assertThat(failed.status()).isEqualTo("FAILED");
        assertThat(failed.invocations()).singleElement().satisfies(invocation -> {
            assertThat(invocation.status()).isEqualTo("SUCCEEDED");
            assertThat(invocation.totalTokens()).isEqualTo(12);
        });
        assertThatThrownBy(() -> transactions.prepare(
                        owner.getId(), information.getId(), snapshot.getId(), profile.getId(), false))
                .isInstanceOf(AnalysisConflictException.class)
                .hasMessageContaining("explicit retry");

        AnalysisPreparation retry = transactions.prepare(
                owner.getId(), information.getId(), snapshot.getId(), profile.getId(), true);
        assertThat(retry.executionPlan().invocationId()).isNotEqualTo(first.executionPlan().invocationId());
        var output = objectMapper.createObjectNode()
                .put("schemaVersion", 1)
                .put("relevanceScore", 82)
                .put("summary", "相关");
        InformationAnalysisView succeeded = transactions.completeSuccess(
                retry.executionPlan(), result(20L, 4L, 24L), output, 82, "相关");
        assertThat(succeeded.status()).isEqualTo("SUCCEEDED");
        assertThat(succeeded.invocations()).extracting(invocation -> invocation.attemptNo())
                .containsExactly(1, 2);

        AnalysisPreparation reused = transactions.prepare(
                owner.getId(), information.getId(), null, profile.getId(), false);
        assertThat(reused.reused().id()).isEqualTo(succeeded.id());
        assertThatThrownBy(() -> transactions.getOwned(other.getId(), succeeded.id()))
                .isInstanceOf(AnalysisNotFoundException.class);
    }

    private UserAccountPo insertUser(String label) {
        UserAccountPo po = new UserAccountPo();
        po.setUsername("task027-" + label + "-" + UUID.randomUUID());
        po.setPasswordHash("$2a$12$task027-not-a-real-secret");
        po.setDisplayName("TASK-027 " + label);
        po.setTimezone("Asia/Shanghai");
        po.setStatus("ACTIVE");
        assertEquals(1, userMapper.insert(po));
        return po;
    }

    private AiPromptProfilePo insertProfile(long userId) {
        AiPromptProfilePo po = new AiPromptProfilePo();
        po.setUserId(userId);
        po.setName("TASK-027 " + UUID.randomUUID());
        po.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        po.setStatus("ACTIVE");
        assertEquals(1, profileMapper.insert(po));
        return po;
    }

    private AiPromptVersionPo insertVersion(long profileId) {
        AiPromptVersionPo po = new AiPromptVersionPo();
        po.setPromptProfileId(profileId);
        po.setVersionNo(1);
        po.setContent("关注 Java 后端岗位");
        po.setContentHash(HASH);
        assertEquals(1, versionMapper.insert(po));
        return po;
    }

    private InformationItemPo insertInformation() {
        LocalDateTime now = LocalDateTime.now();
        InformationItemPo po = new InformationItemPo();
        po.setInformationType("JOB");
        po.setSource("TASK027_TEST");
        po.setSourceItemId(UUID.randomUUID().toString());
        po.setTitle("Java 后端工程师");
        po.setContent("Spring Boot 与 MySQL");
        po.setCollectedAt(now);
        po.setFirstSeenTime(now);
        po.setLastSeenTime(now);
        po.setContentHash(HASH);
        po.setCurrentVersionNo(1);
        po.setRawPayload("{\"source\":\"test\"}");
        po.setSchemaVersion(1);
        po.setCollectorId("task027-test");
        po.setCollectorVersion("1.0");
        assertEquals(1, informationMapper.insert(po));
        return po;
    }

    private InformationSnapshotPo insertSnapshot(long informationId) {
        InformationSnapshotPo po = new InformationSnapshotPo();
        po.setInformationId(informationId);
        po.setVersionNo(1);
        po.setContentHash(HASH);
        po.setTitle("Java 后端工程师");
        po.setContent("Spring Boot 与 MySQL");
        po.setStandardizedPayload("{\"job\":{\"companyName\":\"Example\"}}");
        po.setRawPayload("{\"source\":\"test\"}");
        po.setCollectedAt(LocalDateTime.now());
        po.setCollectorId("task027-test");
        po.setCollectorVersion("1.0");
        assertEquals(1, snapshotMapper.insert(po));
        return po;
    }

    private AiProviderResult result(long input, long output, long total) {
        return new AiProviderResult(
                "FAKE", "fake-model", UUID.randomUUID().toString(), "{}", "stop", 3,
                AiProviderUsage.reported(input, output, total, null, null));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
