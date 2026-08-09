package com.informationplatform.hub.recommendation.feed.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.recommendation.feed.domain.RecommendationFeedView;
import com.informationplatform.hub.recommendation.feed.infrastructure.persistence.RecommendationFeedQueryMapper;
import com.informationplatform.hub.recommendation.feed.infrastructure.persistence.RecommendationFeedQueryRow;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.RecommendationRunMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.UserRecommendationProfileMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.RecommendationRunPo;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.UserRecommendationProfilePo;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 读取最近成功预计算 Run，并应用当前 Interaction visibility。 */
@Service
public class RecommendationFeedService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationFeedService.class);
    private static final String JOB = "JOB";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final TypeReference<List<String>> REASONS_TYPE = new TypeReference<>() {};

    /** 从 Session 认证上下文获取可信 Owner。 */
    private final CurrentUserProvider currentUserProvider;
    /** 选择同 Owner/Type 最近成功 Run。 */
    private final RecommendationRunMapper runMapper;
    /** 读取当前 Profile hash 以计算 stale 标志。 */
    private final UserRecommendationProfileMapper profileMapper;
    /** 在已选 Run 上执行可见性统计与分页。 */
    private final RecommendationFeedQueryMapper feedQueryMapper;
    /** 解码持久化的稳定 reasons JSON。 */
    private final ObjectMapper objectMapper;

    public RecommendationFeedService(
            CurrentUserProvider currentUserProvider,
            RecommendationRunMapper runMapper,
            UserRecommendationProfileMapper profileMapper,
            RecommendationFeedQueryMapper feedQueryMapper,
            ObjectMapper objectMapper) {
        this.currentUserProvider = currentUserProvider;
        this.runMapper = runMapper;
        this.profileMapper = profileMapper;
        this.feedQueryMapper = feedQueryMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 在同一只读事务快照中选择成功 Run、应用 current visibility 并分页。
     *
     * <p>PENDING/RUNNING/FAILED 不参与选择，因此旧成功 Feed 会一直保留到新 Run COMPLETED。
     */
    @Transactional(readOnly = true)
    public RecommendationFeedView get(String informationType, Integer requestedPage, Integer requestedPageSize) {
        long startedNanos = System.nanoTime();
        String type = requireSupportedInformationType(informationType);
        int page = requestedPage == null ? DEFAULT_PAGE : requestedPage;
        int pageSize = requestedPageSize == null ? DEFAULT_PAGE_SIZE : requestedPageSize;
        requirePagination(page, pageSize);
        long userId = currentUserProvider.requireCurrentUser().id();

        try {
            RecommendationRunPo run = runMapper.selectLatestCompleted(userId, type);
            if (run == null) {
                logCompleted(type, page, pageSize, null, 0, startedNanos);
                return new RecommendationFeedView(null, page, pageSize, 0, List.of());
            }
            UserRecommendationProfilePo profile = profileMapper.selectOwnedByType(userId, type);
            boolean profileChanged = profile == null
                    || !run.getProfileContentHash().equals(profile.getContentHash());
            long total = feedQueryMapper.countVisible(run.getId(), userId);
            long offset = ((long) page - 1L) * pageSize;
            List<RecommendationFeedView.Item> items = total == 0 || offset >= total
                    ? List.of()
                    : feedQueryMapper.selectVisible(run.getId(), userId, pageSize, offset).stream()
                            .map(this::toItem)
                            .toList();
            logCompleted(type, page, pageSize, run.getId(), total, startedNanos);
            return new RecommendationFeedView(
                    new RecommendationFeedView.Run(
                            run.getId(),
                            run.getInformationType(),
                            run.getTriggerType(),
                            run.getCompletedAt(),
                            run.getAlgorithmKey(),
                            run.getAlgorithmVersion(),
                            profileChanged),
                    page,
                    pageSize,
                    total,
                    items);
        } catch (DataAccessException exception) {
            throw new RecommendationFeedPersistenceException(
                    "Recommendation Feed data could not be read", exception);
        }
    }

    private RecommendationFeedView.Item toItem(RecommendationFeedQueryRow row) {
        return new RecommendationFeedView.Item(
                row.getRecommendationItemId(),
                row.getInformationId(),
                row.getSnapshotId(),
                row.getRankNo(),
                row.getFinalScore(),
                new RecommendationFeedView.ScoreBreakdown(
                        row.getAiRelevanceScore(),
                        row.getProfileMatchScore(),
                        row.getFreshnessScore()),
                parseReasons(row.getReasonsJson()),
                row.getFeedbackState(),
                row.getJobDisposition(),
                row.getViewCount() != null && row.getViewCount() > 0,
                new RecommendationFeedView.Job(
                        row.getTitle(),
                        row.getCompanyName(),
                        row.getSalaryText(),
                        row.getLocationName(),
                        row.getRemoteType(),
                        row.getSourceUrl()));
    }

    private List<String> parseReasons(String value) {
        try {
            return List.copyOf(objectMapper.readValue(value, REASONS_TYPE));
        } catch (JsonProcessingException | NullPointerException exception) {
            throw new RecommendationFeedPersistenceException(
                    "Recommendation Item reasons could not be decoded", exception);
        }
    }

    private String requireSupportedInformationType(String informationType) {
        if (!JOB.equals(informationType)) {
            throw request(
                    "RECOMMENDATION_INFORMATION_TYPE_UNSUPPORTED",
                    "Only JOB Recommendation is supported");
        }
        return JOB;
    }

    private void requirePagination(int page, int pageSize) {
        if (page < 1) {
            throw request("RECOMMENDATION_FEED_PAGE_INVALID", "page must be at least 1");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw request(
                    "RECOMMENDATION_FEED_PAGE_SIZE_INVALID",
                    "pageSize must be between 1 and 100");
        }
    }

    private void logCompleted(
            String type,
            int page,
            int pageSize,
            Long runId,
            long total,
            long startedNanos) {
        LOGGER.info(
                "Recommendation feed requested, informationType={}, recommendationRunId={}, page={}, pageSize={}, total={}, durationMs={}",
                type,
                runId,
                page,
                pageSize,
                total,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos));
    }

    private RecommendationFeedRequestException request(String code, String message) {
        return new RecommendationFeedRequestException(code, message);
    }
}
