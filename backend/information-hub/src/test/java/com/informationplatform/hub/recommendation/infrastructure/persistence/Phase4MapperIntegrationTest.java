package com.informationplatform.hub.recommendation.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptVersionMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.InformationAnalysisMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptVersionPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.InformationAnalysisPo;
import com.informationplatform.hub.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.informationplatform.hub.identity.infrastructure.persistence.po.UserAccountPo;
import com.informationplatform.hub.information.infrastructure.persistence.mapper.InformationItemMapper;
import com.informationplatform.hub.information.infrastructure.persistence.mapper.InformationSnapshotMapper;
import com.informationplatform.hub.information.infrastructure.persistence.po.InformationItemPo;
import com.informationplatform.hub.information.infrastructure.persistence.po.InformationSnapshotPo;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.RecommendationItemMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.RecommendationRunMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.UserInformationInteractionMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.UserRecommendationProfileMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.RecommendationItemPo;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.RecommendationRunPo;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.UserInformationInteractionPo;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.UserRecommendationProfilePo;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.mapper.JobRecommendationProfileMapper;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.mapper.UserJobDispositionMapper;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.po.JobRecommendationProfilePo;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.po.UserJobDispositionPo;
import com.informationplatform.hub.testing.DatabaseIntegrationTestSafety;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

/** 验证 Phase 4 通用 Core、JOB 扩展、Run/Item 的 MyBatis-Plus 持久化。 */
@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_TEST_DB_URL",
        matches = "jdbc:mysql://.+")
class Phase4MapperIntegrationTest {

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

