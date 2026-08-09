package com.informationplatform.hub.analysis.processing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinition;
import com.informationplatform.hub.analysis.processing.domain.AnalysisExecutionPlan;
import com.informationplatform.hub.analysis.processing.domain.AnalysisPreparation;
import com.informationplatform.hub.analysis.processing.domain.InformationAnalysisView;
import com.informationplatform.hub.analysis.processing.domain.ValidatedAnalysisOutput;
import com.informationplatform.hub.analysis.provider.application.AiProviderClient;
import com.informationplatform.hub.analysis.provider.domain.AiProviderErrorType;
import com.informationplatform.hub.analysis.provider.domain.AiProviderException;
import com.informationplatform.hub.analysis.provider.domain.AiProviderMessage;
import com.informationplatform.hub.analysis.provider.domain.AiProviderMessageRole;
import com.informationplatform.hub.analysis.provider.domain.AiProviderRequest;
import com.informationplatform.hub.analysis.provider.domain.AiProviderResult;
import com.informationplatform.hub.analysis.provider.domain.AiProviderRetryDisposition;
import com.informationplatform.hub.analysis.provider.domain.AiProviderUsage;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.identity.domain.AuthenticatedUser;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class InformationAnalysisServiceTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final InformationAnalysisTransactionService transactions =
            mock(InformationAnalysisTransactionService.class);
    private final AiProviderClient provider = mock(AiProviderClient.class);
    private final AnalysisOutputProcessor outputProcessor = mock(AnalysisOutputProcessor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private InformationAnalysisService service;

    @BeforeEach
    void setUp() {
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new AuthenticatedUser(7, "owner", "Owner", "Asia/Shanghai"));
        service = new InformationAnalysisService(
                currentUserProvider, transactions, provider, outputProcessor);
    }

    @Test
    void reusesSucceededIdentityWithoutCallingProvider() {
        InformationAnalysisView reused = view("SUCCEEDED");
        when(transactions.prepare(7, 11, null, 21, false))
                .thenReturn(AnalysisPreparation.reused(reused));

        InformationAnalysisView result = service.execute(11, null, 21, false);

        assertThat(result).isSameAs(reused);
        verify(provider, never()).execute(any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void completesValidatedProviderResult(CapturedOutput output) {
        AnalysisDefinition definition = mock(AnalysisDefinition.class);
        AiProviderRequest request = request();
        AnalysisExecutionPlan plan = new AnalysisExecutionPlan(31, 41, 7, definition, request);
        AiProviderResult providerResult = providerResult();
        ObjectNode json = objectMapper.createObjectNode();
        json.put("relevanceScore", 82);
        json.put("summary", "相关");
        ValidatedAnalysisOutput<Object> validated =
                new ValidatedAnalysisOutput<>(json, new Object(), providerResult);
        InformationAnalysisView succeeded = view("SUCCEEDED");

        when(transactions.prepare(7, 11, 12L, 21, false))
                .thenReturn(AnalysisPreparation.execute(plan));
        when(provider.execute(request)).thenReturn(providerResult);
        when(outputProcessor.process(any(), any())).thenReturn((ValidatedAnalysisOutput) validated);
        when(transactions.completeSuccess(plan, providerResult, json, 82, "相关"))
                .thenReturn(succeeded);

        assertThat(service.execute(11, 12L, 21, false)).isSameAs(succeeded);
        verify(transactions).completeSuccess(plan, providerResult, json, 82, "相关");
        assertThat(output).contains(
                "Single analysis requested, informationId=11, snapshotId=12, promptProfileId=21",
                "AI analysis started, analysisId=31",
                "AI analysis completed, analysisId=31, provider=FAKE, model=fake-model",
                "inputTokens=10, outputTokens=2");
        assertThat(output).doesNotContain("相关", "test");
    }

    @Test
    void persistsAmbiguousProviderFailureWithoutAutomaticRetry(CapturedOutput output) {
        AnalysisExecutionPlan plan = new AnalysisExecutionPlan(
                31, 41, 7, mock(AnalysisDefinition.class), request());
        AiProviderException timeout = new AiProviderException(
                AiProviderErrorType.TIMEOUT,
                AiProviderRetryDisposition.AMBIGUOUS_DO_NOT_AUTO_RETRY,
                "AI Provider request timed out",
                null,
                null,
                AiProviderUsage.unavailable(),
                30_000,
                null);
        InformationAnalysisView failed = view("FAILED");
        when(transactions.prepare(7, 11, null, 21, false))
                .thenReturn(AnalysisPreparation.execute(plan));
        when(provider.execute(plan.providerRequest())).thenThrow(timeout);
        when(transactions.completeProviderFailure(plan, timeout)).thenReturn(failed);

        assertThat(service.execute(11, null, 21, false)).isSameAs(failed);
        verify(provider).execute(plan.providerRequest());
        verify(transactions).completeProviderFailure(plan, timeout);
        assertThat(output).contains(
                "AI analysis failed, analysisId=31",
                "errorType=TIMEOUT",
                "com.informationplatform.hub.analysis.provider.domain.AiProviderException: AI Provider request timed out");
    }

    private AiProviderRequest request() {
        return new AiProviderRequest(
                List.of(new AiProviderMessage(AiProviderMessageRole.USER, "test")), 1000);
    }

    private AiProviderResult providerResult() {
        return new AiProviderResult(
                "FAKE", "fake-model", "request-1", "{}", "stop", 3,
                AiProviderUsage.reported(10L, 2L, 12L, null, null));
    }

    private InformationAnalysisView view(String status) {
        LocalDateTime time = LocalDateTime.of(2026, 8, 5, 10, 0);
        return new InformationAnalysisView(
                31, 11, 12, "JOB", "JOB_USER_RELEVANCE", 1,
                "USER_RELEVANCE", 21, 22, status, null, null, null,
                100L, 1000L, 1100L, "UTF8_BYTES_DIV3_MARGIN20_V1",
                null, null, time, time, time, time, List.of());
    }
}
