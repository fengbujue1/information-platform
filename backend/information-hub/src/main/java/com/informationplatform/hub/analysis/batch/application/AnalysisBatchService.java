package com.informationplatform.hub.analysis.batch.application;

import com.informationplatform.hub.analysis.batch.domain.AnalysisBatchProgress;
import com.informationplatform.hub.analysis.batch.domain.AnalysisBatchView;
import com.informationplatform.hub.analysis.preview.application.PreviewTokenService;
import com.informationplatform.hub.analysis.preview.domain.PreviewTokenPayload;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/** 编排 Manual Confirm 和 Owner 安全 Batch 查询。 */
@Service
public class AnalysisBatchService {

    /** Session Owner。 */
    private final CurrentUserProvider currentUserProvider;
    /** HMAC、到期时间和 Owner 验证。 */
    private final PreviewTokenService tokenService;
    /** 原子创建和只读查询事务。 */
    private final AnalysisBatchTransactionService transactionService;

    public AnalysisBatchService(
            CurrentUserProvider currentUserProvider,
            PreviewTokenService tokenService,
            AnalysisBatchTransactionService transactionService) {
        this.currentUserProvider = currentUserProvider;
        this.tokenService = tokenService;
        this.transactionService = transactionService;
    }

    /** 验证 Preview Token 并快速创建或复用异步 Batch。 */
    public AnalysisBatchView confirm(String previewToken) {
        long userId = currentUserProvider.requireCurrentUser().id();
        PreviewTokenPayload payload = tokenService.verifyForOwner(previewToken, userId);
        AnalysisBatchView existing =
                transactionService.findManual(userId, payload.manualRequestId());
        if (existing != null) {
            return existing;
        }
        try {
            return transactionService.createConfirmed(payload);
        } catch (DuplicateKeyException exception) {
            // 并发 Confirm 由数据库唯一键收敛；失败事务回滚后返回胜出 Batch。
            AnalysisBatchView concurrent =
                    transactionService.findManual(userId, payload.manualRequestId());
            if (concurrent == null) {
                throw exception;
            }
            return concurrent;
        }
    }

    /** 返回当前 Owner 最近的 Batch。 */
    public List<AnalysisBatchView> list(Integer limit) {
        int resolvedLimit = limit == null ? 20 : limit;
        if (resolvedLimit < 1 || resolvedLimit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
        return transactionService.listOwned(
                currentUserProvider.requireCurrentUser().id(), resolvedLimit);
    }

    /** 返回当前 Owner 的 Batch Detail。 */
    public AnalysisBatchView get(long batchId) {
        return transactionService.getOwned(
                currentUserProvider.requireCurrentUser().id(), batchId);
    }

    /** 返回当前 Owner 的实时 Batch Progress。 */
    public AnalysisBatchProgress progress(long batchId) {
        return transactionService.getProgress(
                currentUserProvider.requireCurrentUser().id(), batchId);
    }
}
