package com.informationplatform.hub.recommendation.application;

import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.information.infrastructure.persistence.mapper.InformationItemMapper;
import com.informationplatform.hub.information.infrastructure.persistence.po.InformationItemPo;
import com.informationplatform.hub.recommendation.domain.FeedbackState;
import com.informationplatform.hub.recommendation.domain.UserInformationInteraction;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.RecommendationItemMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.UserInformationInteractionMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.UserInformationInteractionPo;
import com.informationplatform.hub.recommendation.job.domain.JobDisposition;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.mapper.UserJobDispositionMapper;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.po.UserJobDispositionPo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 实现 current user × Information 的通用交互与 JOB disposition 写入。 */
@Service
public class RecommendationInteractionService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RecommendationInteractionService.class);

    /** Phase 4 唯一实际支持 Job disposition 的 Information Type。 */
    private static final String JOB = "JOB";

    /** 从 Session 认证上下文获取可信 Owner。 */
    private final CurrentUserProvider currentUserProvider;

    /** Information Mapper，用于目标存在性和领域类型校验。 */
    private final InformationItemMapper informationItemMapper;

    /** 通用 Interaction Core Mapper。 */
    private final UserInformationInteractionMapper interactionMapper;

    /** JOB disposition 扩展 Mapper。 */
    private final UserJobDispositionMapper jobDispositionMapper;

    /** Recommendation Item Mapper，用于可选归因的 Owner 校验。 */
    private final RecommendationItemMapper recommendationItemMapper;

    /** 统一产生 UTC 业务时间。 */
    private final Clock clock;

    public RecommendationInteractionService(
            CurrentUserProvider currentUserProvider,
            InformationItemMapper informationItemMapper,
            UserInformationInteractionMapper interactionMapper,
            UserJobDispositionMapper jobDispositionMapper,
            RecommendationItemMapper recommendationItemMapper,
            Clock clock) {
        this.currentUserProvider = currentUserProvider;
        this.informationItemMapper = informationItemMapper;
        this.interactionMapper = interactionMapper;
        this.jobDispositionMapper = jobDispositionMapper;
        this.recommendationItemMapper = recommendationItemMapper;
        this.clock = clock;
    }

    /** 原子记录一次查看并返回更新后的 current aggregate。 */
    @Transactional
    public UserInformationInteraction recordView(
            long informationId, Long recommendationItemId) {
        long userId = currentUserProvider.requireCurrentUser().id();
        requireInformation(informationId);
        requireOwnedAttribution(recommendationItemId, informationId, userId);
        LocalDateTime occurredAt = utcNow();

        // 单条 upsert 同时解决重复查看和并发首次创建，避免 read-then-insert 竞争窗口。
        if (interactionMapper.recordView(
                        userId, informationId, recommendationItemId, occurredAt)
                < 1) {
            throw persistence("Interaction view upsert affected no row");
        }
        LOGGER.info("Recommendation interaction viewed, informationId={}", informationId);
        return loadCurrentState(userId, informationId);
    }

    /** 完整覆盖通用 Feedback；NONE 用于显式取消通用 hard exclusion。 */
    @Transactional
    public UserInformationInteraction replaceFeedback(
            long informationId, String feedbackState, Long recommendationItemId) {
        FeedbackState state = requireFeedbackState(feedbackState);
        long userId = currentUserProvider.requireCurrentUser().id();
        requireInformation(informationId);
        requireOwnedAttribution(recommendationItemId, informationId, userId);
        LocalDateTime occurredAt = utcNow();

        // Feedback upsert 不接触 view 字段或 JOB 扩展，保持两个状态维度独立。
        if (interactionMapper.replaceFeedback(
                        userId,
                        informationId,
                        state.name(),
                        recommendationItemId,
                        occurredAt)
                < 1) {
            throw persistence("Interaction feedback upsert affected no row");
        }
        LOGGER.info(
                "Recommendation feedback updated, informationId={}, feedbackState={}",
                informationId,
                state);
        return loadCurrentState(userId, informationId);
    }

    /** 完整覆盖 JOB disposition；NONE 用于显式取消 JOB hard exclusion。 */
    @Transactional
    public UserInformationInteraction replaceJobDisposition(
            long informationId, String jobDisposition, Long recommendationItemId) {
        JobDisposition disposition = requireJobDisposition(jobDisposition);
        long userId = currentUserProvider.requireCurrentUser().id();
        InformationItemPo information = requireInformation(informationId);
        if (!JOB.equals(information.getInformationType())) {
            throw request(
                    "RECOMMENDATION_JOB_DISPOSITION_INFORMATION_TYPE_INVALID",
                    "Job disposition is only supported for JOB Information");
        }
        requireOwnedAttribution(recommendationItemId, informationId, userId);

        // Core 先通过唯一键 upsert，随后 JOB 扩展与归因在同一事务原子提交。
        interactionMapper.ensureInteraction(userId, informationId, recommendationItemId);
        UserInformationInteractionPo interaction =
                interactionMapper.selectOwnedInteraction(userId, informationId);
        if (interaction == null || interaction.getId() == null) {
            throw persistence("Interaction Core could not be read after upsert");
        }
        LocalDateTime occurredAt = utcNow();
        if (jobDispositionMapper.replaceDisposition(
                        interaction.getId(), disposition.name(), occurredAt)
                < 1) {
            throw persistence("JOB disposition upsert affected no row");
        }
        LOGGER.info(
                "Job disposition updated, informationId={}, jobDisposition={}",
                informationId,
                disposition);
        return loadCurrentState(userId, informationId);
    }

    private InformationItemPo requireInformation(long informationId) {
        if (informationId <= 0) {
            throw request(
                    "RECOMMENDATION_INFORMATION_ID_INVALID",
                    "informationId must be positive");
        }
        InformationItemPo information = informationItemMapper.selectById(informationId);
        if (information == null) {
            throw new RecommendationInteractionNotFoundException(
                    "RECOMMENDATION_INFORMATION_NOT_FOUND",
                    "Information does not exist");
        }
        return information;
    }

    private void requireOwnedAttribution(
            Long recommendationItemId, long informationId, long userId) {
        if (recommendationItemId == null) {
            return;
        }
        if (recommendationItemId <= 0
                || recommendationItemMapper.countOwnedAttribution(
                                recommendationItemId, informationId, userId)
                        != 1) {
            // 不区分跨 Owner、Information 不匹配与不存在，避免泄露其它用户历史 Item。
            throw request(
                    "RECOMMENDATION_INTERACTION_ATTRIBUTION_INVALID",
                    "recommendationItemId is not a valid attribution");
        }
    }

    private FeedbackState requireFeedbackState(String value) {
        try {
            return FeedbackState.fromValue(value);
        } catch (IllegalArgumentException exception) {
            throw request(
                    "RECOMMENDATION_FEEDBACK_STATE_INVALID",
                    "feedbackState must be NONE, INTERESTED or NOT_INTERESTED");
        }
    }

    private JobDisposition requireJobDisposition(String value) {
        try {
            return JobDisposition.fromValue(value);
        } catch (IllegalArgumentException exception) {
            throw request(
                    "RECOMMENDATION_JOB_DISPOSITION_INVALID",
                    "jobDisposition must be NONE, CONTACTED or CONTACTED_NOT_SUITABLE");
        }
    }

    private UserInformationInteraction loadCurrentState(long userId, long informationId) {
        UserInformationInteractionPo interaction =
                interactionMapper.selectOwnedInteraction(userId, informationId);
        if (interaction == null || interaction.getId() == null) {
            throw persistence("Interaction Core does not exist after write");
        }
        UserJobDispositionPo extension = jobDispositionMapper.selectById(interaction.getId());
        JobDisposition disposition = extension == null
                ? JobDisposition.NONE
                : requirePersistedJobDisposition(extension.getJobDisposition());
        return new UserInformationInteraction(
                interaction.getInformationId(),
                interaction.getViewCount(),
                interaction.getLastViewedAt(),
                requirePersistedFeedbackState(interaction.getFeedbackState()),
                interaction.getFeedbackUpdatedAt(),
                disposition,
                extension == null ? null : extension.getDispositionUpdatedAt(),
                interaction.getLastRecommendationItemId(),
                interaction.getUpdatedAt());
    }

    private FeedbackState requirePersistedFeedbackState(String value) {
        try {
            return FeedbackState.fromValue(value);
        } catch (IllegalArgumentException exception) {
            throw persistence("Interaction Core contains an unsupported feedback state");
        }
    }

    private JobDisposition requirePersistedJobDisposition(String value) {
        try {
            return JobDisposition.fromValue(value);
        } catch (IllegalArgumentException exception) {
            throw persistence("JOB disposition extension contains an unsupported state");
        }
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private RecommendationInteractionRequestException request(String code, String message) {
        return new RecommendationInteractionRequestException(code, message);
    }

    private RecommendationInteractionPersistenceException persistence(String message) {
        return new RecommendationInteractionPersistenceException(message);
    }
}
