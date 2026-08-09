package com.informationplatform.hub.recommendation.run.application;

import com.informationplatform.hub.recommendation.job.candidate.application.JobRecommendationCandidateResolver;
import com.informationplatform.hub.recommendation.job.candidate.domain.JobRecommendationCandidateRequest;
import com.informationplatform.hub.recommendation.job.candidate.domain.JobRecommendationCandidateResolution;
import com.informationplatform.hub.recommendation.job.ranking.application.JobRecommendationRanker;
import com.informationplatform.hub.recommendation.job.ranking.domain.JobRecommendationRankingRequest;
import com.informationplatform.hub.recommendation.job.ranking.domain.JobRecommendationRankingResult;
import com.informationplatform.hub.recommendation.job.run.domain.JobRecommendationRunProfileSnapshot;
import com.informationplatform.hub.recommendation.job.run.infrastructure.JobRecommendationRunProfileSnapshotCodec;
import com.informationplatform.hub.recommendation.job.scoring.application.JobRecommendationScorer;
import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScoredCandidate;
import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScoringRequest;
import com.informationplatform.hub.recommendation.run.domain.RecommendationRunExecutionResult;
import com.informationplatform.hub.recommendation.run.domain.RecommendationRunWork;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 在数据库事务外串联 Candidate、Scoring 与 Ranking 的确定性 Run 引擎。 */
@Service
public class RecommendationRunExecutionService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RecommendationRunExecutionService.class);

    /** JOB Candidate Resolver。 */
    private final JobRecommendationCandidateResolver candidateResolver;
    /** JOB_RECOMMENDATION V1 Scorer。 */
    private final JobRecommendationScorer scorer;
    /** JOB_RECOMMENDATION V1 Ranker。 */
    private final JobRecommendationRanker ranker;
    /** 冻结 Profile snapshot 解码器。 */
    private final JobRecommendationRunProfileSnapshotCodec snapshotCodec;

    public RecommendationRunExecutionService(
            JobRecommendationCandidateResolver candidateResolver,
            JobRecommendationScorer scorer,
            JobRecommendationRanker ranker,
            JobRecommendationRunProfileSnapshotCodec snapshotCodec) {
        this.candidateResolver = candidateResolver;
        this.scorer = scorer;
        this.ranker = ranker;
        this.snapshotCodec = snapshotCodec;
    }

    /** 使用 Run 冻结输入执行本地计算；不读取当前 Profile、不调用 Provider、不持有事务。 */
    public RecommendationRunExecutionResult execute(RecommendationRunWork work) {
        JobRecommendationRunProfileSnapshot snapshot =
                snapshotCodec.decode(work.profileSnapshotJson());
        validateFrozenIdentity(work, snapshot);
        long candidateStartedNanos = System.nanoTime();
        JobRecommendationCandidateResolution candidates = candidateResolver.resolve(
                new JobRecommendationCandidateRequest(
                        work.userId(),
                        work.promptVersionId(),
                        work.windowStart(),
                        work.windowEnd(),
                        snapshot.preferences()));
        LOGGER.info(
                "Recommendation candidates resolved, recommendationRunId={}, candidateCount={}, eligibleCount={}, durationMs={}",
                work.runId(),
                candidates.candidateCount(),
                candidates.eligibleCount(),
                elapsedMillis(candidateStartedNanos));
        long scoringStartedNanos = System.nanoTime();
        List<JobRecommendationScoredCandidate> scored = scorer.scoreAll(
                new JobRecommendationScoringRequest(
                        candidates.candidates(),
                        snapshot.preferences(),
                        work.windowStart(),
                        work.windowEnd()));
        JobRecommendationRankingResult ranking = ranker.rank(
                new JobRecommendationRankingRequest(scored, snapshot.topN()));
        LOGGER.info(
                "Recommendation scoring completed, recommendationRunId={}, scoredCount={}, resultCount={}, durationMs={}",
                work.runId(),
                scored.size(),
                ranking.rankedCandidates().size(),
                elapsedMillis(scoringStartedNanos));
        return new RecommendationRunExecutionResult(
                candidates.candidateCount(),
                candidates.eligibleCount(),
                ranking.rankedCandidates());
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000);
    }

    private void validateFrozenIdentity(
            RecommendationRunWork work, JobRecommendationRunProfileSnapshot snapshot) {
        if (snapshot.profileId() != work.profileId()
                || !snapshot.informationType().equals(work.informationType())
                || snapshot.analysisPromptProfileId() != work.promptProfileId()) {
            throw new RecommendationRunPersistenceException(
                    "Recommendation Run frozen Profile identity is inconsistent");
        }
    }
}
