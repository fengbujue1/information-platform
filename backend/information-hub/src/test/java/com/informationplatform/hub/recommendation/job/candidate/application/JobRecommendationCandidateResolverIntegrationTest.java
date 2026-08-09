package com.informationplatform.hub.recommendation.job.candidate.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptVersionMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.InformationAnalysisMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptVersionPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.InformationAnalysisPo;
import com.informationplatform.hub.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.informationplatform.hub.identity.infrastructure.persistence.po.UserAccountPo;
import com.informationplatform.hub.information.infrastructure.persistence.mapper.InformationItemMapper;
import com.informationplatform.hub.information.infrastructure.persistence.mapper.InformationSnapshotMapper;
import com.informationplatform.hub.information.infrastructure.persistence.po.InformationItemPo;
import com.informationplatform.hub.information.infrastructure.persistence.po.InformationSnapshotPo;
import com.informationplatform.hub.job.infrastructure.persistence.mapper.JobInformationMapper;
import com.informationplatform.hub.job.infrastructure.persistence.po.JobInformationPo;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.UserInformationInteractionMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.UserInformationInteractionPo;
import com.informationplatform.hub.recommendation.job.candidate.domain.JobRecommendationCandidateRequest;
import com.informationplatform.hub.recommendation.job.candidate.domain.JobRecommendationCandidateResolution;
import com.informationplatform.hub.recommendation.job.domain.JobRecommendationProfilePreferences;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.mapper.UserJobDispositionMapper;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.po.UserJobDispositionPo;
import com.informationplatform.hub.testing.DatabaseIntegrationTestSafety;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

/** 使用真实 MySQL 验证 TASK-037 候选语义、稳定顺序与查询计划。 */
@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_TEST_DB_URL",
        matches = "jdbc:mysql://.+")
