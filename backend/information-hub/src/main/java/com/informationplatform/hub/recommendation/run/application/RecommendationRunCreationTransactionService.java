package com.informationplatform.hub.recommendation.run.application;

import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.RecommendationRunMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.UserRecommendationProfileMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.RecommendationRunPo;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.UserRecommendationProfilePo;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.JobRecommendationProfileJsonCodec;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.mapper.JobRecommendationProfileMapper;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.po.JobRecommendationProfilePo;
import com.informationplatform.hub.recommendation.job.run.domain.JobRecommendationRunProfileSnapshot;
import com.informationplatform.hub.recommendation.job.run.infrastructure.JobRecommendationRunProfileSnapshotCodec;
import com.informationplatform.hub.recommendation.job.scoring.application.JobRecommendationScorer;
import com.informationplatform.hub.recommendation.run.domain.RecommendationAutoTriggerOutcome;
import com.informationplatform.hub.recommendation.run.domain.RecommendationRunAccepted;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Manual 与 Analysis Batch Auto Trigger 共用的 Run 冻结创建短事务。 */
@Service
public class RecommendationRunCreationTransactionService {

    /** Recommendation Profile Core 行锁与读取。 */
    private final UserRecommendationProfileMapper profileMapper;
    /** JOB Profile Extension 读取。 */
    private final JobRecommendationProfileMapper jobProfileMapper;
    /** Prompt Profile Active Version 与 Owner 校验。 */
    private final AiPromptProfileMapper promptProfileMapper;
    /** Auto Trigger 来源 Analysis Batch 读取。 */
    private final AiAnalysisBatchMapper analysisBatchMapper;
    /** Run 创建与冲突查询。 */
    private final RecommendationRunMapper runMapper;
    /** JOB Profile 数组 JSON 解码。 */
    private final JobRecommendationProfileJsonCodec profileJsonCodec;
    /** 完整 Profile snapshot JSON 编码。 */
    private final JobRecommendationRunProfileSnapshotCodec snapshotCodec;

    public RecommendationRunCreationTransactionService(
            UserRecommendationProfileMapper profileMapper,
            JobRecommendationProfileMapper jobProfileMapper,
            AiPromptProfileMapper promptProfileMapper,
            AiAnalysisBatchMapper analysisBatchMapper,
            RecommendationRunMapper runMapper,
            JobRecommendationProfileJsonCodec profileJsonCodec,
            JobRecommendationRunProfileSnapshotCodec snapshotCodec) {
        this.profileMapper = profileMapper;
        this.jobProfileMapper = jobProfileMapper;
        this.promptProfileMapper = promptProfileMapper;
        this.analysisBatchMapper = analysisBatchMapper;
        this.runMapper = runMapper;
        this.profileJsonCodec = profileJsonCodec;
        this.snapshotCodec = snapshotCodec;
    }

    /**
     * 创建一个 PENDING Manual Run。
     *
     * <p>Profile 行锁串行化同 Owner/Type 的冲突检查，避免两个并发请求同时观察到无活动 Run。
     */
    @Transactional
    public RecommendationRunAccepted createManual(long userId, String informationType) {
        UserRecommendationProfilePo profile =
                profileMapper.selectOwnedByTypeForUpdate(userId, informationType);
        if (profile == null) {
            throw new RecommendationRunNotFoundException(
                    "RECOMMENDATION_PROFILE_NOT_FOUND",
                    "Recommendation Profile does not exist");
        }
        JobRecommendationProfilePo jobProfile = jobProfileMapper.selectById(profile.getId());
        if (jobProfile == null) {
            throw persistence("JOB Recommendation Profile extension does not exist");
        }
        AiPromptProfilePo promptProfile = promptProfileMapper.selectOwnedById(
                profile.getAnalysisPromptProfileId(), userId);
        if (promptProfile == null) {
            throw new RecommendationRunNotFoundException(
                    "RECOMMENDATION_PROMPT_PROFILE_NOT_FOUND",
                    "Prompt Profile does not exist");
        }
        if (!"JOB_USER_RELEVANCE".equals(promptProfile.getAnalysisDefinitionKey())
                || !"ACTIVE".equals(promptProfile.getStatus())
                || promptProfile.getActiveVersionId() == null) {
            throw new RecommendationRunConflictException(
                    "RECOMMENDATION_PROMPT_ACTIVE_VERSION_REQUIRED",
                    "Bound Prompt Profile must be active and have an active version");
        }

        if (runMapper.selectActiveManualForUpdate(userId, informationType) != null) {
            throw new RecommendationRunConflictException(
                    "RECOMMENDATION_RUN_IN_PROGRESS",
                    "A Manual Recommendation Run is already in progress");
        }

        JobRecommendationRunProfileSnapshot snapshot = snapshot(profile, jobProfile);
        LocalDateTime windowEnd = LocalDateTime.now(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MILLIS);
        RecommendationRunPo run = new RecommendationRunPo();
        run.setUserId(userId);
        run.setInformationType(informationType);
        run.setTriggerType("MANUAL");
        run.setProfileId(profile.getId());
        run.setProfileContentHash(profile.getContentHash());
        run.setProfileSnapshotJson(snapshotCodec.encode(snapshot));
        run.setPromptProfileId(promptProfile.getId());
        run.setPromptVersionId(promptProfile.getActiveVersionId());
        run.setAlgorithmKey(JobRecommendationScorer.ALGORITHM_KEY);
        run.setAlgorithmVersion(JobRecommendationScorer.ALGORITHM_VERSION);
        run.setWindowStart(windowEnd.minusDays(profile.getWindowDays()));
        run.setWindowEnd(windowEnd);
        run.setCandidateCount(0);
        run.setEligibleCount(0);
        run.setResultCount(0);
        run.setStatus("PENDING");
        if (runMapper.insert(run) != 1 || run.getId() == null) {
            throw persistence("Recommendation Run insert affected no row");
        }
        return new RecommendationRunAccepted(run.getId(), run.getStatus());
    }

