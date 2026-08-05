package com.informationplatform.hub.analysis.processing.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.definition.application.AnalysisDefinitionRegistry;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinition;
import com.informationplatform.hub.analysis.definition.domain.AnalysisInformationType;
import com.informationplatform.hub.analysis.definition.domain.AnalysisSnapshotSource;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiModelInvocationMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptVersionMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.InformationAnalysisMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiModelInvocationPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptVersionPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.InformationAnalysisPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.query.AnalysisSnapshotQueryMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.query.AnalysisSnapshotRow;
import com.informationplatform.hub.analysis.processing.domain.AnalysisExecutionPlan;
import com.informationplatform.hub.analysis.processing.domain.AnalysisPreparation;
import com.informationplatform.hub.analysis.processing.domain.AnalysisTokenEstimate;
import com.informationplatform.hub.analysis.processing.domain.AssembledAnalysisPrompt;
import com.informationplatform.hub.analysis.processing.domain.InformationAnalysisView;
import com.informationplatform.hub.analysis.processing.domain.ModelInvocationView;
import com.informationplatform.hub.analysis.prompt.domain.PromptVersion;
import com.informationplatform.hub.analysis.provider.application.AiProviderClient;
import com.informationplatform.hub.analysis.provider.domain.AiProviderException;
import com.informationplatform.hub.analysis.provider.domain.AiProviderResult;
import com.informationplatform.hub.analysis.provider.domain.AiProviderRetryDisposition;
import com.informationplatform.hub.analysis.provider.domain.AiProviderUsage;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 承担单条 Analysis 的准备、完成和 Owner 查询短事务。 */
@Service
public class InformationAnalysisTransactionService {

    private static final String RUNNING = "RUNNING";
    private static final String SUCCEEDED = "SUCCEEDED";
    private static final String FAILED = "FAILED";

    /** Prompt Profile 行锁和 Owner 查询。 */
    private final AiPromptProfileMapper profileMapper;
    /** 不可变 Prompt Version 查询。 */
    private final AiPromptVersionMapper versionMapper;
    /** 不含 rawPayload 的 Snapshot 查询。 */
    private final AnalysisSnapshotQueryMapper snapshotQueryMapper;
    /** Analysis 逻辑身份与状态持久化。 */
    private final InformationAnalysisMapper analysisMapper;
    /** Invocation 与 Actual Usage 持久化。 */
    private final AiModelInvocationMapper invocationMapper;
    /** 当前平台 Definition Registry。 */
    private final AnalysisDefinitionRegistry definitionRegistry;
    /** 安全 Prompt Assembly。 */
    private final AnalysisPromptAssembler promptAssembler;
    /** 单条 Estimate。 */
    private final AnalysisTokenEstimator tokenEstimator;
    /** Provider 配置预检与非事务外部执行边界。 */
    private final AiProviderClient providerClient;
    /** Snapshot/result JSON 的安全解析与序列化。 */
    private final ObjectMapper objectMapper;

