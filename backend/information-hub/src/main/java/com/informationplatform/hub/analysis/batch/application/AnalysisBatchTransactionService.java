package com.informationplatform.hub.analysis.batch.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.informationplatform.hub.analysis.batch.domain.AnalysisBatchItemView;
import com.informationplatform.hub.analysis.batch.domain.AnalysisBatchProgress;
import com.informationplatform.hub.analysis.batch.domain.AnalysisBatchView;
import com.informationplatform.hub.analysis.batch.infrastructure.persistence.BatchUsageAggregateRow;
import com.informationplatform.hub.analysis.batch.infrastructure.config.AnalysisBatchProperties;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchItemMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiModelInvocationMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchItemPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchPo;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewConflictException;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewService;
import com.informationplatform.hub.analysis.preview.domain.PreviewTokenPayload;
import com.informationplatform.hub.analysis.preview.domain.ResolvedAnalysisPreview;
import com.informationplatform.hub.analysis.preview.domain.ResolvedPreviewCandidate;
import com.informationplatform.hub.analysis.provider.application.AiProviderClient;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 承担 Manual Confirm 冻结写入和 Owner 安全 Batch 查询事务。 */
@Service
public class AnalysisBatchTransactionService {

    /** Batch 持久化和 Owner 查询。 */
    private final AiAnalysisBatchMapper batchMapper;
    /** Batch Item 持久化。 */
    private final AiAnalysisBatchItemMapper itemMapper;
    /** Batch Actual Usage 聚合。 */
    private final AiModelInvocationMapper invocationMapper;
    /** 与 Preview 完全一致的候选重算。 */
    private final AnalysisPreviewService previewService;
    /** 创建 Batch 前进行无网络 Provider 配置预检。 */
    private final AiProviderClient providerClient;
    /** Worker 安全开关，关闭时禁止留下无法执行的 PENDING Batch。 */
    private final AnalysisBatchProperties batchProperties;

    public AnalysisBatchTransactionService(
            AiAnalysisBatchMapper batchMapper,
            AiAnalysisBatchItemMapper itemMapper,
            AiModelInvocationMapper invocationMapper,
            AnalysisPreviewService previewService,
            AiProviderClient providerClient,
            AnalysisBatchProperties batchProperties) {
        this.batchMapper = batchMapper;
        this.itemMapper = itemMapper;
        this.invocationMapper = invocationMapper;
        this.previewService = previewService;
        this.providerClient = providerClient;
        this.batchProperties = batchProperties;
    }

    /**
     * 在同一 REPEATABLE_READ 事务内重算 Preview，并原子冻结 Batch 与 Items。
     *
     * <p>本方法只执行 Provider 配置预检，不调用外部 HTTP。
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public AnalysisBatchView createConfirmed(PreviewTokenPayload payload) {
        AiAnalysisBatchPo existing =
                batchMapper.selectManual(payload.userId(), payload.manualRequestId());
        if (existing != null) {
            return toView(existing, false);
        }

        // Confirm 必须重算相同绝对窗口并逐项比较，禁止客户端令牌掩盖候选漂移。
        ResolvedAnalysisPreview resolved = previewService.recompute(payload);
        requireSamePreview(payload, resolved);
        if (resolved.selectedCount() > 0 && !batchProperties.isWorkerEnabled()) {
            throw new AnalysisBatchUnavailableException(
                    "Analysis Batch Worker is disabled");
        }
        resolved.candidates().stream()
                .filter(ResolvedPreviewCandidate::selected)
                .findFirst()
                .ifPresent(candidate -> providerClient.validateRequest(candidate.providerRequest()));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        AiAnalysisBatchPo batch = newBatch(payload, resolved, now);
        if (batchMapper.insert(batch) != 1 || batch.getId() == null) {
            throw new AnalysisBatchPersistenceException("Analysis Batch insert affected no row");
        }
        int order = 1;
        for (ResolvedPreviewCandidate candidate : resolved.candidates()) {
            AiAnalysisBatchItemPo item = newItem(batch.getId(), order++, candidate, now);
            if (itemMapper.insert(item) != 1) {
                throw new AnalysisBatchPersistenceException(
                        "Analysis Batch Item insert affected no row");
            }
        }
        return toView(batch, true);
    }

    /** 按幂等键返回既有 Batch，供并发唯一键冲突事务回滚后复用。 */
    @Transactional(readOnly = true)
    public AnalysisBatchView findManual(long userId, String manualRequestId) {
        AiAnalysisBatchPo batch = batchMapper.selectManual(userId, manualRequestId);
        return batch == null ? null : toView(batch, false);
    }

    /** 返回当前 Owner 最近的 Batch，第一版限制 1 至 100 条。 */
    @Transactional(readOnly = true)
    public List<AnalysisBatchView> listOwned(long userId, int limit) {
        return batchMapper.selectList(
                        Wrappers.<AiAnalysisBatchPo>lambdaQuery()
                                .eq(AiAnalysisBatchPo::getUserId, userId)
                                .orderByDesc(
                                        AiAnalysisBatchPo::getCreatedAt,
                                        AiAnalysisBatchPo::getId)
                                .last("LIMIT " + limit))
                .stream()
                .map(batch -> toView(batch, false))
                .toList();
    }

