package com.informationplatform.hub.analysis.processing.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinition;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinitionValidationException;
import com.informationplatform.hub.analysis.processing.domain.AnalysisExecutionPlan;
import com.informationplatform.hub.analysis.processing.domain.AnalysisOutputProcessingException;
import com.informationplatform.hub.analysis.processing.domain.AnalysisPreparation;
import com.informationplatform.hub.analysis.processing.domain.InformationAnalysisView;
import com.informationplatform.hub.analysis.processing.domain.ValidatedAnalysisOutput;
import com.informationplatform.hub.analysis.provider.application.AiProviderClient;
import com.informationplatform.hub.analysis.provider.domain.AiProviderException;
import com.informationplatform.hub.analysis.provider.domain.AiProviderResult;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import org.springframework.stereotype.Service;

/** 在数据库事务之外编排一次同步单条 Information Analysis。 */
@Service
public class InformationAnalysisService {

    /** 从 Session 获取不可伪造的 Owner。 */
    private final CurrentUserProvider currentUserProvider;
    /** Analysis 准备、完成和查询短事务。 */
    private final InformationAnalysisTransactionService transactionService;
    /** 唯一真实 Provider 调用边界。 */
    private final AiProviderClient providerClient;
    /** 单一 JSON 与 Definition Schema 后处理。 */
    private final AnalysisOutputProcessor outputProcessor;

    public InformationAnalysisService(
            CurrentUserProvider currentUserProvider,
            InformationAnalysisTransactionService transactionService,
            AiProviderClient providerClient,
            AnalysisOutputProcessor outputProcessor) {
        this.currentUserProvider = currentUserProvider;
        this.transactionService = transactionService;
        this.providerClient = providerClient;
        this.outputProcessor = outputProcessor;
    }

    /**
     * 执行或复用单条 Analysis。
     *
     * <p>prepare 事务提交后才调用 Provider；任何结果都在新的完成短事务内持久化。
     */
    public InformationAnalysisView execute(
            long informationId,
            Long snapshotId,
            long promptProfileId,
            boolean retryFailed) {
        long userId = currentUserProvider.requireCurrentUser().id();
        AnalysisPreparation preparation = transactionService.prepare(
                userId, informationId, snapshotId, promptProfileId, retryFailed);
        if (preparation.reused() != null) {
            return preparation.reused();
        }

        AnalysisExecutionPlan plan = preparation.executionPlan();
        AiProviderResult providerResult;
        try {
            // 外部 HTTP 调用不得位于数据库事务内，也不得在 Client 内自动重试。
            providerResult = providerClient.execute(plan.providerRequest());
        } catch (AiProviderException exception) {
            return transactionService.completeProviderFailure(plan, exception);
        } catch (RuntimeException exception) {
            // 未分类异常的外部执行结果可能不确定，记录 UNKNOWN 后交由人工判断。
            transactionService.completeUnexpectedFailure(plan);
            throw new AnalysisPersistenceException("Unexpected AI execution failure");
        }

        try {
            ValidatedAnalysisOutput<?> validated = process(plan.definition(), providerResult);
            JsonNode resultJson = validated.resultJson();
            return transactionService.completeSuccess(
                    plan,
                    providerResult,
                    resultJson,
                    resultJson.path("relevanceScore").intValue(),
                    resultJson.path("summary").textValue());
        } catch (AnalysisOutputProcessingException exception) {
            return transactionService.completeOutputFailure(
                    plan, providerResult, exception.code(), exception.getMessage());
        } catch (AnalysisDefinitionValidationException exception) {
            return transactionService.completeOutputFailure(
                    plan, providerResult, exception.code(), exception.getMessage());
        }
    }

    /** 按当前 Owner 查询 Analysis，普通 GET 不触发 Provider。 */
    public InformationAnalysisView get(long analysisId) {
        long userId = currentUserProvider.requireCurrentUser().id();
        return transactionService.getOwned(userId, analysisId);
    }

    /** 捕获 Definition 输出泛型并复用严格 Output Processor。 */
    private ValidatedAnalysisOutput<?> process(
            AnalysisDefinition<?, ?> definition, AiProviderResult providerResult) {
        return processCaptured(definition, providerResult);
    }

    private <O> ValidatedAnalysisOutput<O> processCaptured(
            AnalysisDefinition<?, O> definition, AiProviderResult providerResult) {
        return outputProcessor.process(definition, providerResult);
    }
}
