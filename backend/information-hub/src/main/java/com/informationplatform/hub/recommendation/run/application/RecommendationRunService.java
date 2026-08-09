package com.informationplatform.hub.recommendation.run.application;

import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.RecommendationRunMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.RecommendationRunPo;
import com.informationplatform.hub.recommendation.run.domain.RecommendationRunAccepted;
import com.informationplatform.hub.recommendation.run.domain.RecommendationRunView;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 提供 Manual Refresh 和 Owner/Information Type 隔离的 Run 查询。 */
@Service
public class RecommendationRunService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationRunService.class);
    private static final String JOB = "JOB";

    /** 从 Session 获取不可伪造 Owner。 */
    private final CurrentUserProvider currentUserProvider;
    /** Manual Run 创建短事务。 */
    private final RecommendationRunCreationTransactionService creationTransactions;
    /** Run Owner 查询 Mapper。 */
    private final RecommendationRunMapper runMapper;

    public RecommendationRunService(
            CurrentUserProvider currentUserProvider,
            RecommendationRunCreationTransactionService creationTransactions,
            RecommendationRunMapper runMapper) {
        this.currentUserProvider = currentUserProvider;
        this.creationTransactions = creationTransactions;
        this.runMapper = runMapper;
    }

    /** Manual Refresh 只创建 PENDING Run，不调用 Preview、Analysis Batch 或 Provider。 */
    public RecommendationRunAccepted refresh(String informationType) {
        String type = requireSupportedInformationType(informationType);
        long userId = currentUserProvider.requireCurrentUser().id();
        LOGGER.info("Manual Recommendation refresh requested, informationType={}", type);
        RecommendationRunAccepted accepted = creationTransactions.createManual(userId, type);
        LOGGER.info(
                "Recommendation run created, recommendationRunId={}, triggerType=MANUAL, informationType={}",
                accepted.runId(),
                type);
        return accepted;
    }

    /** 返回当前 Owner/Type 最近 Run；默认 20，最大 100。 */
    @Transactional(readOnly = true)
    public List<RecommendationRunView> list(String informationType, Integer requestedLimit) {
        String type = requireSupportedInformationType(informationType);
        int limit = requestedLimit == null ? 20 : requestedLimit;
        if (limit < 1 || limit > 100) {
            throw request("RECOMMENDATION_RUN_LIMIT_INVALID", "limit must be between 1 and 100");
        }
        long userId = currentUserProvider.requireCurrentUser().id();
        return runMapper.selectOwnedByType(userId, type, limit).stream()
                .map(this::toView)
                .toList();
    }

    /** 返回当前 Owner/Type 的 Run；跨 Owner、跨 Type 与不存在统一为 404。 */
    @Transactional(readOnly = true)
    public RecommendationRunView get(String informationType, long runId) {
        String type = requireSupportedInformationType(informationType);
        if (runId <= 0) {
            throw request("RECOMMENDATION_RUN_ID_INVALID", "runId must be positive");
        }
        long userId = currentUserProvider.requireCurrentUser().id();
        RecommendationRunPo run = runMapper.selectOwnedByIdAndType(runId, userId, type);
        if (run == null) {
            throw new RecommendationRunNotFoundException(
                    "RECOMMENDATION_RUN_NOT_FOUND", "Recommendation Run does not exist");
        }
        return toView(run);
    }

    private String requireSupportedInformationType(String informationType) {
        if (!JOB.equals(informationType)) {
            throw request(
                    "RECOMMENDATION_INFORMATION_TYPE_UNSUPPORTED",
                    "Only JOB Recommendation is supported");
        }
        return JOB;
    }

    private RecommendationRunView toView(RecommendationRunPo run) {
        return new RecommendationRunView(
                run.getId(),
                run.getInformationType(),
                run.getTriggerType(),
                run.getSourceAnalysisBatchId(),
                run.getProfileId(),
                run.getProfileContentHash(),
                run.getPromptProfileId(),
                run.getPromptVersionId(),
                run.getAlgorithmKey(),
                run.getAlgorithmVersion(),
                run.getWindowStart(),
                run.getWindowEnd(),
                run.getCandidateCount(),
                run.getEligibleCount(),
                run.getResultCount(),
                run.getStatus(),
                run.getSkipReason(),
                run.getFailureCode(),
                run.getFailureMessage(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getCreatedAt(),
                run.getUpdatedAt());
    }

    private RecommendationRunRequestException request(String code, String message) {
        return new RecommendationRunRequestException(code, message);
    }
}