    @Autowired private UserAccountMapper userAccountMapper;
    @Autowired private AiPromptProfileMapper promptProfileMapper;
    @Autowired private AiPromptVersionMapper promptVersionMapper;
    @Autowired private InformationItemMapper informationItemMapper;
    @Autowired private InformationSnapshotMapper informationSnapshotMapper;
    @Autowired private InformationAnalysisMapper informationAnalysisMapper;
    @Autowired private AiAnalysisBatchMapper analysisBatchMapper;
    @Autowired private UserRecommendationProfileMapper recommendationProfileMapper;
    @Autowired private JobRecommendationProfileMapper jobRecommendationProfileMapper;
    @Autowired private UserInformationInteractionMapper interactionMapper;
    @Autowired private UserJobDispositionMapper userJobDispositionMapper;
    @Autowired private RecommendationRunMapper recommendationRunMapper;
    @Autowired private RecommendationItemMapper recommendationItemMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void mappersPersistUpdateReadAndDeleteAcceptedRecommendationGraph() {
        UserAccountPo user = insertUser();
        AiPromptProfilePo promptProfile = insertPromptProfile(user.getId());
        AiPromptVersionPo promptVersion = insertPromptVersion(promptProfile.getId());
        InformationItemPo information = insertInformation();
        InformationSnapshotPo snapshot = insertSnapshot(information.getId());
        InformationAnalysisPo analysis = insertAnalysis(
                user.getId(), information.getId(), snapshot.getId(),
                promptProfile.getId(), promptVersion.getId());
        AiAnalysisBatchPo batch = insertBatch(
                user.getId(), promptProfile.getId(), promptVersion.getId());

        UserRecommendationProfilePo recommendationProfile = new UserRecommendationProfilePo();
        recommendationProfile.setUserId(user.getId());
        recommendationProfile.setInformationType("JOB");
        recommendationProfile.setAnalysisPromptProfileId(promptProfile.getId());
        recommendationProfile.setContentHash(HASH_B);
        assertEquals(1, recommendationProfileMapper.insert(recommendationProfile));
        assertNotNull(recommendationProfile.getId());

        JobRecommendationProfilePo jobProfile = new JobRecommendationProfilePo();
        jobProfile.setProfileId(recommendationProfile.getId());
        jobProfile.setTargetRoles("[\"Java 后端\"]");
        jobProfile.setPreferredSkills("[\"Java\",\"Spring Boot\"]");
        assertEquals(1, jobRecommendationProfileMapper.insert(jobProfile));
        assertEquals(
                "[\"Java 后端\"]",
                jobRecommendationProfileMapper
                        .selectById(recommendationProfile.getId())
                        .getTargetRoles());

        UserRecommendationProfilePo educationProfile = new UserRecommendationProfilePo();
        educationProfile.setUserId(user.getId());
        educationProfile.setInformationType("EDUCATION");
        educationProfile.setAnalysisPromptProfileId(promptProfile.getId());
        educationProfile.setContentHash(HASH_A);
        assertEquals(1, recommendationProfileMapper.insert(educationProfile));
        assertNotNull(educationProfile.getId());

        UserRecommendationProfilePo duplicateJobProfile = new UserRecommendationProfilePo();
        duplicateJobProfile.setUserId(user.getId());
        duplicateJobProfile.setInformationType("JOB");
        duplicateJobProfile.setAnalysisPromptProfileId(promptProfile.getId());
        duplicateJobProfile.setContentHash(HASH_A);
        assertThrows(
                DuplicateKeyException.class,
                () -> recommendationProfileMapper.insert(duplicateJobProfile));

        UserRecommendationProfilePo persistedProfile =
                recommendationProfileMapper.selectById(recommendationProfile.getId());
        assertEquals(7, persistedProfile.getWindowDays());
        assertEquals(50, persistedProfile.getTopN());
        assertEquals("JOB", persistedProfile.getInformationType());
        persistedProfile.setTopN(25);
        assertEquals(1, recommendationProfileMapper.updateById(persistedProfile));
        assertEquals(25, recommendationProfileMapper
                .selectById(persistedProfile.getId())
                .getTopN());

        UserInformationInteractionPo interaction = new UserInformationInteractionPo();
        interaction.setUserId(user.getId());
        interaction.setInformationId(information.getId());
        assertEquals(1, interactionMapper.insert(interaction));
        assertNotNull(interaction.getId());
        UserInformationInteractionPo persistedInteraction =
                interactionMapper.selectById(interaction.getId());
        assertEquals(0, persistedInteraction.getViewCount());
        assertEquals("NONE", persistedInteraction.getFeedbackState());

        UserJobDispositionPo disposition = new UserJobDispositionPo();
        disposition.setInteractionId(interaction.getId());
        assertEquals(1, userJobDispositionMapper.insert(disposition));
        assertEquals(
                "NONE",
                userJobDispositionMapper.selectById(interaction.getId()).getJobDisposition());
        disposition.setJobDisposition("CONTACTED");
        assertEquals(1, userJobDispositionMapper.updateById(disposition));
        assertEquals(
                "CONTACTED",
                userJobDispositionMapper.selectById(interaction.getId()).getJobDisposition());

        LocalDateTime windowEnd = LocalDateTime.now();
        RecommendationRunPo run = new RecommendationRunPo();
        run.setUserId(user.getId());
        run.setInformationType("JOB");
        run.setTriggerType("ANALYSIS_BATCH_COMPLETED");
        run.setSourceAnalysisBatchId(batch.getId());
        run.setProfileId(recommendationProfile.getId());
        run.setProfileContentHash(HASH_B);
        run.setProfileSnapshotJson(
                "{\"informationType\":\"JOB\",\"windowDays\":7,\"topN\":25}");
        run.setPromptProfileId(promptProfile.getId());
        run.setPromptVersionId(promptVersion.getId());
        run.setAlgorithmKey("JOB_RECOMMENDATION");
        run.setAlgorithmVersion(1);
        run.setWindowStart(windowEnd.minusDays(7));
        run.setWindowEnd(windowEnd);
        assertEquals(1, recommendationRunMapper.insert(run));
        assertNotNull(run.getId());
        assertEquals("PENDING", recommendationRunMapper.selectById(run.getId()).getStatus());
        assertEquals("JOB", recommendationRunMapper.selectById(run.getId()).getInformationType());

        RecommendationItemPo item = new RecommendationItemPo();
        item.setRunId(run.getId());
        item.setInformationId(information.getId());
        item.setSnapshotId(snapshot.getId());
        item.setAnalysisId(analysis.getId());
        item.setRankNo(1);
        item.setFinalScore(new BigDecimal("92.400"));
        item.setAiRelevanceScore(new BigDecimal("95.000"));
        item.setProfileMatchScore(new BigDecimal("86.000"));
        item.setFreshnessScore(new BigDecimal("90.000"));
        item.setScoreBreakdownJson("{\"aiRelevanceScore\":95}");
        item.setReasonsJson("[\"AI 相关度高\"]");
        item.setDuplicateGroupKey(HASH_A);
        assertEquals(1, recommendationItemMapper.insert(item));
        assertNotNull(item.getId());
        assertEquals(
                new BigDecimal("92.400"),
                recommendationItemMapper.selectById(item.getId()).getFinalScore());

        persistedInteraction.setLastRecommendationItemId(item.getId());
        assertEquals(1, interactionMapper.updateById(persistedInteraction));
        assertEquals(item.getId(), interactionMapper
                .selectById(interaction.getId())
                .getLastRecommendationItemId());

        // 基于真实合法数据图执行 EXPLAIN，确认 V1 Profile、Interaction、Feed 与 Item 查询可命中组合索引。
        assertPossibleKey(
                "EXPLAIN SELECT * FROM user_recommendation_profile "
                        + "WHERE user_id = ? AND information_type = 'JOB'",
                "uk_user_recommendation_profile_identity",
                user.getId());
        assertPossibleKey(
                "EXPLAIN SELECT * FROM user_information_interaction WHERE user_id = ? AND information_id = ?",
                "uk_user_information_interaction_identity",
                user.getId(),
                information.getId());
        assertPossibleKey(
                "EXPLAIN SELECT * FROM recommendation_run WHERE user_id = ? "
                        + "AND information_type = 'JOB' AND status = 'PENDING' "
                        + "ORDER BY completed_at DESC, id DESC LIMIT 1",
                "idx_recommendation_run_user_type_status_created",
                user.getId());
        assertPossibleKey(
                "EXPLAIN SELECT * FROM recommendation_item WHERE run_id = ? ORDER BY rank_no LIMIT 20",
                "uk_recommendation_item_rank",
                run.getId());

        // 先解除可空归因 FK，再按依赖反序验证四个 Mapper 的删除能力。
        assertEquals(1, interactionMapper.update(
                null,
                Wrappers.<UserInformationInteractionPo>lambdaUpdate()
                        .set(UserInformationInteractionPo::getLastRecommendationItemId, null)
                        .eq(UserInformationInteractionPo::getId, interaction.getId())));
        assertEquals(1, userJobDispositionMapper.deleteById(interaction.getId()));
        assertEquals(1, interactionMapper.deleteById(interaction.getId()));
        assertEquals(1, recommendationItemMapper.deleteById(item.getId()));
        assertEquals(1, recommendationRunMapper.deleteById(run.getId()));
        assertEquals(1, jobRecommendationProfileMapper.deleteById(recommendationProfile.getId()));
        assertEquals(1, recommendationProfileMapper.deleteById(recommendationProfile.getId()));
        assertEquals(1, recommendationProfileMapper.deleteById(educationProfile.getId()));
        assertNull(recommendationProfileMapper.selectById(recommendationProfile.getId()));
    }

