package com.informationplatform.hub.analysis.candidate.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.informationplatform.hub.analysis.candidate.domain.CandidateResolution;
import com.informationplatform.hub.analysis.candidate.domain.CandidateResolutionRequest;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiModelInvocationMapper;
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

/** 使用真实 MySQL 验证 FIRST_INGESTED、当前 Snapshot、稳定排序和成功身份排除。 */
@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_TEST_DB_URL",
        matches = "jdbc:mysql://.+")
class JobCandidateResolverIntegrationTest {

    private static final String HASH = "d".repeat(64);

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
    private JobCandidateResolver resolver;

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
    private InformationAnalysisMapper analysisMapper;

    @Autowired
    private AiModelInvocationMapper invocationMapper;

    @Autowired
    private AiAnalysisBatchMapper batchMapper;

    @Test
    @Transactional
    void resolvesHalfOpenWindowCurrentSnapshotStableOrderAndNoWrites() {
        UserAccountPo owner = insertUser();
        AiPromptProfilePo profile = insertProfile(owner.getId());
        AiPromptVersionPo version = insertVersion(profile.getId());
        profile.setActiveVersionId(version.getId());
        assertEquals(1, profileMapper.updateById(profile));
        LocalDateTime start = LocalDateTime.of(2026, 8, 2, 6, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 5, 6, 0);

        InformationItemPo atStart = insertInformation(start, 1);
        InformationSnapshotPo startSnapshot = insertSnapshot(atStart.getId(), 1, "start");
        InformationItemPo tiedFirst = insertInformation(start.plusDays(1), 2);
        insertSnapshot(tiedFirst.getId(), 1, "old");
        InformationSnapshotPo tiedFirstCurrent =
                insertSnapshot(tiedFirst.getId(), 2, "current");
        InformationItemPo tiedSecond = insertInformation(start.plusDays(1), 1);
        InformationSnapshotPo tiedSecondSnapshot =
                insertSnapshot(tiedSecond.getId(), 1, "second");
        insertSnapshot(insertInformation(start.minusNanos(1_000_000), 1).getId(), 1, "before");
        insertSnapshot(insertInformation(end, 1).getId(), 1, "end");

        long invocationBefore = invocationMapper.selectCount(Wrappers.emptyWrapper());
        long batchBefore = batchMapper.selectCount(Wrappers.emptyWrapper());
        CandidateResolution ordered = resolver.resolve(request(
                owner.getId(), version.getId(), start, end, 2));

        assertThat(ordered.totalInWindow()).isEqualTo(3);
        assertThat(ordered.alreadyAnalyzedCount()).isZero();
        assertThat(ordered.eligibleCount()).isEqualTo(3);
        assertThat(ordered.candidates())
                .extracting(candidate -> candidate.informationId())
                .containsExactly(tiedSecond.getId(), tiedFirst.getId());
        assertThat(ordered.candidates().get(1).snapshotId())
                .isEqualTo(tiedFirstCurrent.getId());

        insertSucceededAnalysis(
                owner.getId(), tiedSecond, tiedSecondSnapshot, profile, version);
        long analysisBefore = analysisMapper.selectCount(Wrappers.emptyWrapper());
        CandidateResolution excluded = resolver.resolve(request(
                owner.getId(), version.getId(), start, end, 10));
        assertThat(excluded.totalInWindow()).isEqualTo(3);
        assertThat(excluded.alreadyAnalyzedCount()).isEqualTo(1);
        assertThat(excluded.eligibleCount()).isEqualTo(2);
        assertThat(excluded.candidates())
                .extracting(candidate -> candidate.snapshotId())
                .containsExactly(tiedFirstCurrent.getId(), startSnapshot.getId());
        assertThat(analysisMapper.selectCount(Wrappers.emptyWrapper()))
                .isEqualTo(analysisBefore);
        assertThat(invocationMapper.selectCount(Wrappers.emptyWrapper()))
                .isEqualTo(invocationBefore);
        assertThat(batchMapper.selectCount(Wrappers.emptyWrapper())).isEqualTo(batchBefore);
    }

    private CandidateResolutionRequest request(
            long userId,
            long promptVersionId,
            LocalDateTime start,
            LocalDateTime end,
            int maxCandidates) {
        return new CandidateResolutionRequest(
                userId,
                promptVersionId,
                "JOB_USER_RELEVANCE",
                1,
                start,
                end,
                maxCandidates);
    }

    private UserAccountPo insertUser() {
        UserAccountPo po = new UserAccountPo();
        po.setUsername("task028-" + UUID.randomUUID());
        po.setPasswordHash("$2a$12$task028-not-a-real-secret");
        po.setDisplayName("TASK-028");
        po.setTimezone("Asia/Shanghai");
        po.setStatus("ACTIVE");
        assertEquals(1, userMapper.insert(po));
        return po;
    }

    private AiPromptProfilePo insertProfile(long userId) {
        AiPromptProfilePo po = new AiPromptProfilePo();
        po.setUserId(userId);
        po.setName("TASK-028 " + UUID.randomUUID());
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

    private InformationItemPo insertInformation(LocalDateTime firstSeen, int currentVersion) {
        InformationItemPo po = new InformationItemPo();
        po.setInformationType("JOB");
        po.setSource("TASK028_TEST");
        po.setSourceItemId(UUID.randomUUID().toString());
        po.setTitle("Java 后端工程师");
        po.setContent("Spring Boot");
        po.setCollectedAt(firstSeen);
        po.setFirstSeenTime(firstSeen);
        po.setLastSeenTime(firstSeen);
        po.setContentHash(HASH);
        po.setCurrentVersionNo(currentVersion);
        po.setRawPayload("{\"source\":\"test\"}");
        po.setSchemaVersion(1);
        po.setCollectorId("task028-test");
        po.setCollectorVersion("1.0");
        assertEquals(1, informationMapper.insert(po));
        return po;
    }

    private InformationSnapshotPo insertSnapshot(
            long informationId, int versionNo, String marker) {
        InformationSnapshotPo po = new InformationSnapshotPo();
        po.setInformationId(informationId);
        po.setVersionNo(versionNo);
        po.setContentHash(HASH);
        po.setTitle("Java " + marker);
        po.setContent("Spring Boot " + marker);
        po.setStandardizedPayload(
                "{\"job\":{\"companyName\":\"" + marker + "\"}}");
        po.setRawPayload("{\"source\":\"test\"}");
        po.setCollectedAt(LocalDateTime.of(2026, 8, 4, 0, 0));
        po.setCollectorId("task028-test");
        po.setCollectorVersion("1.0");
        assertEquals(1, snapshotMapper.insert(po));
        return po;
    }

    private void insertSucceededAnalysis(
            long userId,
            InformationItemPo information,
            InformationSnapshotPo snapshot,
            AiPromptProfilePo profile,
            AiPromptVersionPo version) {
        InformationAnalysisPo po = new InformationAnalysisPo();
        po.setUserId(userId);
        po.setInformationId(information.getId());
        po.setSnapshotId(snapshot.getId());
        po.setInformationType("JOB");
        po.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        po.setAnalysisDefinitionVersion(1);
        po.setAnalysisPurpose("USER_RELEVANCE");
        po.setPromptProfileId(profile.getId());
        po.setPromptVersionId(version.getId());
        po.setStatus("SUCCEEDED");
        assertEquals(1, analysisMapper.insert(po));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
