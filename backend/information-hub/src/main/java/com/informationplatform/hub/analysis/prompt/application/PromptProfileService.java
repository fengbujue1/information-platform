package com.informationplatform.hub.analysis.prompt.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptVersionMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptVersionPo;
import com.informationplatform.hub.analysis.prompt.domain.PromptContentHasher;
import com.informationplatform.hub.analysis.prompt.domain.PromptProfile;
import com.informationplatform.hub.analysis.prompt.domain.PromptProfileStatus;
import com.informationplatform.hub.analysis.prompt.domain.PromptVersion;
import com.informationplatform.hub.analysis.prompt.domain.PromptVersionChange;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 实现账号级 Prompt Profile 与不可变 Prompt Version 业务规则。 */
@Service
public class PromptProfileService {

    /** TASK-022 唯一允许的 Analysis Definition Key。 */
    public static final String JOB_USER_RELEVANCE = "JOB_USER_RELEVANCE";

    /** User Prompt 最大 Unicode 字符数。 */
    private static final int MAX_PROMPT_CODE_POINTS = 8_000;

    /** Profile 名称最大 Unicode 字符数，对齐 VARCHAR(255)。 */
    private static final int MAX_PROFILE_NAME_CODE_POINTS = 255;

    /** 从 Session 认证上下文获取可信 Owner。 */
    private final CurrentUserProvider currentUserProvider;

    /** Prompt Profile 持久化 Mapper。 */
    private final AiPromptProfileMapper profileMapper;

    /** 不可变 Prompt Version 持久化 Mapper。 */
    private final AiPromptVersionMapper versionMapper;

    /** Prompt 原文稳定哈希计算器。 */
    private final PromptContentHasher contentHasher;

    public PromptProfileService(
            CurrentUserProvider currentUserProvider,
            AiPromptProfileMapper profileMapper,
            AiPromptVersionMapper versionMapper,
            PromptContentHasher contentHasher) {
        this.currentUserProvider = currentUserProvider;
        this.profileMapper = profileMapper;
        this.versionMapper = versionMapper;
        this.contentHasher = contentHasher;
    }

