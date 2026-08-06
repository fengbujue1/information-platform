package com.informationplatform.hub.analysis.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchItemMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisScheduleMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiModelInvocationMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptVersionMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.InformationAnalysisMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchItemPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisSchedulePo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiModelInvocationPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptVersionPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.InformationAnalysisPo;
import com.informationplatform.hub.analysis.usage.infrastructure.persistence.AnalysisUsageMapper;
import com.informationplatform.hub.analysis.usage.infrastructure.persistence.UserUsageAggregateRow;
import com.informationplatform.hub.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.informationplatform.hub.identity.infrastructure.persistence.po.UserAccountPo;
import com.informationplatform.hub.information.infrastructure.persistence.mapper.InformationItemMapper;
import com.informationplatform.hub.information.infrastructure.persistence.mapper.InformationSnapshotMapper;
import com.informationplatform.hub.information.infrastructure.persistence.po.InformationItemPo;
import com.informationplatform.hub.information.infrastructure.persistence.po.InformationSnapshotPo;
import com.informationplatform.hub.testing.DatabaseIntegrationTestSafety;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

/** 验证 Phase 3 八张表的 MyBatis-Plus 映射、主键回填和关键默认值。 */
@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_TEST_DB_URL",
        matches = "jdbc:mysql://.+")
class Phase3MapperIntegrationTest {

    private static final String CONTENT_HASH = "a".repeat(64);

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
    private UserAccountMapper userAccountMapper;

    @Autowired
    private AiPromptProfileMapper promptProfileMapper;

    @Autowired
    private AiPromptVersionMapper promptVersionMapper;

    @Autowired
    private InformationItemMapper informationItemMapper;

    @Autowired
    private InformationSnapshotMapper informationSnapshotMapper;

    @Autowired
    private InformationAnalysisMapper informationAnalysisMapper;

    @Autowired
    private AiAnalysisScheduleMapper analysisScheduleMapper;

    @Autowired
    private AiAnalysisBatchMapper analysisBatchMapper;

    @Autowired
    private AiAnalysisBatchItemMapper analysisBatchItemMapper;

    @Autowired
    private AiModelInvocationMapper modelInvocationMapper;

    @Autowired
    private AnalysisUsageMapper analysisUsageMapper;

    @Test
    @Transactional
    void mappersPersistAndReadTheAcceptedPhase3Graph() {
        UserAccountPo user = insertUser();
        AiPromptProfilePo profile = insertPromptProfile(user.getId());
        AiPromptVersionPo promptVersion = insertPromptVersion(profile.getId());

        // 激活版本必须引用同一配置档案下已经持久化的不可变版本。
        profile.setActiveVersionId(promptVersion.getId());
        assertEquals(1, promptProfileMapper.updateById(profile));

        InformationItemPo information = insertInformation();
        InformationSnapshotPo snapshot = insertSnapshot(information.getId());
        InformationAnalysisPo analysis = insertAnalysis(
                user.getId(), information.getId(), snapshot.getId(), profile.getId(), promptVersion.getId());
        AiAnalysisSchedulePo schedule = insertSchedule(user.getId(), profile.getId());
        AiAnalysisBatchPo batch =
                insertManualBatch(user.getId(), profile.getId(), promptVersion.getId());
        AiAnalysisBatchItemPo batchItem =
                insertBatchItem(batch.getId(), information.getId(), snapshot.getId(), analysis.getId());
        AiModelInvocationPo invocation =
                insertInvocation(analysis.getId(), user.getId(), batchItem.getId());

        assertEquals(promptVersion.getId(), promptProfileMapper.selectById(profile.getId()).getActiveVersionId());
        assertEquals(snapshot.getId(), informationAnalysisMapper.selectById(analysis.getId()).getSnapshotId());
        assertEquals(Boolean.FALSE, analysisScheduleMapper.selectById(schedule.getId()).getEnabled());
        assertEquals("02:00", analysisScheduleMapper
                .selectById(schedule.getId())
                .getLocalTime()
                .toString());
        assertEquals("PENDING", analysisBatchMapper.selectById(batch.getId()).getStatus());
        assertEquals("SELECTED", analysisBatchItemMapper.selectById(batchItem.getId()).getStatus());

        AiModelInvocationPo persistedInvocation = modelInvocationMapper.selectById(invocation.getId());
        assertEquals("UNAVAILABLE", persistedInvocation.getUsageStatus());
        assertNull(persistedInvocation.getInputTokens());
        assertNull(persistedInvocation.getOutputTokens());
        assertNull(persistedInvocation.getTotalTokens());
        assertNull(persistedInvocation.getCachedInputTokens());
        assertNull(persistedInvocation.getReasoningTokens());

        // User Usage 只聚合当前 Owner 的 Provider 实际报告值，不使用 Estimate 补算。
        persistedInvocation.setUsageStatus("REPORTED");
        persistedInvocation.setInputTokens(100L);
        persistedInvocation.setOutputTokens(50L);
        persistedInvocation.setTotalTokens(150L);
        assertEquals(1, modelInvocationMapper.updateById(persistedInvocation));
        UserUsageAggregateRow usage = analysisUsageMapper.aggregateUserUsage(user.getId());
        assertEquals(1L, usage.getInvocationCount());
        assertEquals(1L, usage.getReportedInvocationCount());
        assertEquals(0L, usage.getUnavailableInvocationCount());
        assertEquals(100L, usage.getInputTokens());
        assertEquals(50L, usage.getOutputTokens());
        assertEquals(150L, usage.getTotalTokens());    }

