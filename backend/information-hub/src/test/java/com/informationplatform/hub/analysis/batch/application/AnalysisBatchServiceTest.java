package com.informationplatform.hub.analysis.batch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.informationplatform.hub.analysis.batch.domain.AnalysisBatchProgress;
import com.informationplatform.hub.analysis.batch.domain.AnalysisBatchView;
import com.informationplatform.hub.analysis.preview.application.PreviewTokenService;
import com.informationplatform.hub.analysis.preview.domain.PreviewTokenPayload;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.identity.domain.AuthenticatedUser;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisBatchServiceTest {

    @Test
    void confirmsTokenForSessionOwnerAndReusesExistingBatch() {
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        PreviewTokenService tokens = mock(PreviewTokenService.class);
        AnalysisBatchTransactionService transactions =
                mock(AnalysisBatchTransactionService.class);
        PreviewTokenPayload payload = payload();
        AnalysisBatchView existing = view();
        when(users.requireCurrentUser())
                .thenReturn(new AuthenticatedUser(7, "owner", "Owner", "Asia/Shanghai"));
        when(tokens.verifyForOwner("token", 7)).thenReturn(payload);
        when(transactions.findManual(7, "request-1")).thenReturn(existing);

        AnalysisBatchService service = new AnalysisBatchService(users, tokens, transactions);

        assertThat(service.confirm("token")).isSameAs(existing);
        verify(tokens).verifyForOwner("token", 7);
        verify(transactions).findManual(7, "request-1");
    }

    @Test
    void appliesStableListLimit() {
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        PreviewTokenService tokens = mock(PreviewTokenService.class);
        AnalysisBatchTransactionService transactions =
                mock(AnalysisBatchTransactionService.class);
        when(users.requireCurrentUser())
                .thenReturn(new AuthenticatedUser(7, "owner", "Owner", "Asia/Shanghai"));
        when(transactions.listOwned(7, 20)).thenReturn(List.of(view()));
        AnalysisBatchService service = new AnalysisBatchService(users, tokens, transactions);

        assertThat(service.list(null)).hasSize(1);
        verify(transactions).listOwned(7, 20);
    }

    private PreviewTokenPayload payload() {
        Instant now = Instant.parse("2026-08-05T06:00:00Z");
        return new PreviewTokenPayload(
                1, 7, 11, 12, "JOB_USER_RELEVANCE", 1,
                now.minusSeconds(259_200), now, 3, 20, 75_000,
                1, 1, 0, 1, 0, 0,
                100, 1000, 1100, "UTF8_BYTES_DIV3_MARGIN20_V1",
                "a".repeat(64), "request-1", now, now.plusSeconds(600));
    }

    private AnalysisBatchView view() {
        return new AnalysisBatchView(
                31, "MANUAL", 11, 12, "JOB", "JOB_USER_RELEVANCE", 1,
                "FIRST_INGESTED", 3,
                null, null, 20, 75_000, 1, 1, 0, 1, 0, 0,
                100L, 1000L, 1100L, "UTF8_BYTES_DIV3_MARGIN20_V1",
                "PENDING", null, null, null, null, null,
                new AnalysisBatchProgress(1, 1, 0, 0, 0, 0, 0, 0, 0,
                        null, null, null),
                List.of());
    }
}
