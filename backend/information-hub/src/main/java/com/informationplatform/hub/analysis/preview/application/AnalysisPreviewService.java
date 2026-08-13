package com.informationplatform.hub.analysis.preview.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.informationplatform.hub.analysis.candidate.application.CandidateResolverRegistry;
import com.informationplatform.hub.analysis.candidate.domain.AnalysisCandidate;
import com.informationplatform.hub.analysis.candidate.domain.CandidateResolution;
import com.informationplatform.hub.analysis.candidate.domain.CandidateResolutionRequest;
import com.informationplatform.hub.analysis.definition.application.AnalysisDefinitionRegistry;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinition;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptVersionMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptVersionPo;
import com.informationplatform.hub.analysis.processing.application.AnalysisPersistenceException;
import com.informationplatform.hub.analysis.processing.application.AnalysisPromptAssembler;
import com.informationplatform.hub.analysis.processing.application.AnalysisTokenEstimator;
import com.informationplatform.hub.analysis.processing.domain.AnalysisTokenEstimate;
import com.informationplatform.hub.analysis.processing.domain.AssembledAnalysisPrompt;
import com.informationplatform.hub.analysis.preview.domain.AnalysisPreview;
import com.informationplatform.hub.analysis.preview.domain.AnalysisPreviewLimits;
import com.informationplatform.hub.analysis.preview.domain.PreviewTokenPayload;
import com.informationplatform.hub.analysis.preview.domain.ResolvedAnalysisPreview;
import com.informationplatform.hub.analysis.preview.domain.ResolvedPreviewCandidate;
import com.informationplatform.hub.analysis.prompt.domain.PromptVersion;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

