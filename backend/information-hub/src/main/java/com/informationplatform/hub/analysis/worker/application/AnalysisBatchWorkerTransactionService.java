package com.informationplatform.hub.analysis.worker.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.informationplatform.hub.analysis.batch.application.AnalysisBatchPersistenceException;
import com.informationplatform.hub.analysis.batch.application.AnalysisBatchTerminalEventPublisher;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchItemMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiModelInvocationMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.InformationAnalysisMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchItemPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiModelInvocationPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.InformationAnalysisPo;
import com.informationplatform.hub.analysis.processing.application.InformationAnalysisTransactionService;
import com.informationplatform.hub.analysis.processing.domain.AnalysisPreparation;
import com.informationplatform.hub.analysis.worker.domain.AnalysisBatchWork;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Worker 领取、完成和重启恢复的短事务边界。 */
@Service
public class AnalysisBatchWorkerTransactionService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AnalysisBatchWorkerTransactionService.class);

    /** Batch 行锁与状态持久化。 */
    private final AiAnalysisBatchMapper batchMapper;
    /** Item 领取、恢复与状态持久化。 */
    private final AiAnalysisBatchItemMapper itemMapper;
    /** Invocation 重启恢复。 */
    private final AiModelInvocationMapper invocationMapper;
    /** Analysis 重启恢复。 */
    private final InformationAnalysisMapper analysisMapper;
    /** 冻结 Analysis/Invocation 的共用单条能力。 */
    private final InformationAnalysisTransactionService analysisTransactions;
    /** Batch 终态进程内事件发布器。 */
    private final AnalysisBatchTerminalEventPublisher terminalEventPublisher;

    public AnalysisBatchWorkerTransactionService(
            AiAnalysisBatchMapper batchMapper,
            AiAnalysisBatchItemMapper itemMapper,
            AiModelInvocationMapper invocationMapper,
            InformationAnalysisMapper analysisMapper,
            InformationAnalysisTransactionService analysisTransactions,
            AnalysisBatchTerminalEventPublisher terminalEventPublisher) {
        this.batchMapper = batchMapper;
        this.itemMapper = itemMapper;
        this.invocationMapper = invocationMapper;
        this.analysisMapper = analysisMapper;
        this.analysisTransactions = analysisTransactions;
        this.terminalEventPublisher = terminalEventPublisher;
    }

    /**
     * 使用 SKIP LOCKED 领取一个 Item，并在同一事务内创建 Analysis/Invocation。
     *
     * <p>若逻辑身份已成功，则直接把 Item 标记为 SKIPPED，不产生 Provider 调用。
     */
    @Transactional
    public AnalysisBatchWork claimNext() {
        AiAnalysisBatchItemPo item = itemMapper.selectNextForUpdate();
        if (item == null) {
            return null;
        }
        AiAnalysisBatchPo batch = requireBatchForUpdate(item.getBatchId());
        LocalDateTime now = utcNow();
        if ("PENDING".equals(batch.getStatus())) {
            batch.setStatus("RUNNING");
            batch.setStartedAt(now);
            updateBatch(batch);
            LOGGER.info(
                    "Analysis batch started, batchId={}, candidateCount={}",
                    batch.getId(),
                    batch.getSelectedCount());
        }

        AnalysisPreparation preparation = analysisTransactions.prepareFrozen(
                batch.getUserId(),
                item.getInformationId(),
                item.getSnapshotId(),
                batch.getPromptProfileId(),
                batch.getPromptVersionId(),
                batch.getAnalysisDefinitionKey(),
                batch.getAnalysisDefinitionVersion(),
                item.getId());
        if (preparation.reused() != null) {
            item.setAnalysisId(preparation.reused().id());
            item.setStatus("SKIPPED");
            item.setDecisionReason("ALREADY_ANALYZED");
            item.setStartedAt(now);
            item.setCompletedAt(now);
            updateItem(item);
            finalizeBatch(batch);
            return null;
        }

        item.setAnalysisId(preparation.executionPlan().analysisId());
        item.setStatus("RUNNING");
        item.setStartedAt(now);
        updateItem(item);
        return new AnalysisBatchWork(batch.getId(), item.getId(), preparation);
    }

    /** 根据已经持久化的 Analysis 结果完成 Item，并在必要时收敛 Batch 终态。 */
    @Transactional
    public void complete(long itemId, long analysisId, String analysisStatus) {
        AiAnalysisBatchItemPo item = itemMapper.selectByIdForUpdate(itemId);
        if (item == null
                || !"RUNNING".equals(item.getStatus())
                || item.getAnalysisId() == null
                || item.getAnalysisId() != analysisId) {
            throw new AnalysisBatchPersistenceException(
                    "RUNNING Batch Item could not be completed");
        }
        AiAnalysisBatchPo batch = requireBatchForUpdate(item.getBatchId());
        item.setStatus("SUCCEEDED".equals(analysisStatus) ? "SUCCEEDED" : "FAILED");
        if (!"SUCCEEDED".equals(analysisStatus)) {
            item.setDecisionReason("ANALYSIS_FAILED");
        }
        item.setCompletedAt(utcNow());
        updateItem(item);
        finalizeBatch(batch);
    }

    /**
     * 应用启动后的首次 Worker 轮询恢复遗留 RUNNING。
     *
     * <p>Provider 是否已收费无法确认，因此 Invocation 统一转 UNKNOWN，绝不自动重试。
     */
    @Transactional
    public int recoverInterrupted() {
        List<AiAnalysisBatchItemPo> runningItems = itemMapper.selectRunningForUpdate();
        Set<Long> affectedBatches = new HashSet<>();
        LocalDateTime now = utcNow();
        for (AiAnalysisBatchItemPo item : runningItems) {
            AiAnalysisBatchPo batch = requireBatchForUpdate(item.getBatchId());
            affectedBatches.add(batch.getId());
            AiModelInvocationPo invocation =
                    invocationMapper.selectRunningByBatchItemForUpdate(item.getId());
            if (invocation != null) {
                invocation.setStatus("UNKNOWN");
                invocation.setErrorCode("AI_EXECUTION_INTERRUPTED");
                invocation.setErrorMessage("AI execution was interrupted before completion");
                invocation.setCompletedAt(now);
                if (invocationMapper.updateById(invocation) != 1) {
                    throw new AnalysisBatchPersistenceException(
                            "Interrupted Invocation recovery affected no row");
                }
            }
            if (item.getAnalysisId() != null) {
                InformationAnalysisPo analysis = analysisMapper.selectOwnedByIdForUpdate(
                        item.getAnalysisId(), batch.getUserId());
                if (analysis != null && "RUNNING".equals(analysis.getStatus())) {
                    analysis.setStatus("FAILED");
                    analysis.setFailureCode("AI_EXECUTION_INTERRUPTED");
                    analysis.setFailureMessage("AI execution was interrupted before completion");
                    analysis.setCompletedAt(now);
                    if (analysisMapper.updateById(analysis) != 1) {
                        throw new AnalysisBatchPersistenceException(
                                "Interrupted Analysis recovery affected no row");
                    }
                }
            }
            item.setStatus("FAILED");
            item.setDecisionReason("WORKER_INTERRUPTED");
            item.setCompletedAt(now);
            updateItem(item);
        }
        for (Long batchId : affectedBatches) {
            finalizeBatch(requireBatchForUpdate(batchId));
        }
        return runningItems.size();
    }

    /** 所有可执行 Item 终结后统一派生 Batch 终态；DEFERRED 不算失败。 */
    private void finalizeBatch(AiAnalysisBatchPo batch) {
        List<AiAnalysisBatchItemPo> items = itemMapper.selectList(
                Wrappers.<AiAnalysisBatchItemPo>lambdaQuery()
                        .eq(AiAnalysisBatchItemPo::getBatchId, batch.getId()));
        boolean unfinished = items.stream().anyMatch(item ->
                "SELECTED".equals(item.getStatus()) || "RUNNING".equals(item.getStatus()));
        if (unfinished) {
            return;
        }
        long executable = items.stream()
                .filter(item -> !"DEFERRED".equals(item.getStatus()))
                .count();
        long successful = items.stream()
                .filter(item -> "SUCCEEDED".equals(item.getStatus())
                        || "SKIPPED".equals(item.getStatus()))
                .count();
        long failed = items.stream()
                .filter(item -> "FAILED".equals(item.getStatus()))
                .count();
        if (executable == 0) {
            batch.setStatus("NOOP");
            batch.setSkipReason("NO_EXECUTABLE_ITEMS");
        } else if (failed == 0) {
            batch.setStatus("COMPLETED");
        } else if (successful > 0) {
            batch.setStatus("PARTIAL_FAILED");
        } else {
            batch.setStatus("FAILED");
        }
        batch.setCompletedAt(utcNow());
        updateBatch(batch);
        terminalEventPublisher.publishIfTerminal(batch);
        LOGGER.info(
                "Analysis batch completed, batchId={}, status={}, executableCount={}, successfulCount={}, failedCount={}, durationMs={}",
                batch.getId(),
                batch.getStatus(),
                executable,
                successful,
                failed,
                durationMillis(batch));
    }

    private AiAnalysisBatchPo requireBatchForUpdate(long batchId) {
        AiAnalysisBatchPo batch = batchMapper.selectByIdForUpdate(batchId);
        if (batch == null) {
            throw new AnalysisBatchPersistenceException("Analysis Batch does not exist");
        }
        return batch;
    }

    private void updateBatch(AiAnalysisBatchPo batch) {
        if (batchMapper.updateById(batch) != 1) {
            throw new AnalysisBatchPersistenceException("Analysis Batch update affected no row");
        }
    }

    private void updateItem(AiAnalysisBatchItemPo item) {
        if (itemMapper.updateById(item) != 1) {
            throw new AnalysisBatchPersistenceException(
                    "Analysis Batch Item update affected no row");
        }
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private long durationMillis(AiAnalysisBatchPo batch) {
        if (batch.getStartedAt() == null || batch.getCompletedAt() == null) {
            return 0;
        }
        return Math.max(
                0,
                java.time.Duration.between(batch.getStartedAt(), batch.getCompletedAt())
                        .toMillis());
    }
}
