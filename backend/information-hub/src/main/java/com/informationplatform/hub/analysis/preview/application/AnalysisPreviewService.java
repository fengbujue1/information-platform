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
import com.informationplatform.hub.analysis.preview.domain.PreviewCandidateEstimate;
import com.informationplatform.hub.analysis.preview.domain.PreviewTokenPayload;
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
                AnalysisPreviewLimits.resolve(windowDays, maxCandidates, maxEstimatedTokens);
        long userId = currentUserProvider.requireCurrentUser().id();
        AiPromptProfilePo profile = requireProfile(promptProfileId, userId);
        AiPromptVersionPo version = requireActiveVersion(profile);
        AnalysisDefinition<?, ?> definition = requireDefinition(profile);
        PromptVersion promptVersion = toPromptVersion(version);

        // 只读取一次时钟并截断到 MySQL DATETIME(3) 精度，冻结绝对半开窗口。
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        Instant windowEnd = issuedAt;
        Instant windowStart = windowEnd.minus(limits.windowDays(), ChronoUnit.DAYS);
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
        String fingerprint = fingerprintCalculator.calculate(budget.decisions());
        Instant expiresAt = issuedAt.plus(TOKEN_TTL);
        PreviewTokenPayload payload = new PreviewTokenPayload(
                1,
                userId,
                profile.getId(),
                version.getId(),
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
                UUID.randomUUID().toString(),
                issuedAt,
                expiresAt);
        String token = tokenService.issue(payload);
        return new AnalysisPreview(
                windowStart,
                windowEnd,
                resolution.totalInWindow(),
                resolution.eligibleCount(),
                resolution.eligibleCount(),
                resolution.alreadyAnalyzedCount(),
                budget.selectedCount(),
                deferredByItemLimit,
                budget.deferredByTokenBudgetCount(),
                budget.estimatedInputTokens(),
                budget.estimatedOutputTokens(),
                budget.estimatedTotalTokens(),
                AnalysisTokenEstimator.METHOD,
                expiresAt,
                token);
    }

    /** 按稳定候选顺序选择预算前缀，防止跳过较早候选后挑选较晚候选。 */
    private BudgetResult applyBudget(
            AnalysisDefinition<?, ?> definition,
            PromptVersion promptVersion,
            List<AnalysisCandidate> candidates,
            long maxEstimatedTokens) {
        List<PreviewCandidateEstimate> decisions = new ArrayList<>();
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
            decisions.add(new PreviewCandidateEstimate(
                    candidate.informationId(),
                    candidate.snapshotId(),
                    candidate.firstSeenTime(),
                    estimate.inputTokens(),
                    estimate.outputTokens(),
                    estimate.totalTokens(),
                    withinBudget));
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
            /** Candidate Limit 内全部有序决策。 */ List<PreviewCandidateEstimate> decisions) {

        private BudgetResult {
            decisions = List.copyOf(decisions);
        }
    }
}