    /** 返回当前 Owner 的全部 Profile，禁止跨账号枚举。 */
    @Transactional(readOnly = true)
    public List<PromptProfile> listProfiles() {
        long userId = currentUserId();
        return profileMapper.selectList(Wrappers.<AiPromptProfilePo>lambdaQuery()
                        .eq(AiPromptProfilePo::getUserId, userId)
                        .orderByDesc(AiPromptProfilePo::getCreatedAt)
                        .orderByDesc(AiPromptProfilePo::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    /** 创建当前 Owner 的 Profile；名称唯一性最终由数据库约束保护。 */
    @Transactional
    public PromptProfile createProfile(String name, String analysisDefinitionKey) {
        long userId = currentUserId();
        String normalizedName = validateAndNormalizeName(name);
        validateDefinitionKey(analysisDefinitionKey);

        AiPromptProfilePo profile = new AiPromptProfilePo();
        profile.setUserId(userId);
        profile.setName(normalizedName);
        profile.setAnalysisDefinitionKey(JOB_USER_RELEVANCE);
        profile.setStatus(PromptProfileStatus.ACTIVE.name());
        try {
            if (profileMapper.insert(profile) != 1) {
                throw new PromptPersistenceException("Prompt Profile insert affected no row");
            }
        } catch (DuplicateKeyException exception) {
            // 数据库唯一约束处理同一账号并发创建同名 Profile 的竞争。
            throw new PromptConflictException(
                    "PROMPT_PROFILE_NAME_CONFLICT",
                    "A Prompt Profile with the same name already exists",
                    exception);
        }
        return toDomain(requireInsertedProfile(profile.getId(), userId));
    }

    /** 返回当前 Owner 的单个 Profile；越权访问与不存在统一返回 404。 */
    @Transactional(readOnly = true)
    public PromptProfile getProfile(long profileId) {
        return toDomain(requireOwnedProfile(profileId, currentUserId()));
    }

    /**
     * 创建或复用不可变 Version，并在同一事务内切换 Active Version。
     *
     * <p>先锁定 Owner 对应的 Profile 行，使相同 Profile 的版本号分配与 Active 切换串行执行。
     */
    @Transactional
    public PromptVersionChange createVersion(long profileId, String content) {
        long userId = currentUserId();
        validatePromptContent(content);
        AiPromptProfilePo profile = requireOwnedProfileForUpdate(profileId, userId);
        String contentHash = contentHasher.hash(content);

        AiPromptVersionPo existing = versionMapper.selectOne(
                Wrappers.<AiPromptVersionPo>lambdaQuery()
                        .eq(AiPromptVersionPo::getPromptProfileId, profileId)
                        .eq(AiPromptVersionPo::getContentHash, contentHash));
        if (existing != null) {
            // 相同内容复用历史 Version，但仍在本事务内将其切换为 Active。
            updateActiveVersion(profile, existing.getId());
            return new PromptVersionChange(toDomain(existing), false);
        }

        AiPromptVersionPo version = new AiPromptVersionPo();
        version.setPromptProfileId(profileId);
        version.setVersionNo(versionMapper.selectMaxVersionNo(profileId) + 1);
        version.setContent(content);
        version.setContentHash(contentHash);
        if (versionMapper.insert(version) != 1) {
            throw new PromptPersistenceException("Prompt Version insert affected no row");
        }
        // 新 Version 与 Active 指针必须一起提交或一起回滚。
        updateActiveVersion(profile, version.getId());
        return new PromptVersionChange(
                toDomain(requireInsertedVersion(version.getId(), profileId)), true);
    }

    /** 返回当前 Owner 指定 Profile 的全部不可变 Version。 */
    @Transactional(readOnly = true)
    public List<PromptVersion> listVersions(long profileId) {
        long userId = currentUserId();
        requireOwnedProfile(profileId, userId);
        return versionMapper.selectList(Wrappers.<AiPromptVersionPo>lambdaQuery()
                        .eq(AiPromptVersionPo::getPromptProfileId, profileId)
                        .orderByDesc(AiPromptVersionPo::getVersionNo))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    /** 在事务内验证 Owner 与 Profile/Version 归属后切换 Active Version。 */
    @Transactional
    public PromptProfile activateVersion(long profileId, long versionId) {
        long userId = currentUserId();
        AiPromptProfilePo profile = requireOwnedProfileForUpdate(profileId, userId);
        requireVersionInProfile(versionId, profileId);
        updateActiveVersion(profile, versionId);
        return toDomain(requireOwnedProfile(profileId, userId));
    }

    /** 仅以 ACTIVE/DISABLED 停用或重新启用 Profile，不物理删除历史。 */
    @Transactional
    public PromptProfile updateStatus(long profileId, String status) {
        long userId = currentUserId();
        PromptProfileStatus targetStatus = parseStatus(status);
        AiPromptProfilePo profile = requireOwnedProfileForUpdate(profileId, userId);
        if (targetStatus.name().equals(profile.getStatus())) {
            return toDomain(profile);
        }
        // 只更新状态列，避免把查询得到的旧 updated_at 回写并覆盖数据库自动更新时间。
        if (profileMapper.updateStatus(profileId, userId, targetStatus.name()) != 1) {
            throw new PromptPersistenceException("Prompt Profile status update affected no row");
        }
        return toDomain(requireOwnedProfile(profileId, userId));
    }

    private long currentUserId() {
        return currentUserProvider.requireCurrentUser().id();
    }

    private String validateAndNormalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new PromptRequestException(
                    "PROMPT_PROFILE_NAME_REQUIRED", "Prompt Profile name is required");
        }
        if (codePointLength(normalized) > MAX_PROFILE_NAME_CODE_POINTS) {
            throw new PromptRequestException(
                    "PROMPT_PROFILE_NAME_TOO_LONG",
                    "Prompt Profile name must not exceed 255 characters");
        }
        return normalized;
    }

    private void validateDefinitionKey(String analysisDefinitionKey) {
        if (!JOB_USER_RELEVANCE.equals(analysisDefinitionKey)) {
            throw new PromptRequestException(
                    "UNSUPPORTED_ANALYSIS_DEFINITION",
                    "Only JOB_USER_RELEVANCE is supported");
        }
    }

    private void validatePromptContent(String content) {
        if (content == null || content.isBlank()) {
            throw new PromptRequestException(
                    "PROMPT_CONTENT_REQUIRED", "Prompt content is required");
        }
        if (codePointLength(content) > MAX_PROMPT_CODE_POINTS) {
            throw new PromptRequestException(
                    "PROMPT_CONTENT_TOO_LONG",
                    "Prompt content must not exceed 8000 characters");
        }
    }

    private PromptProfileStatus parseStatus(String status) {
        try {
            return PromptProfileStatus.valueOf(status == null ? "" : status);
        } catch (IllegalArgumentException exception) {
            throw new PromptRequestException(
                    "INVALID_PROMPT_PROFILE_STATUS",
                    "Prompt Profile status must be ACTIVE or DISABLED");
        }
    }

    private AiPromptProfilePo requireOwnedProfile(long profileId, long userId) {
        AiPromptProfilePo profile = profileMapper.selectOne(
                Wrappers.<AiPromptProfilePo>lambdaQuery()
                        .eq(AiPromptProfilePo::getId, profileId)
                        .eq(AiPromptProfilePo::getUserId, userId));
        if (profile == null) {
            throw profileNotFound();
        }
        return profile;
    }

    private AiPromptProfilePo requireOwnedProfileForUpdate(long profileId, long userId) {
        AiPromptProfilePo profile = profileMapper.selectOwnedByIdForUpdate(profileId, userId);
        if (profile == null) {
            throw profileNotFound();
        }
        return profile;
    }

    private AiPromptVersionPo requireVersionInProfile(long versionId, long profileId) {
        AiPromptVersionPo version = versionMapper.selectOne(
                Wrappers.<AiPromptVersionPo>lambdaQuery()
                        .eq(AiPromptVersionPo::getId, versionId)
                        .eq(AiPromptVersionPo::getPromptProfileId, profileId));
        if (version == null) {
            throw new PromptNotFoundException(
                    "PROMPT_VERSION_NOT_FOUND", "Prompt Version does not exist");
        }
        return version;
    }

    private AiPromptProfilePo requireInsertedProfile(Long profileId, long userId) {
        if (profileId == null) {
            throw new PromptPersistenceException("Prompt Profile insert did not return an id");
        }
        return requireOwnedProfile(profileId, userId);
    }

    private AiPromptVersionPo requireInsertedVersion(Long versionId, long profileId) {
        if (versionId == null) {
            throw new PromptPersistenceException("Prompt Version insert did not return an id");
        }
        return requireVersionInProfile(versionId, profileId);
    }

    private void updateActiveVersion(AiPromptProfilePo profile, long versionId) {
        if (Objects.equals(versionId, profile.getActiveVersionId())) {
            return;
        }
        int updated = profileMapper.updateActiveVersion(
                profile.getId(), profile.getUserId(), versionId);
        if (updated != 1) {
            throw new PromptPersistenceException("Active Prompt Version update affected no row");
        }
        profile.setActiveVersionId(versionId);
    }

    private PromptNotFoundException profileNotFound() {
        return new PromptNotFoundException(
                "PROMPT_PROFILE_NOT_FOUND", "Prompt Profile does not exist");
    }

    private int codePointLength(String value) {
        return value.codePointCount(0, value.length());
    }

    private PromptProfile toDomain(AiPromptProfilePo profile) {
        return new PromptProfile(
                profile.getId(),
                profile.getName(),
                profile.getAnalysisDefinitionKey(),
                profile.getActiveVersionId(),
                PromptProfileStatus.valueOf(profile.getStatus()),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }

    private PromptVersion toDomain(AiPromptVersionPo version) {
        return new PromptVersion(
                version.getId(),
                version.getPromptProfileId(),
                version.getVersionNo(),
                version.getContent(),
                version.getContentHash(),
                version.getCreatedAt());
    }
}