class JobRecommendationCandidateResolverIntegrationTest {

    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);
    private static final String MAPPED_STATEMENT =
            "com.informationplatform.hub.recommendation.job.candidate.infrastructure.persistence"
                    + ".JobRecommendationCandidateQueryMapper.selectEligibleCandidates";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        String databaseUrl = DatabaseIntegrationTestSafety.requireTestDatabase(
                required("INFORMATION_HUB_TEST_DB_URL"));
        registry.add("spring.datasource.url", () -> databaseUrl);
        registry.add("spring.datasource.username", () -> required("INFORMATION_HUB_TEST_DB_USERNAME"));
        registry.add("spring.datasource.password", () -> required("INFORMATION_HUB_TEST_DB_PASSWORD"));
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired private JobRecommendationCandidateResolver resolver;
    @Autowired private UserAccountMapper userMapper;
    @Autowired private AiPromptProfileMapper promptProfileMapper;
    @Autowired private AiPromptVersionMapper promptVersionMapper;
    @Autowired private InformationItemMapper informationMapper;
    @Autowired private InformationSnapshotMapper snapshotMapper;
    @Autowired private JobInformationMapper jobMapper;
    @Autowired private InformationAnalysisMapper analysisMapper;
    @Autowired private UserInformationInteractionMapper interactionMapper;
    @Autowired private UserJobDispositionMapper dispositionMapper;
    @Autowired private SqlSessionFactory sqlSessionFactory;
    @Autowired private DataSource dataSource;

    @Test
    @Transactional
    void resolvesAcceptedCandidateMatrixAndProducesExplainPlan() throws Exception {
        UserAccountPo user = insertUser();
        AiPromptProfilePo profile = insertPromptProfile(user.getId());
        AiPromptVersionPo prompt = insertPromptVersion(profile.getId(), 1, HASH_A);
        AiPromptVersionPo wrongPrompt = insertPromptVersion(profile.getId(), 2, HASH_B);
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime end = start.plusDays(7);

        InformationItemPo atStart = insertInformation(start, 1, "边界岗位", "ACTIVE");
        InformationSnapshotPo atStartSnapshot = insertSnapshot(atStart, 1, "边界岗位", "自研公司");
        insertAnalysis(user, atStart, atStartSnapshot, profile, prompt, 1, "SUCCEEDED", 70);

        InformationItemPo latestDefinition =
                insertInformation(start.plusDays(2), 1, "Java 后端", "ACTIVE");
        InformationSnapshotPo latestSnapshot =
                insertSnapshot(latestDefinition, 1, "Java 后端", "产品公司");
        insertAnalysis(user, latestDefinition, latestSnapshot, profile, prompt, 1, "SUCCEEDED", 80);
        InformationAnalysisPo latestAnalysis = insertAnalysis(
                user, latestDefinition, latestSnapshot, profile, prompt, 2, "SUCCEEDED", 95);

        InformationItemPo contacted =
                insertInformation(start.plusDays(1), 1, "已联系岗位", "UNKNOWN");
        InformationSnapshotPo contactedSnapshot =
                insertSnapshot(contacted, 1, "已联系岗位", "保留公司");
        insertAnalysis(user, contacted, contactedSnapshot, profile, prompt, 2, "SUCCEEDED", 85);
        insertInteraction(user, contacted, "NONE", "CONTACTED");

        InformationItemPo notInterested =
                insertInformation(start.plusHours(12), 1, "不感兴趣岗位", "ACTIVE");
        InformationSnapshotPo notInterestedSnapshot =
                insertSnapshot(notInterested, 1, "不感兴趣岗位", "普通公司");
        insertAnalysis(user, notInterested, notInterestedSnapshot, profile, prompt, 2, "SUCCEEDED", 60);
        insertInteraction(user, notInterested, "NOT_INTERESTED", "NONE");

        InformationItemPo unsuitable =
                insertInformation(start.plusHours(10), 1, "不合适岗位", "ACTIVE");
        InformationSnapshotPo unsuitableSnapshot =
                insertSnapshot(unsuitable, 1, "不合适岗位", "普通公司");
        insertAnalysis(user, unsuitable, unsuitableSnapshot, profile, prompt, 2, "SUCCEEDED", 60);
        insertInteraction(user, unsuitable, "NONE", "CONTACTED_NOT_SUITABLE");

        InformationItemPo changedAfterExclusion =
                insertInformation(start.plusHours(8), 2, "快照变化岗位", "ACTIVE");
        insertSnapshot(changedAfterExclusion, 1, "旧快照", "旧公司");
        InformationSnapshotPo changedCurrent =
                insertSnapshot(changedAfterExclusion, 2, "新快照", "新公司");
        insertAnalysis(user, changedAfterExclusion, changedCurrent, profile, prompt, 2, "SUCCEEDED", 90);
        insertInteraction(user, changedAfterExclusion, "NOT_INTERESTED", "NONE");

        InformationItemPo keywordExcluded =
                insertInformation(start.plusHours(6), 1, "平台岗位", "ACTIVE");
        InformationSnapshotPo keywordSnapshot =
                insertSnapshot(keywordExcluded, 1, "平台岗位", "外包服务公司");
        insertAnalysis(user, keywordExcluded, keywordSnapshot, profile, prompt, 2, "SUCCEEDED", 90);

        InformationItemPo oldSnapshotOnly =
                insertInformation(start.plusHours(5), 2, "旧分析岗位", "ACTIVE");
        InformationSnapshotPo oldSnapshot =
                insertSnapshot(oldSnapshotOnly, 1, "旧分析岗位", "普通公司");
        insertSnapshot(oldSnapshotOnly, 2, "当前岗位", "普通公司");
        insertAnalysis(user, oldSnapshotOnly, oldSnapshot, profile, prompt, 2, "SUCCEEDED", 90);

        InformationItemPo wrongPromptOnly =
                insertInformation(start.plusHours(4), 1, "错误 Prompt", "ACTIVE");
        InformationSnapshotPo wrongPromptSnapshot =
                insertSnapshot(wrongPromptOnly, 1, "错误 Prompt", "普通公司");
        insertAnalysis(user, wrongPromptOnly, wrongPromptSnapshot, profile, wrongPrompt, 2, "SUCCEEDED", 90);

        InformationItemPo failedOnly =
                insertInformation(start.plusHours(3), 1, "失败分析", "ACTIVE");
        InformationSnapshotPo failedSnapshot =
                insertSnapshot(failedOnly, 1, "失败分析", "普通公司");
        insertAnalysis(user, failedOnly, failedSnapshot, profile, prompt, 2, "FAILED", null);

        InformationItemPo atEnd = insertInformation(end, 1, "结束边界", "ACTIVE");
        InformationSnapshotPo atEndSnapshot = insertSnapshot(atEnd, 1, "结束边界", "普通公司");
        insertAnalysis(user, atEnd, atEndSnapshot, profile, prompt, 2, "SUCCEEDED", 90);

        InformationItemPo offline =
                insertInformation(start.plusHours(2), 1, "已下线", "OFFLINE");
        InformationSnapshotPo offlineSnapshot = insertSnapshot(offline, 1, "已下线", "普通公司");
        insertAnalysis(user, offline, offlineSnapshot, profile, prompt, 2, "SUCCEEDED", 90);

        JobRecommendationCandidateRequest request = request(
                user.getId(), prompt.getId(), start, end, List.of("外包"));
        JobRecommendationCandidateResolution resolution = resolver.resolve(request);

        assertThat(resolution.candidateCount()).isEqualTo(7);
        assertThat(resolution.eligibleCount()).isEqualTo(3);
        assertThat(resolution.candidates())
                .extracting(candidate -> candidate.informationId())
                .containsExactly(latestDefinition.getId(), contacted.getId(), atStart.getId());
        assertThat(resolution.candidates().getFirst().analysisId()).isEqualTo(latestAnalysis.getId());
        assertThat(resolution.candidates().getFirst().analysisDefinitionVersion()).isEqualTo(2);

        String explain = explain(request);
        assertThat(explain)
                .contains("information_item", "information_snapshot", "information_analysis")
                .contains(
                        "user_information_interaction",
                        "idx_information_item_type_first_seen_id",
                        "uk_information_analysis_identity",
                        "uk_user_information_interaction_identity");
    }

    private String explain(JobRecommendationCandidateRequest request) throws Exception {
        MappedStatement statement = sqlSessionFactory
                .getConfiguration()
                .getMappedStatement(MAPPED_STATEMENT);
        Map<String, Object> parameters = Map.of(
                "request", request,
                "compatibleVersions", List.of(1, 2));
        BoundSql boundSql = statement.getBoundSql(parameters);
        try (Connection connection = dataSource.getConnection();
                PreparedStatement prepared = connection.prepareStatement(
                        "EXPLAIN FORMAT=JSON " + boundSql.getSql())) {
            new DefaultParameterHandler(statement, parameters, boundSql).setParameters(prepared);
            try (ResultSet resultSet = prepared.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getString(1);
            }
        }
    }

    private JobRecommendationCandidateRequest request(
            long userId,
            long promptVersionId,
            LocalDateTime start,
            LocalDateTime end,
            List<String> excludedKeywords) {
        return new JobRecommendationCandidateRequest(
                userId,
                promptVersionId,
                start,
                end,
                new JobRecommendationProfilePreferences(
                        List.of("Java 后端"),
                        List.of("Java"),
                        List.of("上海"),
                        List.of("REMOTE"),
                        20_000,
                        excludedKeywords));
    }

    private UserAccountPo insertUser() {
        UserAccountPo po = new UserAccountPo();
        po.setUsername("task037-" + UUID.randomUUID());
        po.setPasswordHash("$2a$12$task037-not-a-real-secret");
        po.setDisplayName("TASK-037");
        po.setTimezone("Asia/Shanghai");
        po.setStatus("ACTIVE");
        assertEquals(1, userMapper.insert(po));
        return po;
    }

    private AiPromptProfilePo insertPromptProfile(long userId) {
        AiPromptProfilePo po = new AiPromptProfilePo();
        po.setUserId(userId);
        po.setName("TASK-037 " + UUID.randomUUID());
        po.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        po.setStatus("ACTIVE");
        assertEquals(1, promptProfileMapper.insert(po));
        return po;
    }

    private AiPromptVersionPo insertPromptVersion(long profileId, int versionNo, String hash) {
        AiPromptVersionPo po = new AiPromptVersionPo();
        po.setPromptProfileId(profileId);
        po.setVersionNo(versionNo);
        po.setContent("TASK-037 prompt " + versionNo);
        po.setContentHash(hash);
        assertEquals(1, promptVersionMapper.insert(po));
        return po;
    }

    private InformationItemPo insertInformation(
            LocalDateTime firstSeen, int currentVersion, String title, String jobStatus) {
        InformationItemPo po = new InformationItemPo();
        po.setInformationType("JOB");
        po.setSource("TASK037_TEST");
        po.setSourceItemId(UUID.randomUUID().toString());
        po.setTitle(title);
        po.setContent("Spring Boot");
        po.setCollectedAt(firstSeen);
        po.setFirstSeenTime(firstSeen);
        po.setLastSeenTime(firstSeen);
        po.setContentHash(HASH_A);
        po.setCurrentVersionNo(currentVersion);
        po.setRawPayload("{\"test\":true}");
        po.setSchemaVersion(1);
        po.setCollectorId("task037-test");
        po.setCollectorVersion("1.0");
        assertEquals(1, informationMapper.insert(po));

        JobInformationPo job = new JobInformationPo();
        job.setInformationId(po.getId());
        job.setCompanyName("测试公司");
        job.setJobStatus(jobStatus);
        job.setDetailStatus("SUCCESS");
        assertEquals(1, jobMapper.insert(job));
        return po;
    }

    private InformationSnapshotPo insertSnapshot(
            InformationItemPo information, int versionNo, String title, String companyName) {
        InformationSnapshotPo po = new InformationSnapshotPo();
        po.setInformationId(information.getId());
        po.setVersionNo(versionNo);
        po.setContentHash(versionNo == 1 ? HASH_A : HASH_B);
        po.setTitle(title);
        po.setContent("Spring Boot 服务");
        po.setStandardizedPayload(
                "{\"informationType\":\"JOB\",\"job\":{\"companyName\":\""
                        + companyName
                        + "\"}}");
        po.setRawPayload("{\"test\":true}");
        po.setCollectedAt(information.getFirstSeenTime());
        po.setCollectorId("task037-test");
        po.setCollectorVersion("1.0");
        assertEquals(1, snapshotMapper.insert(po));
        return po;
    }

    private InformationAnalysisPo insertAnalysis(
            UserAccountPo user,
            InformationItemPo information,
            InformationSnapshotPo snapshot,
            AiPromptProfilePo profile,
            AiPromptVersionPo prompt,
            int definitionVersion,
            String status,
            Integer score) {
        InformationAnalysisPo po = new InformationAnalysisPo();
        po.setUserId(user.getId());
        po.setInformationId(information.getId());
        po.setSnapshotId(snapshot.getId());
        po.setInformationType("JOB");
        po.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        po.setAnalysisDefinitionVersion(definitionVersion);
        po.setAnalysisPurpose("USER_RELEVANCE");
        po.setPromptProfileId(profile.getId());
        po.setPromptVersionId(prompt.getId());
        po.setStatus(status);
        if (score != null) {
            po.setResultJson("{\"relevanceScore\":" + score + ",\"summary\":\"匹配\"}");
            po.setRelevanceScore(score);
            po.setSummary("匹配");
        }
        assertEquals(1, analysisMapper.insert(po));
        return po;
    }

    private void insertInteraction(
            UserAccountPo user,
            InformationItemPo information,
            String feedbackState,
            String jobDisposition) {
        UserInformationInteractionPo interaction = new UserInformationInteractionPo();
        interaction.setUserId(user.getId());
        interaction.setInformationId(information.getId());
        interaction.setViewCount(0);
        interaction.setFeedbackState(feedbackState);
        assertEquals(1, interactionMapper.insert(interaction));

        UserJobDispositionPo disposition = new UserJobDispositionPo();
        disposition.setInteractionId(interaction.getId());
        disposition.setJobDisposition(jobDisposition);
        assertEquals(1, dispositionMapper.insert(disposition));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