    private void assertPossibleKey(String sql, String expectedIndex, Object... arguments) {
        String possibleKeys = (String) jdbcTemplate.queryForMap(sql, arguments).get("possible_keys");
        assertNotNull(possibleKeys);
        assertTrue(
                java.util.Set.of(possibleKeys.split(",")).contains(expectedIndex),
                possibleKeys);
    }

    private UserAccountPo insertUser() {
        UserAccountPo po = new UserAccountPo();
        po.setUsername("task034-" + UUID.randomUUID());
        po.setPasswordHash("$2a$12$task034-not-a-real-secret");
        po.setTimezone("Asia/Shanghai");
        assertEquals(1, userAccountMapper.insert(po));
        return po;
    }

    private AiPromptProfilePo insertPromptProfile(long userId) {
        AiPromptProfilePo po = new AiPromptProfilePo();
        po.setUserId(userId);
        po.setName("TASK-034 Prompt " + UUID.randomUUID());
        po.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        assertEquals(1, promptProfileMapper.insert(po));
        return po;
    }

    private AiPromptVersionPo insertPromptVersion(long profileId) {
        AiPromptVersionPo po = new AiPromptVersionPo();
        po.setPromptProfileId(profileId);
        po.setVersionNo(1);
        po.setContent("TASK-034 mapper test prompt");
        po.setContentHash(HASH_A);
        assertEquals(1, promptVersionMapper.insert(po));
        return po;
    }

