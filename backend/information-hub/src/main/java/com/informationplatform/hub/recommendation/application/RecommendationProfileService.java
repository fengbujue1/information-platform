package com.informationplatform.hub.recommendation.application;

import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.recommendation.domain.RecommendationProfileCore;
import com.informationplatform.hub.recommendation.infrastructure.persistence.mapper.UserRecommendationProfileMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.UserRecommendationProfilePo;
import com.informationplatform.hub.recommendation.job.domain.JobRecommendationProfile;
import com.informationplatform.hub.recommendation.job.domain.JobRecommendationProfilePreferences;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.JobRecommendationProfileJsonCodec;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.mapper.JobRecommendationProfileMapper;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.po.JobRecommendationProfilePo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 实现当前 Owner 的 Generic Core + JOB Extension Recommendation Profile 业务。 */
@Service
public class RecommendationProfileService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RecommendationProfileService.class);

    /** Phase 4 唯一实际支持的 Information Type。 */
    public static final String JOB = "JOB";

    /** JOB Recommendation 必须绑定的 Analysis Definition。 */
    private static final String JOB_USER_RELEVANCE = "JOB_USER_RELEVANCE";

    /** 从 Session 认证上下文获取可信 Owner。 */
    private final CurrentUserProvider currentUserProvider;

    /** Recommendation Profile Core Mapper。 */
    private final UserRecommendationProfileMapper profileMapper;

    /** JOB Recommendation Profile Extension Mapper。 */
    private final JobRecommendationProfileMapper jobProfileMapper;

    /** AI Prompt Profile Mapper，用于同 Owner 与可用性校验。 */
    private final AiPromptProfileMapper promptProfileMapper;

    /** JOB 偏好规范化器。 */
    private final JobRecommendationProfileNormalizer normalizer;

    /** 完整组合 Profile hash 计算器。 */
    private final JobRecommendationProfileHasher hasher;

    /** JOB Extension JSON 编解码器。 */
    private final JobRecommendationProfileJsonCodec jsonCodec;

    public RecommendationProfileService(
            CurrentUserProvider currentUserProvider,
            UserRecommendationProfileMapper profileMapper,
            JobRecommendationProfileMapper jobProfileMapper,
            AiPromptProfileMapper promptProfileMapper,
            JobRecommendationProfileNormalizer normalizer,
            JobRecommendationProfileHasher hasher,
            JobRecommendationProfileJsonCodec jsonCodec) {
        this.currentUserProvider = currentUserProvider;
        this.profileMapper = profileMapper;
        this.jobProfileMapper = jobProfileMapper;
        this.promptProfileMapper = promptProfileMapper;
        this.normalizer = normalizer;
        this.hasher = hasher;
        this.jsonCodec = jsonCodec;
    }

    /** 返回当前 Owner 在指定 Information Type 下的完整组合 Profile。 */
    @Transactional(readOnly = true)
    public JobRecommendationProfile getProfile(String informationType) {
        String type = requireSupportedInformationType(informationType);
        long userId = currentUserProvider.requireCurrentUser().id();
        UserRecommendationProfilePo core = profileMapper.selectOwnedByType(userId, type);
        if (core == null) {
            throw profileNotFound();
        }
        return combine(core, requireJobExtension(core.getId()));
    }

    /**
     * 创建或完整替换当前 Owner 的 JOB Profile。
     *
     * <p>Core 与 JOB Extension 在同一事务提交；本方法不创建 Analysis、Recommendation Run 或刷新事件。
     */
    @Transactional
    public RecommendationProfileChange saveProfile(
            String informationType, SaveRecommendationProfileCommand command) {
        String type = requireSupportedInformationType(informationType);
        validateCore(command);
        long userId = currentUserProvider.requireCurrentUser().id();
        LOGGER.info(
                "Recommendation profile update requested, informationType={}, promptProfileId={}",
                type,
                command.analysisPromptProfileId());

        requireUsableOwnedPromptProfile(command.analysisPromptProfileId(), userId);
        JobRecommendationProfilePreferences preferences = normalizer.normalize(command);
        String contentHash = hasher.hash(
                type,
                command.analysisPromptProfileId(),
                command.windowDays(),
                command.topN(),
                preferences);

        // 唯一键范围行锁保证同一 Owner + Information Type 的完整替换串行执行。
        UserRecommendationProfilePo core =
                profileMapper.selectOwnedByTypeForUpdate(userId, type);
        boolean created = core == null;
        if (created) {
            core = insertCore(userId, type, command, contentHash);
            insertJobExtension(core.getId(), preferences);
        } else {
            updateCore(core.getId(), userId, type, command, contentHash);
            updateJobExtension(core.getId(), preferences);
        }

        JobRecommendationProfile saved = getOwnedProfile(userId, type);
        LOGGER.info(
                "Recommendation profile updated, recommendationProfileId={}, informationType={}, created={}, contentHash={}",
                saved.core().id(),
                type,
                created,
                saved.core().contentHash());
        return new RecommendationProfileChange(saved, created);
    }

    private void validateCore(SaveRecommendationProfileCommand command) {
        if (command == null) {
            throw request(
                    "RECOMMENDATION_PROFILE_BODY_REQUIRED",
                    "Recommendation Profile request body is required");
        }
        if (command.analysisPromptProfileId() <= 0) {
            throw request(
                    "RECOMMENDATION_PROFILE_PROMPT_ID_INVALID",
                    "analysisPromptProfileId must be positive");
        }
        if (command.windowDays() < 1 || command.windowDays() > 30) {
            throw request(
                    "RECOMMENDATION_PROFILE_WINDOW_DAYS_INVALID",
                    "windowDays must be between 1 and 30");
        }
        if (command.topN() < 1 || command.topN() > 100) {
            throw request(
                    "RECOMMENDATION_PROFILE_TOP_N_INVALID",
                    "topN must be between 1 and 100");
        }
    }

    private String requireSupportedInformationType(String informationType) {
        if (!JOB.equals(informationType)) {
            throw request(
                    "RECOMMENDATION_INFORMATION_TYPE_UNSUPPORTED",
                    "Only JOB Recommendation Profile is supported");
        }
        return JOB;
    }

    private void requireUsableOwnedPromptProfile(long promptProfileId, long userId) {
        AiPromptProfilePo promptProfile =
                promptProfileMapper.selectOwnedById(promptProfileId, userId);
        if (promptProfile == null) {
            // 不区分不存在与跨 Owner，避免泄露其它账号资源。
            throw new RecommendationProfileNotFoundException(
                    "RECOMMENDATION_PROMPT_PROFILE_NOT_FOUND",
                    "Prompt Profile does not exist");
        }
        if (!JOB_USER_RELEVANCE.equals(promptProfile.getAnalysisDefinitionKey())) {
            throw request(
                    "RECOMMENDATION_PROMPT_PROFILE_INCOMPATIBLE",
                    "Prompt Profile must use JOB_USER_RELEVANCE");
        }
        if (!"ACTIVE".equals(promptProfile.getStatus())) {
            throw request(
                    "RECOMMENDATION_PROMPT_PROFILE_DISABLED",
                    "Prompt Profile must be active");
        }
        if (promptProfile.getActiveVersionId() == null) {
            throw request(
                    "RECOMMENDATION_PROMPT_PROFILE_VERSION_REQUIRED",
                    "Prompt Profile must have an active version");
        }
    }

    private UserRecommendationProfilePo insertCore(
            long userId,
            String informationType,
            SaveRecommendationProfileCommand command,
            String contentHash) {
        UserRecommendationProfilePo core = new UserRecommendationProfilePo();
        core.setUserId(userId);
        core.setInformationType(informationType);
        core.setAnalysisPromptProfileId(command.analysisPromptProfileId());
        core.setWindowDays(command.windowDays());
        core.setTopN(command.topN());
        core.setContentHash(contentHash);
        try {
            if (profileMapper.insert(core) != 1 || core.getId() == null) {
                throw persistence("Recommendation Profile Core insert affected no row");
            }
        } catch (DuplicateKeyException exception) {
            throw new RecommendationProfileConflictException(
                    "RECOMMENDATION_PROFILE_CONCURRENT_UPDATE",
                    "Recommendation Profile was updated concurrently",
                    exception);
        }
        return core;
    }

    private void updateCore(
            long profileId,
            long userId,
            String informationType,
            SaveRecommendationProfileCommand command,
            String contentHash) {
        if (profileMapper.updateOwnedProfile(
                        profileId,
                        userId,
                        informationType,
                        command.analysisPromptProfileId(),
                        command.windowDays(),
                        command.topN(),
                        contentHash)
                != 1) {
            throw persistence("Recommendation Profile Core update affected no row");
        }
    }

    private void insertJobExtension(
            long profileId, JobRecommendationProfilePreferences preferences) {
        JobRecommendationProfilePo extension = toPo(profileId, preferences);
        if (jobProfileMapper.insert(extension) != 1) {
            throw persistence("JOB Recommendation Profile insert affected no row");
        }
    }

    private void updateJobExtension(
            long profileId, JobRecommendationProfilePreferences preferences) {
        JobRecommendationProfilePo extension = toPo(profileId, preferences);
        if (jobProfileMapper.updateFullProfile(
                        profileId,
                        extension.getTargetRoles(),
                        extension.getPreferredSkills(),
                        extension.getPreferredCities(),
                        extension.getPreferredRemoteTypes(),
                        extension.getSalaryMinMonthlyYuan(),
                        extension.getExcludedKeywords())
                != 1) {
            throw persistence("JOB Recommendation Profile update affected no row");
        }
    }

    private JobRecommendationProfile getOwnedProfile(long userId, String informationType) {
        UserRecommendationProfilePo core =
                profileMapper.selectOwnedByType(userId, informationType);
        if (core == null) {
            throw persistence("Saved Recommendation Profile Core could not be read");
        }
        return combine(core, requireJobExtension(core.getId()));
    }

    private JobRecommendationProfilePo requireJobExtension(long profileId) {
        JobRecommendationProfilePo extension = jobProfileMapper.selectById(profileId);
        if (extension == null) {
            throw persistence("JOB Recommendation Profile extension does not exist");
        }
        return extension;
    }

    private JobRecommendationProfilePo toPo(
            long profileId, JobRecommendationProfilePreferences preferences) {
        JobRecommendationProfilePo po = new JobRecommendationProfilePo();
        po.setProfileId(profileId);
        po.setTargetRoles(jsonCodec.encode(preferences.targetRoles()));
        po.setPreferredSkills(jsonCodec.encode(preferences.preferredSkills()));
        po.setPreferredCities(jsonCodec.encode(preferences.preferredCities()));
        po.setPreferredRemoteTypes(jsonCodec.encode(preferences.preferredRemoteTypes()));
        po.setSalaryMinMonthlyYuan(preferences.salaryMinMonthlyYuan());
        po.setExcludedKeywords(jsonCodec.encode(preferences.excludedKeywords()));
        return po;
    }

    private JobRecommendationProfile combine(
            UserRecommendationProfilePo core, JobRecommendationProfilePo extension) {
        RecommendationProfileCore coreDomain = new RecommendationProfileCore(
                core.getId(),
                core.getInformationType(),
                core.getAnalysisPromptProfileId(),
                core.getWindowDays(),
                core.getTopN(),
                core.getContentHash(),
                core.getCreatedAt(),
                core.getUpdatedAt());
        JobRecommendationProfilePreferences preferences =
                new JobRecommendationProfilePreferences(
                        jsonCodec.decode(extension.getTargetRoles()),
                        jsonCodec.decode(extension.getPreferredSkills()),
                        jsonCodec.decode(extension.getPreferredCities()),
                        jsonCodec.decode(extension.getPreferredRemoteTypes()),
                        extension.getSalaryMinMonthlyYuan(),
                        jsonCodec.decode(extension.getExcludedKeywords()));
        return new JobRecommendationProfile(coreDomain, preferences);
    }

    private RecommendationProfileNotFoundException profileNotFound() {
        return new RecommendationProfileNotFoundException(
                "RECOMMENDATION_PROFILE_NOT_FOUND",
                "Recommendation Profile does not exist");
    }

    private RecommendationProfileRequestException request(String code, String message) {
        return new RecommendationProfileRequestException(code, message);
    }

    private RecommendationProfilePersistenceException persistence(String message) {
        return new RecommendationProfilePersistenceException(message);
    }
}