    /** 返回当前 Owner 的 Batch 和全部冻结 Items。 */
    @Transactional(readOnly = true)
    public AnalysisBatchView getOwned(long userId, long batchId) {
        AiAnalysisBatchPo batch = requireOwned(userId, batchId);
        return toView(batch, true);
    }

    /** 返回当前 Owner 的实时状态和 Actual Usage 聚合。 */
    @Transactional(readOnly = true)
    public AnalysisBatchProgress getProgress(long userId, long batchId) {
        requireOwned(userId, batchId);
        return progress(batchId, userId, items(batchId));
    }

    private AiAnalysisBatchPo newBatch(
            PreviewTokenPayload payload,
            ResolvedAnalysisPreview resolved,
            LocalDateTime now) {
        AiAnalysisBatchPo batch = new AiAnalysisBatchPo();
        batch.setUserId(payload.userId());
        batch.setTriggerType("MANUAL");
        batch.setManualRequestId(payload.manualRequestId());
        batch.setInformationType(resolved.informationType());
        batch.setAnalysisDefinitionKey(resolved.definitionKey());
        batch.setAnalysisDefinitionVersion(resolved.definitionVersion());
        batch.setPromptProfileId(resolved.promptProfileId());
        batch.setPromptVersionId(resolved.promptVersionId());
        batch.setWindowBasis("FIRST_INGESTED");
        batch.setRequestedWindowDays(resolved.windowDays());
        batch.setWindowStart(LocalDateTime.ofInstant(resolved.windowStart(), ZoneOffset.UTC));
        batch.setWindowEnd(LocalDateTime.ofInstant(resolved.windowEnd(), ZoneOffset.UTC));
        batch.setRequestedMaxCandidates(resolved.maxCandidates());
        batch.setRequestedTokenBudget(resolved.maxEstimatedTokens());
        batch.setTotalInWindow(toInt(resolved.totalInWindow()));
        batch.setEligibleCount(toInt(resolved.eligibleCount()));
        batch.setAlreadyAnalyzedCount(toInt(resolved.alreadyAnalyzedCount()));
        batch.setSelectedCount(toInt(resolved.selectedCount()));
        batch.setDeferredByItemLimitCount(toInt(resolved.deferredByItemLimitCount()));
        batch.setDeferredByTokenBudgetCount(toInt(resolved.deferredByTokenBudgetCount()));
        batch.setEstimatedInputTokens(resolved.estimatedInputTokens());
        batch.setEstimatedOutputTokens(resolved.estimatedOutputTokens());
        batch.setEstimatedTotalTokens(resolved.estimatedTotalTokens());
        batch.setEstimateMethod(resolved.estimateMethod());
        if (resolved.selectedCount() == 0) {
            batch.setStatus("NOOP");
            batch.setSkipReason("NO_EXECUTABLE_ITEMS");
            batch.setCompletedAt(now);
        } else {
            batch.setStatus("PENDING");
        }
        return batch;
    }

    private AiAnalysisBatchItemPo newItem(
            long batchId,
            int selectionOrder,
            ResolvedPreviewCandidate candidate,
            LocalDateTime now) {
        AiAnalysisBatchItemPo item = new AiAnalysisBatchItemPo();
        item.setBatchId(batchId);
        item.setInformationId(candidate.candidate().informationId());
        item.setSnapshotId(candidate.candidate().snapshotId());
        item.setSelectionOrder(selectionOrder);
        item.setEstimatedInputTokens(candidate.estimate().inputTokens());
        item.setEstimatedOutputTokens(candidate.estimate().outputTokens());
        item.setEstimatedTotalTokens(candidate.estimate().totalTokens());
        if (candidate.selected()) {
            item.setStatus("SELECTED");
        } else {
            item.setStatus("DEFERRED");
            item.setDecisionReason("TOKEN_BUDGET");
            item.setCompletedAt(now);
        }
        return item;
    }

    /** 比较 Token 内全部冻结汇总和指纹，任一变化都拒绝创建 Batch。 */
    private void requireSamePreview(
            PreviewTokenPayload payload, ResolvedAnalysisPreview resolved) {
        boolean same = payload.userId() == resolved.userId()
                && payload.promptProfileId() == resolved.promptProfileId()
                && payload.promptVersionId() == resolved.promptVersionId()
                && Objects.equals(payload.definitionKey(), resolved.definitionKey())
                && payload.definitionVersion() == resolved.definitionVersion()
                && Objects.equals(payload.windowStart(), resolved.windowStart())
                && Objects.equals(payload.windowEnd(), resolved.windowEnd())
                && payload.windowDays() == resolved.windowDays()
                && payload.maxCandidates() == resolved.maxCandidates()
                && payload.maxEstimatedTokens() == resolved.maxEstimatedTokens()
                && payload.totalInWindow() == resolved.totalInWindow()
                && payload.eligibleCount() == resolved.eligibleCount()
                && payload.alreadyAnalyzedCount() == resolved.alreadyAnalyzedCount()
                && payload.selectedCount() == resolved.selectedCount()
                && payload.deferredByItemLimitCount()
                        == resolved.deferredByItemLimitCount()
                && payload.deferredByTokenBudgetCount()
                        == resolved.deferredByTokenBudgetCount()
                && payload.estimatedInputTokens() == resolved.estimatedInputTokens()
                && payload.estimatedOutputTokens() == resolved.estimatedOutputTokens()
                && payload.estimatedTotalTokens() == resolved.estimatedTotalTokens()
                && Objects.equals(payload.estimateMethod(), resolved.estimateMethod())
                && Objects.equals(
                        payload.candidateFingerprint(), resolved.candidateFingerprint());
        if (!same) {
            throw new AnalysisPreviewConflictException(
                    "PREVIEW_CANDIDATES_DRIFTED",
                    "Preview candidates changed before confirmation");
        }
    }