    private InformationItemPo insertInformation() {
        LocalDateTime now = LocalDateTime.now();
        InformationItemPo po = new InformationItemPo();
        po.setInformationType("JOB");
        po.setSource("TASK034_TEST");
        po.setSourceItemId(UUID.randomUUID().toString());
        po.setTitle("Java 后端工程师");
        po.setCollectedAt(now);
        po.setFirstSeenTime(now);
        po.setLastSeenTime(now);
        po.setContentHash(HASH_A);
        po.setCurrentVersionNo(1);
        po.setRawPayload("{\"test\":true}");
        po.setSchemaVersion(1);
        po.setCollectorId("task034-test");
        po.setCollectorVersion("1.0");
        assertEquals(1, informationItemMapper.insert(po));
        return po;
    }

    private InformationSnapshotPo insertSnapshot(long informationId) {
        InformationSnapshotPo po = new InformationSnapshotPo();
        po.setInformationId(informationId);
        po.setVersionNo(1);
        po.setContentHash(HASH_A);
        po.setTitle("Java 后端工程师");
        po.setStandardizedPayload("{\"informationType\":\"JOB\"}");
        po.setRawPayload("{\"test\":true}");
        po.setCollectedAt(LocalDateTime.now());
        po.setCollectorId("task034-test");
        po.setCollectorVersion("1.0");
        assertEquals(1, informationSnapshotMapper.insert(po));
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
        po.setAnalysisDefinitionVersion(2);
        po.setAnalysisPurpose("USER_RELEVANCE");
        po.setPromptProfileId(profileId);
        po.setPromptVersionId(promptVersionId);
        po.setStatus("SUCCEEDED");
        po.setResultJson("{\"relevanceScore\":95,\"summary\":\"匹配\"}");
        po.setRelevanceScore(95);
        assertEquals(1, informationAnalysisMapper.insert(po));
        return po;
    }

    private AiAnalysisBatchPo insertBatch(long userId, long profileId, long promptVersionId) {
        LocalDateTime windowEnd = LocalDateTime.now();
        AiAnalysisBatchPo po = new AiAnalysisBatchPo();
        po.setUserId(userId);
        po.setTriggerType("MANUAL");
        po.setManualRequestId(UUID.randomUUID().toString());
        po.setInformationType("JOB");
        po.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        po.setAnalysisDefinitionVersion(2);
        po.setPromptProfileId(profileId);
        po.setPromptVersionId(promptVersionId);
        po.setRequestedWindowDays(7);
        po.setWindowStart(windowEnd.minusDays(7));
        po.setWindowEnd(windowEnd);
        po.setRequestedMaxCandidates(50);
        po.setRequestedTokenBudget(75000L);
        assertEquals(1, analysisBatchMapper.insert(po));
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