    private UserAccountPo insertUser() {
        UserAccountPo po = new UserAccountPo();
        po.setUsername("task024-" + UUID.randomUUID());
        po.setPasswordHash("$2a$12$task024-not-a-real-bootstrap-secret");
        po.setDisplayName("TASK-024 Test");
        po.setTimezone("Asia/Shanghai");
        assertEquals(1, userAccountMapper.insert(po));
        assertNotNull(po.getId());
        return po;
    }

    private AiPromptProfilePo insertPromptProfile(long userId) {
        AiPromptProfilePo po = new AiPromptProfilePo();
        po.setUserId(userId);
        po.setName("默认求职分析");
        po.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        assertEquals(1, promptProfileMapper.insert(po));
        assertNotNull(po.getId());
        return po;
    }

    private AiPromptVersionPo insertPromptVersion(long profileId) {
        AiPromptVersionPo po = new AiPromptVersionPo();
        po.setPromptProfileId(profileId);
        po.setVersionNo(1);
        po.setContent("优先匹配 Java 后端职位");
        po.setContentHash(CONTENT_HASH);
        assertEquals(1, promptVersionMapper.insert(po));
        assertNotNull(po.getId());
        return po;
    }

    private InformationItemPo insertInformation() {
        LocalDateTime now = LocalDateTime.now();
        InformationItemPo po = new InformationItemPo();
        po.setInformationType("JOB");
        po.setSource("TASK024_TEST");
        po.setSourceItemId(UUID.randomUUID().toString());
        po.setSourceUrl("https://example.test/jobs/task024");
        po.setTitle("Java 后端工程师");
        po.setContent("Spring Boot 与 MySQL");
        po.setCollectedAt(now);
        po.setFirstSeenTime(now);
        po.setLastSeenTime(now);
        po.setContentHash(CONTENT_HASH);
        po.setCurrentVersionNo(1);
        po.setRawPayload("{\"source\":\"task024-test\"}");
        po.setSchemaVersion(1);
        po.setCollectorId("task024-test");
        po.setCollectorVersion("1.0");
        po.setCollectionContext("{\"test\":true}");
        assertEquals(1, informationItemMapper.insert(po));
        assertNotNull(po.getId());
        return po;
    }