    /**
     * 从已提交的 Analysis Batch 终态创建幂等 Auto Run。
     *
     * <p>Auto Run 冻结当前完整 Recommendation Profile，但 Prompt Version 必须使用来源 Batch 的不可变版本。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RecommendationAutoTriggerOutcome createAuto(long batchId) {
        AiAnalysisBatchPo batch = analysisBatchMapper.selectById(batchId);
        if (batch == null) {
            return RecommendationAutoTriggerOutcome.skipped("SOURCE_BATCH_NOT_FOUND");
        }
        if (!"COMPLETED".equals(batch.getStatus())
                && !"PARTIAL_FAILED".equals(batch.getStatus())) {
            return RecommendationAutoTriggerOutcome.skipped(
                    "SOURCE_BATCH_STATUS_" + batch.getStatus());
        }
        if (!"MANUAL".equals(batch.getTriggerType())
                && !"SCHEDULED".equals(batch.getTriggerType())) {
            return RecommendationAutoTriggerOutcome.skipped("SOURCE_BATCH_TRIGGER_UNSUPPORTED");
        }
        if (!"JOB".equals(batch.getInformationType())) {
            return RecommendationAutoTriggerOutcome.skipped("INFORMATION_TYPE_UNSUPPORTED");
        }

        RecommendationRunPo existing =
                runMapper.selectBySourceAnalysisBatchId(batch.getId());
        if (existing != null) {
            return RecommendationAutoTriggerOutcome.duplicate(existing.getId());
        }

        // Profile 行锁冻结一个一致的 Core + JOB Extension，并串行化同 Profile 的重复事件。
        UserRecommendationProfilePo profile = profileMapper.selectOwnedByTypeForUpdate(
                batch.getUserId(), batch.getInformationType());
        if (profile == null) {
            return RecommendationAutoTriggerOutcome.skipped("RECOMMENDATION_PROFILE_NOT_FOUND");
        }
        if (!profile.getAnalysisPromptProfileId().equals(batch.getPromptProfileId())) {
            return RecommendationAutoTriggerOutcome.skipped("PROMPT_PROFILE_MISMATCH");
        }
        JobRecommendationProfilePo jobProfile = jobProfileMapper.selectById(profile.getId());
        if (jobProfile == null) {
            throw persistence("JOB Recommendation Profile extension does not exist");
        }

        LocalDateTime windowEnd = LocalDateTime.now(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MILLIS);
        RecommendationRunPo run = new RecommendationRunPo();
        run.setUserId(batch.getUserId());
        run.setInformationType(batch.getInformationType());
        run.setTriggerType("ANALYSIS_BATCH_COMPLETED");
        run.setSourceAnalysisBatchId(batch.getId());
        run.setProfileId(profile.getId());
        run.setProfileContentHash(profile.getContentHash());
        run.setProfileSnapshotJson(snapshotCodec.encode(snapshot(profile, jobProfile)));
        run.setPromptProfileId(batch.getPromptProfileId());
        run.setPromptVersionId(batch.getPromptVersionId());
        run.setAlgorithmKey(JobRecommendationScorer.ALGORITHM_KEY);
        run.setAlgorithmVersion(JobRecommendationScorer.ALGORITHM_VERSION);
        run.setWindowStart(windowEnd.minusDays(profile.getWindowDays()));
        run.setWindowEnd(windowEnd);
        run.setCandidateCount(0);
        run.setEligibleCount(0);
        run.setResultCount(0);
        run.setStatus("PENDING");
        insertRun(run);
        return RecommendationAutoTriggerOutcome.created(run.getId());
    }

    private JobRecommendationRunProfileSnapshot snapshot(
            UserRecommendationProfilePo core, JobRecommendationProfilePo extension) {
        return new JobRecommendationRunProfileSnapshot(
                1,
                core.getId(),
                core.getInformationType(),
                core.getAnalysisPromptProfileId(),
                core.getWindowDays(),
                core.getTopN(),
                profileJsonCodec.decode(extension.getTargetRoles()),
                profileJsonCodec.decode(extension.getPreferredSkills()),
                profileJsonCodec.decode(extension.getPreferredCities()),
                profileJsonCodec.decode(extension.getPreferredRemoteTypes()),
                extension.getSalaryMinMonthlyYuan(),
                profileJsonCodec.decode(extension.getExcludedKeywords()));
    }

    private RecommendationRunPersistenceException persistence(String message) {
        return new RecommendationRunPersistenceException(message);
    }

    private void insertRun(RecommendationRunPo run) {
        if (runMapper.insert(run) != 1 || run.getId() == null) {
            throw persistence("Recommendation Run insert affected no row");
        }
    }
}