/** 编排无 Provider、无持久化副作用的 Candidate Preview。 */
@Service
public class AnalysisPreviewService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(10);

    /** 从 Session 获取可信 Owner。 */
    private final CurrentUserProvider currentUserProvider;
    /** Owner Profile 只读解析。 */
    private final AiPromptProfileMapper profileMapper;
    /** Active Prompt Version 解析。 */
    private final AiPromptVersionMapper versionMapper;
    /** 当前 Definition Registry。 */
    private final AnalysisDefinitionRegistry definitionRegistry;
    /** Information 类型到 CandidateResolver 的注册表。 */
    private final CandidateResolverRegistry candidateResolverRegistry;
    /** 与真实调用一致的安全 Prompt Assembly。 */
    private final AnalysisPromptAssembler promptAssembler;
    /** TASK-027 已实现的冻结 Estimate V1。 */
    private final AnalysisTokenEstimator tokenEstimator;
    /** 有序候选与预算决策指纹。 */
    private final CandidateFingerprintCalculator fingerprintCalculator;
    /** 10 分钟 HMAC Token 签发器。 */
    private final PreviewTokenService tokenService;
    /** 手动、确认和定时入口共享的动态限制策略。 */
    private final AnalysisPreviewLimitPolicy limitPolicy;
    /** 固定绝对窗口与到期时间的 UTC Clock。 */
    private final Clock clock;

    public AnalysisPreviewService(
            CurrentUserProvider currentUserProvider,
            AiPromptProfileMapper profileMapper,
            AiPromptVersionMapper versionMapper,
            AnalysisDefinitionRegistry definitionRegistry,
            CandidateResolverRegistry candidateResolverRegistry,
            AnalysisPromptAssembler promptAssembler,
            AnalysisTokenEstimator tokenEstimator,
            CandidateFingerprintCalculator fingerprintCalculator,
            PreviewTokenService tokenService,
            AnalysisPreviewLimitPolicy limitPolicy,
            Clock clock) {
        this.currentUserProvider = currentUserProvider;
        this.profileMapper = profileMapper;
        this.versionMapper = versionMapper;
        this.definitionRegistry = definitionRegistry;
        this.candidateResolverRegistry = candidateResolverRegistry;
        this.promptAssembler = promptAssembler;
        this.tokenEstimator = tokenEstimator;
        this.fingerprintCalculator = fingerprintCalculator;
        this.tokenService = tokenService;
        this.limitPolicy = limitPolicy;
        this.clock = clock;
    }

    /**
     * 在一个只读事务中冻结 Profile/Version/Definition 和候选视图。
     *
     * <p>本方法不调用 Provider，不创建 Analysis、Invocation、Batch 或 Preview 记录。
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AnalysisPreview preview(
            long promptProfileId,
            Integer windowDays,
            Integer maxCandidates,
            Long maxEstimatedTokens) {
        if (promptProfileId <= 0) {
            throw request("PREVIEW_PROFILE_ID_INVALID", "promptProfileId must be positive");
        }
        AnalysisPreviewLimits limits =
                limitPolicy.resolve(windowDays, maxCandidates, maxEstimatedTokens);
        // 只读取一次时钟并截断到 MySQL DATETIME(3) 精度，冻结绝对半开窗口。
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        Instant windowEnd = issuedAt;
        Instant windowStart = windowEnd.minus(limits.windowDays(), ChronoUnit.DAYS);
        long userId = currentUserProvider.requireCurrentUser().id();
        ResolvedAnalysisPreview resolved = resolve(
                userId,
                promptProfileId,
                null,
                null,
                null,
                windowStart,
                windowEnd,
                limits);
        Instant expiresAt = issuedAt.plus(TOKEN_TTL);
        PreviewTokenPayload payload = new PreviewTokenPayload(
                1,
                userId,
                resolved.promptProfileId(),
                resolved.promptVersionId(),
                resolved.definitionKey(),
                resolved.definitionVersion(),
                windowStart,
                windowEnd,
                limits.windowDays(),
                limits.maxCandidates(),
                limits.maxEstimatedTokens(),
                resolved.totalInWindow(),
                resolved.eligibleCount(),
                resolved.alreadyAnalyzedCount(),
                resolved.selectedCount(),
                resolved.deferredByItemLimitCount(),
                resolved.deferredByTokenBudgetCount(),
                resolved.estimatedInputTokens(),
                resolved.estimatedOutputTokens(),
                resolved.estimatedTotalTokens(),
                resolved.estimateMethod(),
                resolved.candidateFingerprint(),
                UUID.randomUUID().toString(),
                issuedAt,
                expiresAt);
        String token = tokenService.issue(payload);
        return new AnalysisPreview(
                windowStart,
                windowEnd,
                resolved.totalInWindow(),
                resolved.eligibleCount(),
                resolved.eligibleCount(),
                resolved.alreadyAnalyzedCount(),
                resolved.selectedCount(),
                resolved.deferredByItemLimitCount(),
                resolved.deferredByTokenBudgetCount(),
                resolved.estimatedInputTokens(),
                resolved.estimatedOutputTokens(),
                resolved.estimatedTotalTokens(),
                resolved.estimateMethod(),
                expiresAt,
                token);
    }

    /**
     * 使用 Token 冻结的绝对窗口和版本重新解析候选，供 Confirm 在同一事务内校验漂移。
     */
    public ResolvedAnalysisPreview recompute(PreviewTokenPayload payload) {
        long userId = currentUserProvider.requireCurrentUser().id();
        if (payload.userId() != userId) {
            throw new AnalysisPreviewConflictException(
                    "PREVIEW_TOKEN_OWNER_MISMATCH",
                    "Preview Token does not belong to the current user");
        }
        AnalysisPreviewLimits limits;
        try {
            // 部署时降低平台上限后，旧 Token 必须作为预览漂移拒绝，不能创建新批次。
            limits = limitPolicy.resolve(
                    payload.windowDays(),
                    payload.maxCandidates(),
                    payload.maxEstimatedTokens());
        } catch (AnalysisPreviewRequestException exception) {
            throw new AnalysisPreviewConflictException(
                    "ANALYSIS_PREVIEW_DRIFTED",
                    "Preview limits no longer satisfy the current platform policy");
        }
        return resolve(
                userId,
                payload.promptProfileId(),
                payload.promptVersionId(),
                payload.definitionKey(),
                payload.definitionVersion(),
                payload.windowStart(),
                payload.windowEnd(),
                limits);
    }

    /**
     * 按 Schedule 冻结的 Owner、计划点和限制解析候选。
     *
     * <p>该内部入口不依赖浏览器 Session、不签发 Preview Token，也不调用 Provider。
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ResolvedAnalysisPreview resolveScheduled(
            long userId,
            long promptProfileId,
            Instant windowEnd,
            AnalysisPreviewLimits limits) {
        if (userId <= 0 || promptProfileId <= 0) {
            throw request(
                    "SCHEDULE_CONTEXT_INVALID",
                    "Schedule owner and Prompt Profile must be positive");
        }
        Instant normalizedEnd = windowEnd.truncatedTo(ChronoUnit.MILLIS);
        Instant windowStart =
                normalizedEnd.minus(limits.windowDays(), ChronoUnit.DAYS);
        return resolve(
                userId,
                promptProfileId,
                null,
                null,
                null,
                windowStart,
                normalizedEnd,
                limits);
    }

    /** 在调用方事务快照内解析 Profile、版本、候选、Estimate 和预算决策。 */
    private ResolvedAnalysisPreview resolve(
            long userId,
            long promptProfileId,
            Long expectedVersionId,
            String expectedDefinitionKey,
            Integer expectedDefinitionVersion,
            Instant windowStart,
            Instant windowEnd,
            AnalysisPreviewLimits limits) {
        AiPromptProfilePo profile = requireProfile(promptProfileId, userId);
        AiPromptVersionPo version = requireActiveVersion(profile);
        AnalysisDefinition<?, ?> definition = requireDefinition(profile);
        if ((expectedVersionId != null && !expectedVersionId.equals(version.getId()))
                || (expectedDefinitionKey != null
                        && !expectedDefinitionKey.equals(definition.id().key()))
                || (expectedDefinitionVersion != null
                        && expectedDefinitionVersion != definition.id().version())) {
            throw new AnalysisPreviewConflictException(
                    "PREVIEW_CONTEXT_DRIFTED",
                    "Prompt or Analysis Definition changed after Preview");
        }
        PromptVersion promptVersion = toPromptVersion(version);
        CandidateResolution resolution = candidateResolverRegistry
                .require(definition.informationType())
                .resolve(new CandidateResolutionRequest(
                        userId,
                        version.getId(),
                        definition.id().key(),
                        definition.id().version(),
                        LocalDateTime.ofInstant(windowStart, ZoneOffset.UTC),
                        LocalDateTime.ofInstant(windowEnd, ZoneOffset.UTC),
                        limits.maxCandidates()));
        BudgetResult budget = applyBudget(
                definition, promptVersion, resolution.candidates(), limits.maxEstimatedTokens());
        long deferredByItemLimit =
                resolution.eligibleCount() - resolution.candidates().size();
        String fingerprint = fingerprintCalculator.calculate(
                budget.candidates().stream()
                        .map(ResolvedPreviewCandidate::fingerprintValue)
                        .toList());
        return new ResolvedAnalysisPreview(
                userId,
                profile.getId(),
                version.getId(),
                definition.informationType().name(),
                definition.id().key(),
                definition.id().version(),
                windowStart,
                windowEnd,
                limits.windowDays(),
                limits.maxCandidates(),
                limits.maxEstimatedTokens(),
                resolution.totalInWindow(),
                resolution.eligibleCount(),
                resolution.alreadyAnalyzedCount(),
                budget.selectedCount(),
                deferredByItemLimit,
                budget.deferredByTokenBudgetCount(),
                budget.estimatedInputTokens(),
                budget.estimatedOutputTokens(),
                budget.estimatedTotalTokens(),
                AnalysisTokenEstimator.METHOD,
                fingerprint,
                budget.candidates());
    }

    /** 按稳定候选顺序选择预算前缀，防止跳过较早候选后挑选较晚候选。 */
    private BudgetResult applyBudget(
            AnalysisDefinition<?, ?> definition,
            PromptVersion promptVersion,
            List<AnalysisCandidate> candidates,
            long maxEstimatedTokens) {
        List<ResolvedPreviewCandidate> decisions = new ArrayList<>();
        long input = 0;
        long output = 0;
        long total = 0;
        long selected = 0;
        boolean budgetExhausted = false;
        for (AnalysisCandidate candidate : candidates) {
            AssembledAnalysisPrompt assembled = assemble(definition, promptVersion, candidate);
            AnalysisTokenEstimate estimate = tokenEstimator.estimate(assembled.providerRequest());
            boolean withinBudget = !budgetExhausted
                    && estimate.totalTokens() <= maxEstimatedTokens - total;
            if (withinBudget) {
                input = Math.addExact(input, estimate.inputTokens());
                output = Math.addExact(output, estimate.outputTokens());
                total = Math.addExact(total, estimate.totalTokens());
                selected++;
            } else {
                budgetExhausted = true;
            }
            decisions.add(new ResolvedPreviewCandidate(
                    candidate, estimate, withinBudget, assembled.providerRequest()));
        }
        return new BudgetResult(
                selected, candidates.size() - selected, input, output, total, decisions);
    }

    @SuppressWarnings("unchecked")
    private <I> AssembledAnalysisPrompt assemble(
            AnalysisDefinition<?, ?> definition,
            PromptVersion promptVersion,
            AnalysisCandidate candidate) {
        return promptAssembler.assemble(
                (AnalysisDefinition<I, ?>) definition,
                promptVersion,
                candidate.snapshotSource());
    }

    private AiPromptProfilePo requireProfile(long profileId, long userId) {
        AiPromptProfilePo profile = profileMapper.selectOwnedById(profileId, userId);
        if (profile == null) {
            throw new AnalysisPreviewNotFoundException(
                    "PREVIEW_PROMPT_PROFILE_NOT_FOUND", "Prompt Profile does not exist");
        }
        if (!"ACTIVE".equals(profile.getStatus())) {
            throw request("PREVIEW_PROMPT_PROFILE_DISABLED", "Prompt Profile is disabled");
        }
        if (profile.getActiveVersionId() == null) {
            throw request(
                    "PREVIEW_ACTIVE_VERSION_REQUIRED", "Prompt Profile has no active version");
        }
        return profile;
    }

    private AiPromptVersionPo requireActiveVersion(AiPromptProfilePo profile) {
        AiPromptVersionPo version = versionMapper.selectOne(
                Wrappers.<AiPromptVersionPo>lambdaQuery()
                        .eq(AiPromptVersionPo::getId, profile.getActiveVersionId())
                        .eq(AiPromptVersionPo::getPromptProfileId, profile.getId()));
        if (version == null) {
            throw new AnalysisPersistenceException(
                    "Active Prompt Version does not belong to its Profile");
        }
        return version;
    }

    private AnalysisDefinition<?, ?> requireDefinition(AiPromptProfilePo profile) {
        try {
            return definitionRegistry.requireCurrent(profile.getAnalysisDefinitionKey());
        } catch (IllegalArgumentException exception) {
            throw request(
                    "PREVIEW_DEFINITION_UNAVAILABLE",
                    "Prompt Profile Analysis Definition is unavailable");
        }
    }

    private PromptVersion toPromptVersion(AiPromptVersionPo version) {
        return new PromptVersion(
                version.getId(),
                version.getPromptProfileId(),
                version.getVersionNo(),
                version.getContent(),
                version.getContentHash(),
                version.getCreatedAt());
    }

    private AnalysisPreviewRequestException request(String code, String message) {
        return new AnalysisPreviewRequestException(code, message);
    }

    /** 单次 Preview 的预算聚合与有序指纹输入。 */
    private record BudgetResult(
            /** 预算内数量。 */ long selectedCount,
            /** Token Budget 延后数量。 */ long deferredByTokenBudgetCount,
            /** 预算内 Estimated 输入 Token。 */ long estimatedInputTokens,
            /** 预算内 Estimated 输出 Token。 */ long estimatedOutputTokens,
            /** 预算内 Estimated 总 Token。 */ long estimatedTotalTokens,
            /** Candidate Limit 内全部有序决策。 */ List<ResolvedPreviewCandidate> candidates) {

        private BudgetResult {
            candidates = List.copyOf(candidates);
        }
    }
}