    private AnalysisBatchView toView(AiAnalysisBatchPo batch, boolean includeItems) {
        List<AiAnalysisBatchItemPo> itemRows =
                includeItems ? items(batch.getId()) : List.of();
        AnalysisBatchProgress progress =
                progress(batch.getId(), batch.getUserId(), itemRows);
        return new AnalysisBatchView(
                batch.getId(),
                batch.getTriggerType(),
                batch.getPromptProfileId(),
                batch.getPromptVersionId(),
                batch.getInformationType(),
                batch.getAnalysisDefinitionKey(),
                batch.getAnalysisDefinitionVersion(),
                batch.getWindowBasis(),
                batch.getRequestedWindowDays(),
                batch.getWindowStart(),
                batch.getWindowEnd(),
                batch.getRequestedMaxCandidates(),
                batch.getRequestedTokenBudget(),
                batch.getTotalInWindow(),
                batch.getEligibleCount(),
                batch.getAlreadyAnalyzedCount(),
                batch.getSelectedCount(),
                batch.getDeferredByItemLimitCount(),
                batch.getDeferredByTokenBudgetCount(),
                batch.getEstimatedInputTokens(),
                batch.getEstimatedOutputTokens(),
                batch.getEstimatedTotalTokens(),
                batch.getEstimateMethod(),
                batch.getStatus(),
                batch.getSkipReason(),
                batch.getStartedAt(),
                batch.getCompletedAt(),
                batch.getCreatedAt(),
                batch.getUpdatedAt(),
                progress,
                itemRows.stream().map(this::toItemView).toList());
    }

    private AnalysisBatchProgress progress(
            long batchId, long userId, List<AiAnalysisBatchItemPo> suppliedItems) {
        List<AiAnalysisBatchItemPo> rows =
                suppliedItems.isEmpty() ? items(batchId) : suppliedItems;
        BatchUsageAggregateRow usage = invocationMapper.aggregateBatchUsage(batchId, userId);
        return new AnalysisBatchProgress(
                rows.size(),
                count(rows, "SELECTED"),
                count(rows, "RUNNING"),
                count(rows, "SUCCEEDED"),
                count(rows, "FAILED"),
                count(rows, "SKIPPED"),
                count(rows, "DEFERRED"),
                usage == null || usage.getReportedInvocationCount() == null
                        ? 0 : usage.getReportedInvocationCount(),
                usage == null || usage.getUnavailableInvocationCount() == null
                        ? 0 : usage.getUnavailableInvocationCount(),
                usage == null ? null : usage.getInputTokens(),
                usage == null ? null : usage.getOutputTokens(),
                usage == null ? null : usage.getTotalTokens());
    }

    private List<AiAnalysisBatchItemPo> items(long batchId) {
        return itemMapper.selectList(
                Wrappers.<AiAnalysisBatchItemPo>lambdaQuery()
                        .eq(AiAnalysisBatchItemPo::getBatchId, batchId)
                        .orderByAsc(AiAnalysisBatchItemPo::getSelectionOrder));
    }

    private long count(List<AiAnalysisBatchItemPo> rows, String status) {
        return rows.stream().filter(item -> status.equals(item.getStatus())).count();
    }

    private AnalysisBatchItemView toItemView(AiAnalysisBatchItemPo item) {
        return new AnalysisBatchItemView(
                item.getId(),
                item.getInformationId(),
                item.getSnapshotId(),
                item.getAnalysisId(),
                item.getSelectionOrder(),
                item.getStatus(),
                item.getDecisionReason(),
                item.getEstimatedInputTokens(),
                item.getEstimatedOutputTokens(),
                item.getEstimatedTotalTokens(),
                item.getStartedAt(),
                item.getCompletedAt());
    }

    private AiAnalysisBatchPo requireOwned(long userId, long batchId) {
        AiAnalysisBatchPo batch = batchMapper.selectOwnedById(batchId, userId);
        if (batch == null) {
            throw new AnalysisBatchNotFoundException("Analysis Batch does not exist");
        }
        return batch;
    }

    private int toInt(long value) {
        try {
            return Math.toIntExact(value);
        } catch (ArithmeticException exception) {
            throw new AnalysisBatchPersistenceException("Analysis Batch count exceeds INT range");
        }
    }
}
