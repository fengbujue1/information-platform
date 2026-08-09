package com.informationplatform.hub.recommendation.feed.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptVersionMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.InformationAnalysisMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptVersionPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.InformationAnalysisPo;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.identity.domain.AuthenticatedUser;
import com.informationplatform.hub.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.informationplatform.hub.identity.infrastructure.persistence.po.UserAccountPo;
import com.informationplatform.hub.information.infrastructure.persistence.mapper.InformationItemMapper;
import com.informationplatform.hub.information.infrastructure.persistence.mapper.InformationSnapshotMapper;
import com.informationplatform.hub.information.infrastructure.persistence.po.InformationItemPo;
import com.informationplatform.hub.information.infrastructure.persistence.po.InformationSnapshotPo;
import com.informationplatform.hub.job.infrastructure.persistence.mapper.JobInformationMapper;
import com.informationplatform.hub.job.infrastructure.persistence.po.JobInformationPo;
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
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

/** 使用真实 MySQL 验证成功 Run 切换、分页、Owner、stale 与即时可见性。 */
@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_TEST_DB_URL",
        matches = "jdbc:mysql://.+")
class RecommendationFeedServiceIntegrationTest {

    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        String databaseUrl = DatabaseIntegrationTestSafety.requireTestDatabase(
                required("INFORMATION_HUB_TEST_DB_URL"));
        registry.add("spring.datasource.url", () -> databaseUrl);
        registry.add("spring.datasource.username", () -> required("INFORMATION_HUB_TEST_DB_USERNAME"));
        registry.add("spring.datasource.password", () -> required("INFORMATION_HUB_TEST_DB_PASSWORD"));
        // 全量测试会缓存多个 Spring Context；限制本用例连接池，避免挤占共享测试库连接。
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 2);
        registry.add("spring.datasource.hikari.minimum-idle", () -> 0);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired private RecommendationFeedService feedService;
    @Autowired private UserAccountMapper userMapper;
    @Autowired private AiPromptProfileMapper promptProfileMapper;
    @Autowired private AiPromptVersionMapper promptVersionMapper;
    @Autowired private UserRecommendationProfileMapper profileMapper;
    @Autowired private JobRecommendationProfileMapper jobProfileMapper;
    @Autowired private InformationItemMapper informationMapper;
    @Autowired private JobInformationMapper jobMapper;
    @Autowired private InformationSnapshotMapper snapshotMapper;
    @Autowired private InformationAnalysisMapper analysisMapper;
    @Autowired private RecommendationRunMapper runMapper;
    @Autowired private RecommendationItemMapper itemMapper;
    @Autowired private UserInformationInteractionMapper interactionMapper;
    @Autowired private UserJobDispositionMapper dispositionMapper;
    @Autowired private TransactionTemplate transactions;

    @MockitoBean private CurrentUserProvider currentUserProvider;

    @Test
    void preservesOldFeedUntilCompletionAndAppliesCurrentVisibilityPaginationOwnerAndStale() {
        Fixture fixture = transactions.execute(status -> insertFixture());
        assertThat(fixture).isNotNull();
        when(currentUserProvider.requireCurrentUser()).thenReturn(new AuthenticatedUser(
                fixture.owner().userId(), "task042-owner", null, "Asia/Shanghai"));
        try {
            // 较新的 PENDING/FAILED 及另一 Owner 的 COMPLETED 都不能覆盖当前 Owner 的旧成功 Feed。
            var oldFeed = feedService.get("JOB", 1, 20);
            assertThat(oldFeed.run().id()).isEqualTo(fixture.oldCompletedRunId());
            assertThat(oldFeed.total()).isEqualTo(1);
            assertThat(oldFeed.items()).singleElement().satisfies(item -> {
                assertThat(item.informationId()).isEqualTo(fixture.information().getFirst().informationId());
                assertThat(item.jobDisposition()).isEqualTo("CONTACTED");
                assertThat(item.viewed()).isTrue();
            });

            transactions.executeWithoutResult(status -> clearHardExclusions(fixture));
            var firstPage = feedService.get("JOB", 1, 2);
            var secondPage = feedService.get("JOB", 2, 2);
            assertThat(firstPage.total()).isEqualTo(3);
            assertThat(firstPage.items()).hasSize(2);
            assertThat(secondPage.items()).singleElement()
                    .extracting(item -> item.informationId())
                    .isEqualTo(fixture.information().get(2).informationId());

            transactions.executeWithoutResult(status -> {
                UserRecommendationProfilePo profile =
                        profileMapper.selectById(fixture.owner().profileId());
                profile.setContentHash(HASH_B);
                assertEquals(1, profileMapper.updateById(profile));
            });
            assertThat(feedService.get("JOB", 1, 20).run().profileChangedSinceRun()).isTrue();

            transactions.executeWithoutResult(status -> completeNewRun(fixture));
            var switched = feedService.get("JOB", 1, 20);
            assertThat(switched.run().id()).isEqualTo(fixture.newRunId());
            assertThat(switched.items()).singleElement()
                    .extracting(item -> item.informationId())
                    .isEqualTo(fixture.information().get(2).informationId());
        } finally {
            transactions.executeWithoutResult(status -> deleteFixture(fixture));
        }
    }

    private Fixture insertFixture() {
        Owner owner = insertOwner("owner");
        Owner otherOwner = insertOwner("other");
        List<InformationGraph> information = List.of(
                insertInformationGraph(owner, 1, "Java Backend Engineer"),
                insertInformationGraph(owner, 2, "Senior Java Engineer"),
                insertInformationGraph(owner, 3, "Platform Engineer"));
        LocalDateTime base = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(10);

        RecommendationRunPo oldCompleted = insertRun(owner, "COMPLETED", base, 3);
        for (int index = 0; index < information.size(); index++) {
            insertItem(oldCompleted.getId(), information.get(index), index + 1);
        }
        insertInteraction(owner.userId(), information.getFirst().informationId(),
                "INTERESTED", "CONTACTED", 2);
        insertInteraction(owner.userId(), information.get(1).informationId(),
                "NOT_INTERESTED", "NONE", 0);
        insertInteraction(owner.userId(), information.get(2).informationId(),
                "NONE", "CONTACTED_NOT_SUITABLE", 0);

        insertRun(owner, "PENDING", base.plusMinutes(1), 0);
        insertRun(owner, "FAILED", base.plusMinutes(2), 0);
        RecommendationRunPo newRun = insertRun(owner, "RUNNING", base.plusMinutes(3), 1);
        insertRun(otherOwner, "COMPLETED", base.plusMinutes(4), 0);

        return new Fixture(
                owner,
                otherOwner,
                information,
                oldCompleted.getId(),
                newRun.getId());
    }

    private Owner insertOwner(String suffix) {
        UserAccountPo user = new UserAccountPo();
        user.setUsername("task042-" + suffix + "-" + UUID.randomUUID());
        user.setPasswordHash("$2a$12$task042-not-a-real-secret");
        user.setTimezone("Asia/Shanghai");
        assertEquals(1, userMapper.insert(user));

        AiPromptProfilePo promptProfile = new AiPromptProfilePo();
        promptProfile.setUserId(user.getId());
        promptProfile.setName("TASK-042 " + UUID.randomUUID());
        promptProfile.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        assertEquals(1, promptProfileMapper.insert(promptProfile));

        AiPromptVersionPo promptVersion = new AiPromptVersionPo();
        promptVersion.setPromptProfileId(promptProfile.getId());
        promptVersion.setVersionNo(1);
        promptVersion.setContent("TASK-042 prompt");
        promptVersion.setContentHash(HASH_A);
        assertEquals(1, promptVersionMapper.insert(promptVersion));

        UserRecommendationProfilePo profile = new UserRecommendationProfilePo();
        profile.setUserId(user.getId());
        profile.setInformationType("JOB");
        profile.setAnalysisPromptProfileId(promptProfile.getId());
        profile.setWindowDays(7);
        profile.setTopN(50);
        profile.setContentHash(HASH_A);
        assertEquals(1, profileMapper.insert(profile));

        JobRecommendationProfilePo extension = new JobRecommendationProfilePo();
        extension.setProfileId(profile.getId());
        extension.setTargetRoles("[\"Java 后端\"]");
        extension.setExcludedKeywords("[]");
        assertEquals(1, jobProfileMapper.insert(extension));
        return new Owner(
                user.getId(), promptProfile.getId(), promptVersion.getId(), profile.getId());
    }

    private InformationGraph insertInformationGraph(Owner owner, int sequence, String title) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC).minusDays(sequence);
        InformationItemPo information = new InformationItemPo();
        information.setInformationType("JOB");
        information.setSource("TASK042_TEST");
        information.setSourceItemId(UUID.randomUUID().toString());
        information.setSourceUrl("https://example.test/job/" + sequence);
        information.setTitle(title);
        information.setCollectedAt(now);
        information.setFirstSeenTime(now);
        information.setLastSeenTime(now);
        information.setContentHash(HASH_A);
        information.setCurrentVersionNo(1);
        information.setRawPayload("{}");
        information.setSchemaVersion(1);
        information.setCollectorId("task042-test");
        information.setCollectorVersion("1");
        assertEquals(1, informationMapper.insert(information));

        JobInformationPo job = new JobInformationPo();
        job.setInformationId(information.getId());
        job.setCompanyName("Example " + sequence);
        job.setSalaryText("20-35K");
        job.setLocationName("成都");
        job.setRemoteType("REMOTE");
        job.setJobStatus("ACTIVE");
        job.setDetailStatus("UNKNOWN");
        assertEquals(1, jobMapper.insert(job));

        InformationSnapshotPo snapshot = new InformationSnapshotPo();
        snapshot.setInformationId(information.getId());
        snapshot.setVersionNo(1);
        snapshot.setContentHash(HASH_A);
        snapshot.setTitle(title);
        snapshot.setStandardizedPayload("{\"informationType\":\"JOB\"}");
        snapshot.setRawPayload("{}");
        snapshot.setCollectedAt(now);
        snapshot.setCollectorId("task042-test");
        snapshot.setCollectorVersion("1");
        assertEquals(1, snapshotMapper.insert(snapshot));

        InformationAnalysisPo analysis = new InformationAnalysisPo();
        analysis.setUserId(owner.userId());
        analysis.setInformationId(information.getId());
        analysis.setSnapshotId(snapshot.getId());
        analysis.setInformationType("JOB");
        analysis.setAnalysisDefinitionKey("JOB_USER_RELEVANCE");
        analysis.setAnalysisDefinitionVersion(2);
        analysis.setAnalysisPurpose("USER_RELEVANCE");
        analysis.setPromptProfileId(owner.promptProfileId());
        analysis.setPromptVersionId(owner.promptVersionId());
        analysis.setStatus("SUCCEEDED");
        analysis.setResultJson("{\"relevanceScore\":90}");
        analysis.setRelevanceScore(90);
        assertEquals(1, analysisMapper.insert(analysis));
        return new InformationGraph(information.getId(), snapshot.getId(), analysis.getId());
    }

    private RecommendationRunPo insertRun(
            Owner owner, String status, LocalDateTime time, int resultCount) {
        RecommendationRunPo run = new RecommendationRunPo();
        run.setUserId(owner.userId());
        run.setInformationType("JOB");
        run.setTriggerType("MANUAL");
        run.setProfileId(owner.profileId());
        run.setProfileContentHash(HASH_A);
        run.setProfileSnapshotJson("{\"informationType\":\"JOB\"}");
        run.setPromptProfileId(owner.promptProfileId());
        run.setPromptVersionId(owner.promptVersionId());
        run.setAlgorithmKey("JOB_RECOMMENDATION");
        run.setAlgorithmVersion(1);
        run.setWindowStart(time.minusDays(7));
        run.setWindowEnd(time);
        run.setCandidateCount(resultCount);
        run.setEligibleCount(resultCount);
        run.setResultCount(resultCount);
        run.setStatus(status);
        if (!"PENDING".equals(status)) {
            run.setStartedAt(time.minusSeconds(1));
        }
        if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
            run.setCompletedAt(time);
        }
        assertEquals(1, runMapper.insert(run));
        return run;
    }

    private void insertItem(long runId, InformationGraph graph, int rank) {
        RecommendationItemPo item = new RecommendationItemPo();
        item.setRunId(runId);
        item.setInformationId(graph.informationId());
        item.setSnapshotId(graph.snapshotId());
        item.setAnalysisId(graph.analysisId());
        item.setRankNo(rank);
        item.setFinalScore(new BigDecimal("92.400"));
        item.setAiRelevanceScore(new BigDecimal("95.000"));
        item.setProfileMatchScore(new BigDecimal("86.000"));
        item.setFreshnessScore(new BigDecimal("90.000"));
        item.setScoreBreakdownJson(
                "{\"aiRelevanceScore\":95,\"profileMatchScore\":86,\"freshnessScore\":90}");
        item.setReasonsJson("[\"AI 相关度高\",\"最近进入平台\"]");
        item.setDuplicateGroupKey(String.valueOf(rank).repeat(64));
        assertEquals(1, itemMapper.insert(item));
    }

    private void insertInteraction(
            long userId,
            long informationId,
            String feedback,
            String disposition,
            int viewCount) {
        UserInformationInteractionPo interaction = new UserInformationInteractionPo();
        interaction.setUserId(userId);
        interaction.setInformationId(informationId);
        interaction.setViewCount(viewCount);
        interaction.setFeedbackState(feedback);
        assertEquals(1, interactionMapper.insert(interaction));
        UserJobDispositionPo extension = new UserJobDispositionPo();
        extension.setInteractionId(interaction.getId());
        extension.setJobDisposition(disposition);
        assertEquals(1, dispositionMapper.insert(extension));
    }

    private void clearHardExclusions(Fixture fixture) {
        UserInformationInteractionPo feedback = interactionMapper.selectOwnedInteraction(
                fixture.owner().userId(), fixture.information().get(1).informationId());
        assertThat(interactionMapper.replaceFeedback(
                        fixture.owner().userId(),
                        fixture.information().get(1).informationId(),
                        "NONE",
                        null,
                        LocalDateTime.now(ZoneOffset.UTC)))
                .isPositive();
        assertThat(feedback).isNotNull();

        UserInformationInteractionPo disposition = interactionMapper.selectOwnedInteraction(
                fixture.owner().userId(), fixture.information().get(2).informationId());
        assertThat(disposition).isNotNull();
        assertThat(dispositionMapper.replaceDisposition(
                        disposition.getId(), "NONE", LocalDateTime.now(ZoneOffset.UTC)))
                .isPositive();
    }

    private void completeNewRun(Fixture fixture) {
        // Worker 的完成短事务先插入全部 Item，再原子切换 Run，Feed 不会看到半成品。
        insertItem(fixture.newRunId(), fixture.information().get(2), 1);
        RecommendationRunPo run = runMapper.selectById(fixture.newRunId());
        run.setStatus("COMPLETED");
        run.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
        assertEquals(1, runMapper.updateById(run));
    }

    private void deleteFixture(Fixture fixture) {
        List<Long> informationIds = fixture.information().stream()
                .map(InformationGraph::informationId)
                .toList();
        for (long informationId : informationIds) {
            UserInformationInteractionPo interaction = interactionMapper.selectOwnedInteraction(
                    fixture.owner().userId(), informationId);
            if (interaction != null) {
                dispositionMapper.deleteById(interaction.getId());
                interactionMapper.deleteById(interaction.getId());
            }
        }
        List<RecommendationRunPo> runs = new ArrayList<>(
                runMapper.selectOwnedByType(fixture.owner().userId(), "JOB", 100));
        runs.addAll(runMapper.selectOwnedByType(fixture.otherOwner().userId(), "JOB", 100));
        for (RecommendationRunPo run : runs) {
            itemMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RecommendationItemPo>()
                    .eq("run_id", run.getId()));
            runMapper.deleteById(run.getId());
        }
        for (InformationGraph graph : fixture.information()) {
            analysisMapper.deleteById(graph.analysisId());
            snapshotMapper.deleteById(graph.snapshotId());
            jobMapper.deleteById(graph.informationId());
            informationMapper.deleteById(graph.informationId());
        }
        deleteOwner(fixture.owner());
        deleteOwner(fixture.otherOwner());
    }

    private void deleteOwner(Owner owner) {
        jobProfileMapper.deleteById(owner.profileId());
        profileMapper.deleteById(owner.profileId());
        promptVersionMapper.deleteById(owner.promptVersionId());
        promptProfileMapper.deleteById(owner.promptProfileId());
        userMapper.deleteById(owner.userId());
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    private record Owner(
            /** Owner 用户主键。 */ long userId,
            /** Owner Prompt Profile 主键。 */ long promptProfileId,
            /** Owner Prompt Version 主键。 */ long promptVersionId,
            /** Owner Recommendation Profile 主键。 */ long profileId) {}

    private record InformationGraph(
            /** JOB Information 主键。 */ long informationId,
            /** 不可变 Snapshot 主键。 */ long snapshotId,
            /** 成功 USER_RELEVANCE Analysis 主键。 */ long analysisId) {}

    private record Fixture(
            /** 当前 Feed Owner。 */ Owner owner,
            /** 用于验证隔离的另一 Owner。 */ Owner otherOwner,
            /** 当前 Owner 的三个 JOB 数据图。 */ List<InformationGraph> information,
            /** 旧成功 Feed Run 主键。 */ long oldCompletedRunId,
            /** 之后切换为成功的新 Run 主键。 */ long newRunId) {}
}
