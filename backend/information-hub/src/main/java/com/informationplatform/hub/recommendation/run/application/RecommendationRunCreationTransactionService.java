package com.informationplatform.hub.recommendation.run.application;

import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
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
import com.informationplatform.hub.recommendation.run.domain.RecommendationRunAccepted;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manual Refresh 原子冻结 Profile、Prompt Version、算法和时间窗口的短事务。 */
@Service
public class RecommendationRunCreationTransactionService {

    /** Recommendation Profile Core 行锁与读取。 */
    private final UserRecommendationProfileMapper profileMapper;
    /** JOB Profile Extension 读取。 */
    private final JobRecommendationProfileMapper jobProfileMapper;
    /** Prompt Profile Active Version 与 Owner 校验。 */
    private final AiPromptProfileMapper promptProfileMapper;
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
            RecommendationRunMapper runMapper,
            JobRecommendationProfileJsonCodec profileJsonCodec,
            JobRecommendationRunProfileSnapshotCodec snapshotCodec) {
        this.profileMapper = profileMapper;
        this.jobProfileMapper = jobProfileMapper;
        this.promptProfileMapper = promptProfileMapper;
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
}
