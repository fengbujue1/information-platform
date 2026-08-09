package com.informationplatform.hub.recommendation.run.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.RecommendationItemMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.RecommendationRunMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.RecommendationItemPo;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.RecommendationRunPo;
import com.informationplatform.hub.recommendation.job.candidate.domain.JobRecommendationCandidate;
import com.informationplatform.hub.recommendation.job.ranking.domain.JobRecommendationRankedCandidate;
import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScore;
import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScoreBreakdown;
import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScoredCandidate;
import com.informationplatform.hub.recommendation.run.domain.RecommendationRunExecutionResult;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

/** 验证 Run claim、stale recovery、Item 完成、NOOP 与失败状态的短事务规则。 */
class RecommendationRunWorkerTransactionServiceTest {

    @Test
    void claimsPendingRunAsRunning() {
        Fixture fixture = fixture();
        RecommendationRunPo run = run("PENDING");
        when(fixture.runs.selectNextPendingForUpdate()).thenReturn(run);
        when(fixture.runs.updateById(run)).thenReturn(1);

        var work = fixture.service.claimNext();

        assertThat(run.getStatus()).isEqualTo("RUNNING");
        assertThat(run.getStartedAt()).isNotNull();
        assertThat(work.runId()).isEqualTo(41);
        assertThat(work.promptVersionId()).isEqualTo(32);
    }

    @Test
    void recoversOnlySelectedStaleRunningRunsToPending() {
        Fixture fixture = fixture();
        RecommendationRunPo stale = run("RUNNING");
        stale.setStartedAt(LocalDateTime.now().minusHours(1));
        when(fixture.runs.selectStaleRunningForUpdate(any())).thenReturn(List.of(stale));
        when(fixture.runs.updateById(stale)).thenReturn(1);

        assertThat(fixture.service.recoverStale(Duration.ofMinutes(5))).isEqualTo(1);
        assertThat(stale.getStatus()).isEqualTo("PENDING");
        assertThat(stale.getStartedAt()).isNull();
    }

    @Test
    void atomicallyBuildsImmutableItemsAndCompletesRun() {
        Fixture fixture = fixture();
        RecommendationRunPo run = run("RUNNING");
        when(fixture.runs.selectByIdForUpdate(41)).thenReturn(run);
        when(fixture.runs.updateById(run)).thenReturn(1);
        when(fixture.items.insert(any(RecommendationItemPo.class))).thenReturn(1);

        var completion = fixture.service.complete(
                41, new RecommendationRunExecutionResult(5, 3, List.of(ranked(51, 1))));

        assertThat(completion.status()).isEqualTo("COMPLETED");
        assertThat(run.getCandidateCount()).isEqualTo(5);
        assertThat(run.getEligibleCount()).isEqualTo(3);
        assertThat(run.getResultCount()).isEqualTo(1);
        ArgumentCaptor<RecommendationItemPo> item =
                ArgumentCaptor.forClass(RecommendationItemPo.class);
        verify(fixture.items).insert(item.capture());
        assertThat(item.getValue().getRunId()).isEqualTo(41);
        assertThat(item.getValue().getRankNo()).isEqualTo(1);
        assertThat(item.getValue().getScoreBreakdownJson()).contains("aiRelevanceScore");
        assertThat(item.getValue().getReasonsJson()).isEqualTo("[\"AI 相关度高\"]");
    }

    @Test
    void doesNotPublishRunCompletionWhenAnyImmutableItemInsertFails() {
        Fixture fixture = fixture();
        RecommendationRunPo run = run("RUNNING");
        when(fixture.runs.selectByIdForUpdate(41)).thenReturn(run);
        when(fixture.items.insert(any(RecommendationItemPo.class)))
                .thenReturn(1)
                .thenThrow(new DuplicateKeyException("duplicate rank"));

        assertThatThrownBy(() -> fixture.service.complete(
                        41,
                        new RecommendationRunExecutionResult(
                                2, 2, List.of(ranked(51, 1), ranked(52, 2)))))
                .isInstanceOf(DuplicateKeyException.class);

        // Run 终态只在全部 Item 成功写入后更新；事务代理会同时回滚此前 Item。
        verify(fixture.runs, never()).updateById(run);
        assertThat(run.getStatus()).isEqualTo("RUNNING");
    }

    @Test
    void completesEmptyResultAsNoopAndCanFailCurrentRun() {
        Fixture fixture = fixture();
        RecommendationRunPo noop = run("RUNNING");
        when(fixture.runs.selectByIdForUpdate(41)).thenReturn(noop);
        when(fixture.runs.updateById(noop)).thenReturn(1);

        var completion = fixture.service.complete(
                41, new RecommendationRunExecutionResult(0, 0, List.of()));

        assertThat(completion.status()).isEqualTo("NOOP");
        assertThat(noop.getSkipReason()).isEqualTo("NO_ELIGIBLE_CANDIDATES");

        RecommendationRunPo failed = run("RUNNING");
        when(fixture.runs.selectByIdForUpdate(42)).thenReturn(failed);
        when(fixture.runs.updateById(failed)).thenReturn(1);
        fixture.service.fail(42, "RECOMMENDATION_RUN_EXECUTION_FAILED");
        assertThat(failed.getStatus()).isEqualTo("FAILED");
        assertThat(failed.getFailureMessage()).doesNotContain("payload", "token");
    }

    private Fixture fixture() {
        RecommendationRunMapper runs = mock(RecommendationRunMapper.class);
        RecommendationItemMapper items = mock(RecommendationItemMapper.class);
        return new Fixture(
                runs,
                items,
                new RecommendationRunWorkerTransactionService(
                        runs, items, new ObjectMapper()));
    }

    private RecommendationRunPo run(String status) {
        RecommendationRunPo run = new RecommendationRunPo();
        run.setId(41L);
        run.setUserId(7L);
        run.setInformationType("JOB");
        run.setProfileId(21L);
        run.setProfileSnapshotJson("{\"schemaVersion\":1}");
        run.setPromptProfileId(31L);
        run.setPromptVersionId(32L);
        run.setAlgorithmKey("JOB_RECOMMENDATION");
        run.setAlgorithmVersion(1);
        run.setWindowStart(LocalDateTime.of(2026, 8, 1, 0, 0));
        run.setWindowEnd(LocalDateTime.of(2026, 8, 8, 0, 0));
        run.setStatus(status);
        return run;
    }

    private JobRecommendationRankedCandidate ranked(long informationId, int rankNo) {
        ObjectMapper mapper = new ObjectMapper();
        JobRecommendationCandidate candidate = new JobRecommendationCandidate(
                informationId,
                informationId + 100,
                informationId + 200,
                2,
                LocalDateTime.of(2026, 8, 7, 0, 0),
                "Java 后端",
                "正文",
                mapper.createObjectNode().set(
                        "job",
                        mapper.createObjectNode()
                                .put("companyName", "测试公司")
                                .put("cityName", "上海")),
                mapper.createObjectNode().put("relevanceScore", 90),
                90);
        JobRecommendationScore score = new JobRecommendationScore(
                "JOB_RECOMMENDATION",
                1,
                decimal("90.000"),
                new JobRecommendationScoreBreakdown(
                        decimal("90.000"), decimal("90.000"), decimal("90.000")),
                List.of("AI 相关度高"));
        return new JobRecommendationRankedCandidate(
                new JobRecommendationScoredCandidate(candidate, score),
                rankNo,
                "a".repeat(64));
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private record Fixture(
            RecommendationRunMapper runs,
            RecommendationItemMapper items,
            RecommendationRunWorkerTransactionService service) {
    }
}