    private InformationSnapshotPo insertSnapshot(long informationId) {
        InformationSnapshotPo po = new InformationSnapshotPo();
        po.setInformationId(informationId);
        po.setVersionNo(1);
        po.setContentHash(CONTENT_HASH);
        po.setTitle("Java 后端工程师");
        po.setContent("Spring Boot 与 MySQL");
        po.setStandardizedPayload("{\"informationType\":\"JOB\"}");
        po.setRawPayload("{\"source\":\"task024-test\"}");
        po.setCollectedAt(LocalDateTime.now());
        po.setCollectorId("task024-test");
        po.setCollectorVersion("1.0");
        assertEquals(1, informationSnapshotMapper.insert(po));
        assertNotNull(po.getId());
        return po;
    }

    private InformationAnalysisPo insertAnalysis(
            long userId,
            long informationId,
            long snapshotId,
            long profileId,
            long promptVersionId) {
        InformationAnalysisPo po = new InformationAnalysisPo();
        po.setUserId(userId);
        po.setInformationId(informationId);
        po.setSnapshotId(snapshotId);
        po.setInformationType("JOB");
        po.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        po.setAnalysisDefinitionVersion(1);
        po.setAnalysisPurpose("USER_RELEVANCE");
        po.setPromptProfileId(profileId);
        po.setPromptVersionId(promptVersionId);
        po.setStatus("PENDING");
        po.setEstimatedInputTokens(120L);
        po.setEstimatedOutputTokens(80L);
        po.setEstimatedTotalTokens(200L);
        po.setEstimateMethod("UTF8_BYTES_DIV_4_V1");
        assertEquals(1, informationAnalysisMapper.insert(po));
        assertNotNull(po.getId());
        return po;
    }

    private AiAnalysisSchedulePo insertSchedule(long userId, long profileId) {
        AiAnalysisSchedulePo po = new AiAnalysisSchedulePo();
        po.setUserId(userId);
        po.setName("每日职位分析");
        po.setPromptProfileId(profileId);
        assertEquals(1, analysisScheduleMapper.insert(po));
        assertNotNull(po.getId());
        return po;
    }

    private AiAnalysisBatchPo insertManualBatch(
            long userId, long profileId, long promptVersionId) {
        LocalDateTime windowEnd = LocalDateTime.now();
        AiAnalysisBatchPo po = new AiAnalysisBatchPo();
        po.setUserId(userId);
        po.setTriggerType("MANUAL");
        po.setManualRequestId(UUID.randomUUID().toString());
        po.setInformationType("JOB");
        po.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        po.setAnalysisDefinitionVersion(1);
        po.setPromptProfileId(profileId);
        po.setPromptVersionId(promptVersionId);
        po.setRequestedWindowDays(3);
        po.setWindowStart(windowEnd.minusDays(3));
        po.setWindowEnd(windowEnd);
        po.setRequestedMaxCandidates(20);
        po.setRequestedTokenBudget(75000L);
        assertEquals(1, analysisBatchMapper.insert(po));
        assertNotNull(po.getId());
        return po;
    }

    private AiAnalysisBatchItemPo insertBatchItem(
            long batchId, long informationId, long snapshotId, long analysisId) {
        AiAnalysisBatchItemPo po = new AiAnalysisBatchItemPo();
        po.setBatchId(batchId);
        po.setInformationId(informationId);
        po.setSnapshotId(snapshotId);
        po.setAnalysisId(analysisId);
        po.setSelectionOrder(1);
        po.setEstimatedInputTokens(120L);
        po.setEstimatedOutputTokens(80L);
        po.setEstimatedTotalTokens(200L);
        assertEquals(1, analysisBatchItemMapper.insert(po));
        assertNotNull(po.getId());
        return po;
    }

    private AiModelInvocationPo insertInvocation(long analysisId, long userId, long batchItemId) {
        AiModelInvocationPo po = new AiModelInvocationPo();
        po.setAnalysisId(analysisId);
        po.setUserId(userId);
        po.setBatchItemId(batchItemId);
        po.setProvider("OPENAI_COMPATIBLE");
        po.setModelName("test-model");
        po.setAttemptNo(1);
        po.setStartedAt(LocalDateTime.now());
        assertEquals(1, modelInvocationMapper.insert(po));
        assertNotNull(po.getId());
        return po;
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
