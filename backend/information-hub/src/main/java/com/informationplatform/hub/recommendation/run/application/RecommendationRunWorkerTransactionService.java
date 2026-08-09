package com.informationplatform.hub.recommendation.run.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.RecommendationItemMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.RecommendationRunMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.RecommendationItemPo;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.RecommendationRunPo;
import com.informationplatform.hub.recommendation.job.ranking.domain.JobRecommendationRankedCandidate;
import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScore;
import com.informationplatform.hub.recommendation.run.domain.RecommendationRunCompletion;
import com.informationplatform.hub.recommendation.run.domain.RecommendationRunExecutionResult;
import com.informationplatform.hub.recommendation.run.domain.RecommendationRunWork;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Worker PENDING claim、stale recovery、原子完成和失败终结的短事务边界。 */
@Service
public class RecommendationRunWorkerTransactionService {

    /** Run 行锁与状态持久化。 */
    private final RecommendationRunMapper runMapper;
    /** 不可变 Item 插入。 */
    private final RecommendationItemMapper itemMapper;
    /** score breakdown 和 reasons JSON 编码。 */
    private final ObjectMapper objectMapper;

    public RecommendationRunWorkerTransactionService(
            RecommendationRunMapper runMapper,
            RecommendationItemMapper itemMapper,
            ObjectMapper objectMapper) {
        this.runMapper = runMapper;
        this.itemMapper = itemMapper;
        this.objectMapper = objectMapper;
    }

    /** 使用 SKIP LOCKED 领取一个 PENDING Run，并在同一短事务转为 RUNNING。 */
    @Transactional
    public RecommendationRunWork claimNext() {
        RecommendationRunPo run = runMapper.selectNextPendingForUpdate();
        if (run == null) {
            return null;
        }
        run.setStatus("RUNNING");
        run.setSkipReason(null);
        run.setFailureCode(null);
        run.setFailureMessage(null);
        run.setStartedAt(utcNow());
        run.setCompletedAt(null);
        updateRun(run);
        return toWork(run);
    }

    /** 将超过阈值的 RUNNING Run 恢复为 PENDING；Item 仅在完成事务插入，因此无需清理半成品。 */
    @Transactional
    public int recoverStale(Duration staleAfter) {
        if (staleAfter == null || staleAfter.isZero() || staleAfter.isNegative()) {
            throw new IllegalArgumentException("Recommendation stale duration must be positive");
        }
        LocalDateTime cutoff = utcNow().minus(staleAfter);
        List<RecommendationRunPo> staleRuns = runMapper.selectStaleRunningForUpdate(cutoff);
        for (RecommendationRunPo run : staleRuns) {
            run.setStatus("PENDING");
            run.setStartedAt(null);
            run.setCompletedAt(null);
            run.setFailureCode(null);
            run.setFailureMessage(null);
            updateRun(run);
        }
        return staleRuns.size();
    }

    /**
     * 在单个短事务中插入全部不可变 Item 并完成 Run。
     *
     * <p>任一 Item 插入失败会回滚全部 Item 和 Run 终态，避免 Feed 观察到部分结果。
     */
    @Transactional
    public RecommendationRunCompletion complete(
            long runId, RecommendationRunExecutionResult result) {
        RecommendationRunPo run = requireRunningForUpdate(runId);
        for (JobRecommendationRankedCandidate ranked : result.rankedCandidates()) {
            if (itemMapper.insert(toItem(runId, ranked)) != 1) {
                throw persistence("Recommendation Item insert affected no row");
            }
        }
        LocalDateTime completedAt = utcNow();
        run.setCandidateCount(Math.toIntExact(result.candidateCount()));
        run.setEligibleCount(Math.toIntExact(result.eligibleCount()));
        run.setResultCount(result.rankedCandidates().size());
        run.setCompletedAt(completedAt);
        if (result.rankedCandidates().isEmpty()) {
            run.setStatus("NOOP");
            run.setSkipReason("NO_ELIGIBLE_CANDIDATES");
        } else {
            run.setStatus("COMPLETED");
            run.setSkipReason(null);
        }
        updateRun(run);
        return new RecommendationRunCompletion(run.getStatus(), run.getResultCount());
    }

    /** 将仍处于 RUNNING 的当前 Run 标记 FAILED，不修改任何历史 COMPLETED Run。 */
    @Transactional
    public void fail(long runId, String failureCode) {
        RecommendationRunPo run = requireRunningForUpdate(runId);
        run.setStatus("FAILED");
        run.setFailureCode(failureCode);
        run.setFailureMessage("Recommendation Run execution failed");
        run.setCompletedAt(utcNow());
        updateRun(run);
    }

    private RecommendationRunPo requireRunningForUpdate(long runId) {
        RecommendationRunPo run = runMapper.selectByIdForUpdate(runId);
        if (run == null || !"RUNNING".equals(run.getStatus())) {
            throw persistence("RUNNING Recommendation Run does not exist");
        }
        return run;
    }

    private RecommendationRunWork toWork(RecommendationRunPo run) {
        return new RecommendationRunWork(
                run.getId(),
                run.getUserId(),
                run.getInformationType(),
                run.getProfileId(),
                run.getProfileSnapshotJson(),
                run.getPromptProfileId(),
                run.getPromptVersionId(),
                run.getAlgorithmKey(),
                run.getAlgorithmVersion(),
                run.getWindowStart(),
                run.getWindowEnd());
    }

    private RecommendationItemPo toItem(
            long runId, JobRecommendationRankedCandidate ranked) {
        JobRecommendationScore score = ranked.scoredCandidate().score();
        RecommendationItemPo item = new RecommendationItemPo();
        item.setRunId(runId);
        item.setInformationId(ranked.scoredCandidate().candidate().informationId());
        item.setSnapshotId(ranked.scoredCandidate().candidate().snapshotId());
        item.setAnalysisId(ranked.scoredCandidate().candidate().analysisId());
        item.setRankNo(ranked.rankNo());
        item.setFinalScore(score.finalScore());
        item.setAiRelevanceScore(score.scoreBreakdown().aiRelevanceScore());
        item.setProfileMatchScore(score.scoreBreakdown().profileMatchScore());
        item.setFreshnessScore(score.scoreBreakdown().freshnessScore());
        item.setScoreBreakdownJson(encode(score.scoreBreakdown()));
        item.setReasonsJson(encode(score.reasons()));
        item.setDuplicateGroupKey(ranked.duplicateGroupKey());
        return item;
    }

    private String encode(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new RecommendationRunPersistenceException(
                    "Recommendation Item JSON could not be encoded", exception);
        }
    }

    private void updateRun(RecommendationRunPo run) {
        if (runMapper.updateById(run) != 1) {
            throw persistence("Recommendation Run update affected no row");
        }
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private RecommendationRunPersistenceException persistence(String message) {
        return new RecommendationRunPersistenceException(message);
    }
}