    public InformationAnalysisTransactionService(
            AiPromptProfileMapper profileMapper,
            AiPromptVersionMapper versionMapper,
            AnalysisSnapshotQueryMapper snapshotQueryMapper,
            InformationAnalysisMapper analysisMapper,
            AiModelInvocationMapper invocationMapper,
            AnalysisDefinitionRegistry definitionRegistry,
            AnalysisPromptAssembler promptAssembler,
            AnalysisTokenEstimator tokenEstimator,
            AiProviderClient providerClient,
            ObjectMapper objectMapper) {
        this.profileMapper = profileMapper;
        this.versionMapper = versionMapper;
        this.snapshotQueryMapper = snapshotQueryMapper;
        this.analysisMapper = analysisMapper;
        this.invocationMapper = invocationMapper;
        this.definitionRegistry = definitionRegistry;
        this.promptAssembler = promptAssembler;
        this.tokenEstimator = tokenEstimator;
        this.providerClient = providerClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 冻结 Owner、Active Prompt Version、Snapshot 和 Definition，并创建 RUNNING 审计记录。
     *
     * <p>Provider 仅执行无网络配置预检；真实 HTTP 在本事务提交后由编排 Service 调用。
     */
    @Transactional
    public AnalysisPreparation prepare(
            long userId,
            long informationId,
            Long snapshotId,
            long promptProfileId,
            boolean retryFailed) {
        requirePositive(userId, "userId");
        requirePositive(informationId, "informationId");
        requirePositive(promptProfileId, "promptProfileId");
        if (snapshotId != null) {
            requirePositive(snapshotId, "snapshotId");
        }

        // 锁定 Profile，确保读取的 Active Version 与本次冻结关系一致。
        AiPromptProfilePo profile = profileMapper.selectOwnedByIdForUpdate(promptProfileId, userId);
        if (profile == null) {
            throw notFound("PROMPT_PROFILE_NOT_FOUND", "Prompt Profile does not exist");
        }
        if (!"ACTIVE".equals(profile.getStatus())) {
            throw request("PROMPT_PROFILE_DISABLED", "Prompt Profile is disabled");
        }
        if (profile.getActiveVersionId() == null) {
            throw request("PROMPT_ACTIVE_VERSION_REQUIRED", "Prompt Profile has no active version");
        }
        AiPromptVersionPo version = versionMapper.selectOne(
                Wrappers.<AiPromptVersionPo>lambdaQuery()
                        .eq(AiPromptVersionPo::getId, profile.getActiveVersionId())
                        .eq(AiPromptVersionPo::getPromptProfileId, profile.getId()));
        if (version == null) {
            throw new AnalysisPersistenceException(
                    "Active Prompt Version does not belong to its Profile");
        }

        AnalysisDefinition<?, ?> definition;
        try {
            definition = definitionRegistry.requireCurrent(profile.getAnalysisDefinitionKey());
        } catch (IllegalArgumentException exception) {
            throw request(
                    "ANALYSIS_DEFINITION_UNAVAILABLE",
                    "Prompt Profile Analysis Definition is unavailable");
        }
        AnalysisSnapshotRow snapshot = snapshotId == null
                ? snapshotQueryMapper.selectCurrent(informationId)
                : snapshotQueryMapper.selectExplicit(informationId, snapshotId);
        if (snapshot == null) {
            throw notFound("ANALYSIS_SNAPSHOT_NOT_FOUND", "Information Snapshot does not exist");
        }
        return prepareResolved(
                userId,
                profile,
                version,
                definition,
                snapshot,
                retryFailed,
                null);
    }

    /**
     * 使用 Batch 已冻结的 Prompt/Definition/Snapshot 创建或复用 Analysis。
     *
     * <p>调用方 Worker 领取事务会参与本事务；Invocation 必须绑定 batchItemId。
     */
    @Transactional
    public AnalysisPreparation prepareFrozen(
            long userId,
            long informationId,
            long snapshotId,
            long promptProfileId,
            long promptVersionId,
            String definitionKey,
            int definitionVersion,
            long batchItemId) {
        requirePositive(userId, "userId");
        requirePositive(informationId, "informationId");
        requirePositive(snapshotId, "snapshotId");
        requirePositive(promptProfileId, "promptProfileId");
        requirePositive(promptVersionId, "promptVersionId");
        requirePositive(batchItemId, "batchItemId");

        // Batch 已冻结版本，因此只验证 Owner/归属，不读取后来切换的 Active Version。
        AiPromptProfilePo profile = profileMapper.selectOwnedByIdForUpdate(
                promptProfileId, userId);
        if (profile == null) {
            throw notFound("PROMPT_PROFILE_NOT_FOUND", "Prompt Profile does not exist");
        }
        AiPromptVersionPo version = versionMapper.selectOne(
                Wrappers.<AiPromptVersionPo>lambdaQuery()
                        .eq(AiPromptVersionPo::getId, promptVersionId)
                        .eq(AiPromptVersionPo::getPromptProfileId, promptProfileId));
        if (version == null) {
            throw new AnalysisPersistenceException(
                    "Frozen Prompt Version does not belong to its Profile");
        }
        AnalysisDefinition<?, ?> definition;
        try {
            definition = definitionRegistry.require(definitionKey, definitionVersion);
        } catch (IllegalArgumentException exception) {
            throw request(
                    "ANALYSIS_DEFINITION_UNAVAILABLE",
                    "Frozen Analysis Definition is unavailable");
        }
        AnalysisSnapshotRow snapshot =
                snapshotQueryMapper.selectExplicit(informationId, snapshotId);
        if (snapshot == null) {
            throw notFound("ANALYSIS_SNAPSHOT_NOT_FOUND", "Information Snapshot does not exist");
        }
        return prepareResolved(
                userId,
                profile,
                version,
                definition,
                snapshot,
                true,
                batchItemId);
    }

    /** 使用已经解析并校验的冻结上下文创建 Analysis 和 RUNNING Invocation。 */
    private AnalysisPreparation prepareResolved(
            long userId,
            AiPromptProfilePo profile,
            AiPromptVersionPo version,
            AnalysisDefinition<?, ?> definition,
            AnalysisSnapshotRow snapshot,
            boolean retryFailed,
            Long batchItemId) {
        AnalysisSnapshotSource snapshotSource = toSnapshotSource(snapshot);
        PromptVersion promptVersion = new PromptVersion(
                version.getId(),
                version.getPromptProfileId(),
                version.getVersionNo(),
                version.getContent(),
                version.getContentHash(),
                version.getCreatedAt());
        AssembledAnalysisPrompt assembled = assemble(definition, promptVersion, snapshotSource);
        AnalysisTokenEstimate estimate = tokenEstimator.estimate(assembled.providerRequest());

        InformationAnalysisPo analysis = analysisMapper.selectIdentityForUpdate(
                userId,
                snapshot.getSnapshotId(),
                version.getId(),
                definition.id().key(),
                definition.id().version());
        if (analysis != null && SUCCEEDED.equals(analysis.getStatus())) {
            return AnalysisPreparation.reused(toView(analysis));
        }
        if (analysis != null && !FAILED.equals(analysis.getStatus())) {
            throw new AnalysisConflictException(
                    "ANALYSIS_ALREADY_RUNNING", "The same Analysis is already running");
        }
        if (analysis != null && !retryFailed) {
            throw new AnalysisConflictException(
                    "ANALYSIS_RETRY_REQUIRED", "Failed Analysis requires explicit retry");
        }

        // 禁用或配置不完整时在任何 Analysis/Invocation 写入前失败，且不会发起网络调用。
        providerClient.validateRequest(assembled.providerRequest());
        LocalDateTime now = utcNow();
        if (analysis == null) {
            analysis = newAnalysis(
                    userId, profile, version, snapshot, definition, estimate, now);
            try {
                if (analysisMapper.insert(analysis) != 1 || analysis.getId() == null) {
                    throw new AnalysisPersistenceException("Analysis insert affected no row");
                }
            } catch (DuplicateKeyException exception) {
                // 数据库唯一键收敛并发首次调用，禁止第二次 Provider 请求。
                throw new AnalysisConflictException(
                        "ANALYSIS_ALREADY_RUNNING", "The same Analysis is already running");
            }
        } else {
            resetFailedAnalysis(analysis, estimate, now);
            if (analysisMapper.updateById(analysis) != 1) {
                throw new AnalysisPersistenceException("Analysis retry update affected no row");
            }
        }

        AiModelInvocationPo invocation = new AiModelInvocationPo();
        invocation.setAnalysisId(analysis.getId());
        invocation.setUserId(userId);
        invocation.setBatchItemId(batchItemId);
        invocation.setProvider(providerClient.providerId());
        invocation.setModelName(providerClient.modelName());
        invocation.setAttemptNo(invocationMapper.selectMaxAttemptNo(analysis.getId()) + 1);
        invocation.setStatus(RUNNING);
        invocation.setUsageStatus("UNAVAILABLE");
        invocation.setStartedAt(now);
        if (invocationMapper.insert(invocation) != 1 || invocation.getId() == null) {
            throw new AnalysisPersistenceException("Model Invocation insert affected no row");
        }
        return AnalysisPreparation.execute(new AnalysisExecutionPlan(
                analysis.getId(),
                invocation.getId(),
                userId,
                definition,
                assembled.providerRequest()));
    }

    /** 保存 Provider 成功、Schema 成功及 Actual Usage。 */
    @Transactional
    public InformationAnalysisView completeSuccess(
            AnalysisExecutionPlan plan,
            AiProviderResult result,
            JsonNode resultJson,
            Integer relevanceScore,
            String summary) {
        CompletionRows rows = lockCompletionRows(plan);
        completeInvocationFromResult(rows.invocation(), result, SUCCEEDED, null, null);
        InformationAnalysisPo analysis = rows.analysis();
        analysis.setStatus(SUCCEEDED);
        analysis.setResultJson(writeJson(resultJson));
        analysis.setRelevanceScore(relevanceScore);
        analysis.setSummary(summary);
        clearAnalysisFailure(analysis);
        analysis.setCompletedAt(utcNow());
        persistCompletion(rows);
        return toView(analysis);
    }

    /** Provider 已完成但输出校验失败时保留全部已报告 Usage，并只令 Analysis 失败。 */
    @Transactional
    public InformationAnalysisView completeOutputFailure(
            AnalysisExecutionPlan plan,
            AiProviderResult result,
            String failureCode,
            String failureMessage) {
        CompletionRows rows = lockCompletionRows(plan);
        completeInvocationFromResult(rows.invocation(), result, SUCCEEDED, null, null);
        failAnalysis(rows.analysis(), failureCode, failureMessage);
        persistCompletion(rows);
        return toView(rows.analysis());
    }

    /** 保存 Provider 异常、Usage 和冻结的不确定重试语义。 */
    @Transactional
    public InformationAnalysisView completeProviderFailure(
            AnalysisExecutionPlan plan, AiProviderException exception) {
        CompletionRows rows = lockCompletionRows(plan);
        String invocationStatus = invocationFailureStatus(exception);
        AiModelInvocationPo invocation = rows.invocation();
        invocation.setStatus(invocationStatus);
        invocation.setProviderRequestId(exception.providerRequestId());
        invocation.setLatencyMs(exception.latencyMs());
        applyUsage(invocation, exception.usage());
        invocation.setErrorCode("AI_PROVIDER_" + exception.errorType().name());
        invocation.setErrorMessage(safeMessage(exception.getMessage()));
        invocation.setCompletedAt(utcNow());
        failAnalysis(
                rows.analysis(),
                "AI_PROVIDER_" + exception.errorType().name(),
                safeMessage(exception.getMessage()));
        persistCompletion(rows);
        return toView(rows.analysis());
    }

    /** 未分类运行时异常留下 UNKNOWN Invocation，禁止普通重复调用。 */
    @Transactional
    public void completeUnexpectedFailure(AnalysisExecutionPlan plan) {
        CompletionRows rows = lockCompletionRows(plan);
        AiModelInvocationPo invocation = rows.invocation();
        invocation.setStatus("UNKNOWN");
        invocation.setErrorCode("AI_EXECUTION_UNKNOWN");
        invocation.setErrorMessage("AI execution result is unknown");
        invocation.setCompletedAt(utcNow());
        failAnalysis(
                rows.analysis(), "AI_EXECUTION_UNKNOWN", "AI execution result is unknown");
        persistCompletion(rows);
    }

    /** 按当前 Owner 返回 Analysis 与全部 Invocation；越权和不存在统一为 404。 */
    @Transactional(readOnly = true)
    public InformationAnalysisView getOwned(long userId, long analysisId) {
        requirePositive(userId, "userId");
        requirePositive(analysisId, "analysisId");
        InformationAnalysisPo analysis = analysisMapper.selectOwnedById(analysisId, userId);
        if (analysis == null) {
            throw notFound("ANALYSIS_NOT_FOUND", "Analysis does not exist");
        }
        return toView(analysis);
    }

    @SuppressWarnings("unchecked")
    private <I> AssembledAnalysisPrompt assemble(
            AnalysisDefinition<?, ?> definition,
            PromptVersion promptVersion,
            AnalysisSnapshotSource snapshotSource) {
        return promptAssembler.assemble(
                (AnalysisDefinition<I, ?>) definition, promptVersion, snapshotSource);
    }

    private AnalysisSnapshotSource toSnapshotSource(AnalysisSnapshotRow row) {
        try {
            return new AnalysisSnapshotSource(
                    row.getSnapshotId(),
                    row.getInformationId(),
                    AnalysisInformationType.valueOf(row.getInformationType()),
                    row.getTitle(),
                    row.getContent(),
                    objectMapper.readTree(row.getStandardizedPayload()));
        } catch (IllegalArgumentException | JsonProcessingException exception) {
            throw new AnalysisPersistenceException(
                    "Snapshot standardized payload could not be read", exception);
        }
    }

    private InformationAnalysisPo newAnalysis(
            long userId,
            AiPromptProfilePo profile,
            AiPromptVersionPo version,
            AnalysisSnapshotRow snapshot,
            AnalysisDefinition<?, ?> definition,
            AnalysisTokenEstimate estimate,
            LocalDateTime now) {
        InformationAnalysisPo analysis = new InformationAnalysisPo();
        analysis.setUserId(userId);
        analysis.setInformationId(snapshot.getInformationId());
        analysis.setSnapshotId(snapshot.getSnapshotId());
        analysis.setInformationType(definition.informationType().name());
        analysis.setAnalysisDefinitionKey(definition.id().key());
        analysis.setAnalysisDefinitionVersion(definition.id().version());
        analysis.setAnalysisPurpose(definition.analysisPurpose().name());
        analysis.setPromptProfileId(profile.getId());
        analysis.setPromptVersionId(version.getId());
        analysis.setStatus(RUNNING);
        applyEstimate(analysis, estimate);
        analysis.setStartedAt(now);
        return analysis;
    }

    private void resetFailedAnalysis(
            InformationAnalysisPo analysis, AnalysisTokenEstimate estimate, LocalDateTime now) {
        analysis.setStatus(RUNNING);
        analysis.setResultJson(null);
        analysis.setRelevanceScore(null);
        analysis.setSummary(null);
        applyEstimate(analysis, estimate);
        clearAnalysisFailure(analysis);
        analysis.setStartedAt(now);
        analysis.setCompletedAt(null);
    }

    private void applyEstimate(InformationAnalysisPo analysis, AnalysisTokenEstimate estimate) {
        analysis.setEstimatedInputTokens(estimate.inputTokens());
        analysis.setEstimatedOutputTokens(estimate.outputTokens());
        analysis.setEstimatedTotalTokens(estimate.totalTokens());
        analysis.setEstimateMethod(estimate.method());
    }

    private CompletionRows lockCompletionRows(AnalysisExecutionPlan plan) {
        InformationAnalysisPo analysis = analysisMapper.selectOwnedByIdForUpdate(
                plan.analysisId(), plan.userId());
        if (analysis == null || !RUNNING.equals(analysis.getStatus())) {
            throw new AnalysisPersistenceException("RUNNING Analysis could not be completed");
        }
        AiModelInvocationPo invocation = invocationMapper.selectForUpdate(
                plan.invocationId(), plan.analysisId(), plan.userId());
        if (invocation == null || !RUNNING.equals(invocation.getStatus())) {
            throw new AnalysisPersistenceException("RUNNING Invocation could not be completed");
        }
        return new CompletionRows(analysis, invocation);
    }

    private void completeInvocationFromResult(
            AiModelInvocationPo invocation,
            AiProviderResult result,
            String status,
            String errorCode,
            String errorMessage) {
        invocation.setProvider(result.provider());
        invocation.setModelName(result.modelName());
        invocation.setProviderRequestId(result.providerRequestId());
        invocation.setStatus(status);
        invocation.setFinishReason(result.finishReason());
        applyUsage(invocation, result.usage());
        invocation.setLatencyMs(result.latencyMs());
        invocation.setErrorCode(errorCode);
        invocation.setErrorMessage(errorMessage);
        invocation.setCompletedAt(utcNow());
    }

    private void applyUsage(AiModelInvocationPo invocation, AiProviderUsage usage) {
        invocation.setInputTokens(usage.inputTokens());
        invocation.setOutputTokens(usage.outputTokens());
        invocation.setTotalTokens(usage.totalTokens());
        invocation.setCachedInputTokens(usage.cachedInputTokens());
        invocation.setReasoningTokens(usage.reasoningTokens());
        invocation.setUsageStatus(usage.status().name());
    }

    private void failAnalysis(
            InformationAnalysisPo analysis, String failureCode, String failureMessage) {
        analysis.setStatus(FAILED);
        analysis.setResultJson(null);
        analysis.setRelevanceScore(null);
        analysis.setSummary(null);
        analysis.setFailureCode(failureCode);
        analysis.setFailureMessage(safeMessage(failureMessage));
        analysis.setCompletedAt(utcNow());
    }

    private void clearAnalysisFailure(InformationAnalysisPo analysis) {
        analysis.setFailureCode(null);
        analysis.setFailureMessage(null);
    }

    private void persistCompletion(CompletionRows rows) {
        if (invocationMapper.updateById(rows.invocation()) != 1
                || analysisMapper.updateById(rows.analysis()) != 1) {
            throw new AnalysisPersistenceException("Analysis completion affected no row");
        }
    }

    private String invocationFailureStatus(AiProviderException exception) {
        if (exception.errorType().name().equals("TIMEOUT")) {
            return "TIMEOUT";
        }
        return exception.retryDisposition() == AiProviderRetryDisposition.AMBIGUOUS_DO_NOT_AUTO_RETRY
                ? "UNKNOWN"
                : FAILED;
    }

    private InformationAnalysisView toView(InformationAnalysisPo analysis) {
        List<ModelInvocationView> invocations = invocationMapper.selectList(
                        Wrappers.<AiModelInvocationPo>lambdaQuery()
                                .eq(AiModelInvocationPo::getAnalysisId, analysis.getId())
                                .eq(AiModelInvocationPo::getUserId, analysis.getUserId())
                                .orderByAsc(AiModelInvocationPo::getAttemptNo))
                .stream()
                .map(this::toInvocationView)
                .toList();
        return new InformationAnalysisView(
                analysis.getId(),
                analysis.getInformationId(),
                analysis.getSnapshotId(),
                analysis.getInformationType(),
                analysis.getAnalysisDefinitionKey(),
                analysis.getAnalysisDefinitionVersion(),
                analysis.getAnalysisPurpose(),
                analysis.getPromptProfileId(),
                analysis.getPromptVersionId(),
                analysis.getStatus(),
                readJson(analysis.getResultJson()),
                analysis.getRelevanceScore(),
                analysis.getSummary(),
                analysis.getEstimatedInputTokens(),
                analysis.getEstimatedOutputTokens(),
                analysis.getEstimatedTotalTokens(),
                analysis.getEstimateMethod(),
                analysis.getFailureCode(),
                analysis.getFailureMessage(),
                analysis.getStartedAt(),
                analysis.getCompletedAt(),
                analysis.getCreatedAt(),
                analysis.getUpdatedAt(),
                invocations);
    }

    private ModelInvocationView toInvocationView(AiModelInvocationPo invocation) {
        return new ModelInvocationView(
                invocation.getId(),
                invocation.getAttemptNo(),
                invocation.getProvider(),
                invocation.getModelName(),
                invocation.getProviderRequestId(),
                invocation.getStatus(),
                invocation.getFinishReason(),
                invocation.getInputTokens(),
                invocation.getOutputTokens(),
                invocation.getTotalTokens(),
                invocation.getCachedInputTokens(),
                invocation.getReasoningTokens(),
                invocation.getUsageStatus(),
                invocation.getLatencyMs(),
                invocation.getErrorCode(),
                invocation.getErrorMessage(),
                invocation.getStartedAt(),
                invocation.getCompletedAt());
    }

    private JsonNode readJson(String value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new AnalysisPersistenceException("Analysis result JSON could not be read", exception);
        }
    }

    private String writeJson(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new AnalysisPersistenceException("Analysis result JSON could not be written", exception);
        }
    }

    private String safeMessage(String value) {
        if (value == null || value.isBlank()) {
            return "AI analysis failed";
        }
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }

    private void requirePositive(long value, String field) {
        if (value <= 0) {
            throw request("INVALID_ANALYSIS_REQUEST", field + " must be positive");
        }
    }

    private AnalysisRequestException request(String code, String message) {
        return new AnalysisRequestException(code, message);
    }

    private AnalysisNotFoundException notFound(String code, String message) {
        return new AnalysisNotFoundException(code, message);
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /** 同一完成事务内已锁定的两条审计记录。 */
    private record CompletionRows(
            /** RUNNING Analysis。 */ InformationAnalysisPo analysis,
            /** RUNNING Invocation。 */ AiModelInvocationPo invocation) {
    }
}
