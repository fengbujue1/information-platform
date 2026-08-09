package com.informationplatform.hub.recommendation.run.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.batch.application.AnalysisBatchTerminalEventPublisher;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptVersionMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptVersionPo;
import com.informationplatform.hub.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.informationplatform.hub.identity.infrastructure.persistence.po.UserAccountPo;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.RecommendationRunMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.UserRecommendationProfileMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.RecommendationRunPo;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.UserRecommendationProfilePo;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.mapper.JobRecommendationProfileMapper;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.po.JobRecommendationProfilePo;
import com.informationplatform.hub.testing.DatabaseIntegrationTestSafety;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/** 使用真实 MySQL 验证 AFTER_COMMIT Auto Trigger 与 source Batch 唯一幂等。 */
@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_TEST_DB_URL",
        matches = "jdbc:mysql://.+")
class RecommendationAutoTriggerIntegrationTest {

    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        String databaseUrl = DatabaseIntegrationTestSafety.requireTestDatabase(
                required("INFORMATION_HUB_TEST_DB_URL"));
        registry.add("spring.datasource.url", () -> databaseUrl);
        registry.add("spring.datasource.username", () -> required("INFORMATION_HUB_TEST_DB_USERNAME"));
        registry.add("spring.datasource.password", () -> required("INFORMATION_HUB_TEST_DB_PASSWORD"));
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired private UserAccountMapper userMapper;
    @Autowired private AiPromptProfileMapper promptProfileMapper;
    @Autowired private AiPromptVersionMapper promptVersionMapper;
    @Autowired private AiAnalysisBatchMapper batchMapper;
    @Autowired private UserRecommendationProfileMapper profileMapper;
    @Autowired private JobRecommendationProfileMapper jobProfileMapper;
    @Autowired private RecommendationRunMapper runMapper;
    @Autowired private AnalysisBatchTerminalEventPublisher terminalEvents;
    @Autowired private TransactionTemplate transactions;

    @Test
    void createsOneAutoRunOnlyAfterCommitAndKeepsDuplicateEventIdempotent() throws Exception {
        Fixture fixture = transactions.execute(status -> insertFixture());
        assertThat(fixture).isNotNull();
        try {
            transactions.executeWithoutResult(status -> terminalEvents.publishIfTerminal(
                    batchMapper.selectById(fixture.batchId())));

            RecommendationRunPo created =
                    runMapper.selectBySourceAnalysisBatchId(fixture.batchId());
            assertThat(created).isNotNull();
            assertThat(created.getTriggerType()).isEqualTo("ANALYSIS_BATCH_COMPLETED");
            assertThat(created.getStatus()).isEqualTo("PENDING");
            assertThat(created.getUserId()).isEqualTo(fixture.userId());
            assertThat(created.getPromptProfileId()).isEqualTo(fixture.promptProfileId());
            assertThat(created.getPromptVersionId()).isEqualTo(fixture.promptVersionId());
            assertThat(new ObjectMapper()
                            .readTree(created.getProfileSnapshotJson())
                            .path("informationType")
                            .asText())
                    .isEqualTo("JOB");

            transactions.executeWithoutResult(status -> terminalEvents.publishIfTerminal(
                    batchMapper.selectById(fixture.batchId())));

            assertThat(runMapper.selectBySourceAnalysisBatchId(fixture.batchId()).getId())
                    .isEqualTo(created.getId());
        } finally {
            transactions.executeWithoutResult(status -> deleteFixture(fixture));
        }
    }

    private Fixture insertFixture() {
        UserAccountPo user = new UserAccountPo();
        user.setUsername("task041-" + UUID.randomUUID());
        user.setPasswordHash("$2a$12$task041-not-a-real-secret");
        user.setTimezone("Asia/Shanghai");
        assertEquals(1, userMapper.insert(user));

        AiPromptProfilePo promptProfile = new AiPromptProfilePo();
        promptProfile.setUserId(user.getId());
        promptProfile.setName("TASK-041 " + UUID.randomUUID());
        promptProfile.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        assertEquals(1, promptProfileMapper.insert(promptProfile));

        AiPromptVersionPo promptVersion = new AiPromptVersionPo();
        promptVersion.setPromptProfileId(promptProfile.getId());
        promptVersion.setVersionNo(1);
        promptVersion.setContent("TASK-041 integration prompt");
        promptVersion.setContentHash(HASH_A);
        assertEquals(1, promptVersionMapper.insert(promptVersion));

        UserRecommendationProfilePo profile = new UserRecommendationProfilePo();
        profile.setUserId(user.getId());
        profile.setInformationType("JOB");
        profile.setAnalysisPromptProfileId(promptProfile.getId());
        profile.setWindowDays(7);
        profile.setTopN(50);
        profile.setContentHash(HASH_B);
        assertEquals(1, profileMapper.insert(profile));

        JobRecommendationProfilePo extension = new JobRecommendationProfilePo();
        extension.setProfileId(profile.getId());
        extension.setTargetRoles("[\"Java 后端\"]");
        extension.setPreferredSkills("[\"Java\"]");
        extension.setExcludedKeywords("[]");
        assertEquals(1, jobProfileMapper.insert(extension));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        AiAnalysisBatchPo batch = new AiAnalysisBatchPo();
        batch.setUserId(user.getId());
        batch.setTriggerType("MANUAL");
        batch.setManualRequestId(UUID.randomUUID().toString());
        batch.setInformationType("JOB");
        batch.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        batch.setAnalysisDefinitionVersion(2);
        batch.setPromptProfileId(promptProfile.getId());
        batch.setPromptVersionId(promptVersion.getId());
        batch.setRequestedWindowDays(7);
        batch.setWindowStart(now.minusDays(7));
        batch.setWindowEnd(now);
        batch.setRequestedMaxCandidates(50);
        batch.setRequestedTokenBudget(75_000L);
        batch.setStatus("COMPLETED");
        batch.setCompletedAt(now);
        assertEquals(1, batchMapper.insert(batch));

        return new Fixture(
                user.getId(),
                promptProfile.getId(),
                promptVersion.getId(),
                profile.getId(),
                batch.getId());
    }

    private void deleteFixture(Fixture fixture) {
        RecommendationRunPo run = runMapper.selectBySourceAnalysisBatchId(fixture.batchId());
        if (run != null) {
            runMapper.deleteById(run.getId());
        }
        batchMapper.deleteById(fixture.batchId());
        jobProfileMapper.deleteById(fixture.profileId());
        profileMapper.deleteById(fixture.profileId());
        promptVersionMapper.deleteById(fixture.promptVersionId());
        promptProfileMapper.deleteById(fixture.promptProfileId());
        userMapper.deleteById(fixture.userId());
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    private record Fixture(
            /** Owner 用户主键。 */ long userId,
            /** Batch/Profile 绑定的 Prompt Profile 主键。 */ long promptProfileId,
            /** Batch 冻结的 Prompt Version 主键。 */ long promptVersionId,
            /** Recommendation Profile Core 主键。 */ long profileId,
            /** 已完成 Analysis Batch 主键。 */ long batchId) {
    }
}
