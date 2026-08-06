package com.informationplatform.hub.analysis.usage.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.informationplatform.hub.analysis.usage.domain.AnalysisUsageSummary;
import com.informationplatform.hub.analysis.usage.infrastructure.persistence.AnalysisUsageMapper;
import com.informationplatform.hub.analysis.usage.infrastructure.persistence.UserUsageAggregateRow;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.identity.domain.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalysisUsageServiceTest {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private AnalysisUsageMapper usageMapper;

    @Test
    void aggregatesTodayMonthAndAllTimeByOwnerTimezone() {
        Instant now = Instant.parse("2026-08-05T16:30:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        when(currentUserProvider.requireCurrentUser()).thenReturn(
                new AuthenticatedUser(7, "owner", "Owner", "Asia/Shanghai"));

        LocalDateTime todayStart = LocalDateTime.parse("2026-08-05T16:00:00");
        LocalDateTime monthStart = LocalDateTime.parse("2026-07-31T16:00:00");
        LocalDateTime periodEnd = LocalDateTime.parse("2026-08-05T16:30:00");
        when(usageMapper.aggregateUserUsageBetween(7, todayStart, periodEnd))
                .thenReturn(row(2, 1, 1, 100L, 50L, 150L));
        when(usageMapper.aggregateUserUsageBetween(7, monthStart, periodEnd))
                .thenReturn(row(5, 4, 1, 500L, 200L, 700L));
        when(usageMapper.aggregateUserUsage(7))
                .thenReturn(row(9, 7, 2, 900L, 400L, 1300L));

        AnalysisUsageSummary result = new AnalysisUsageService(
                currentUserProvider, usageMapper, clock).getCurrentUserUsage();

        assertEquals("Asia/Shanghai", result.timezone());
        assertEquals(Instant.parse("2026-08-05T16:00:00Z"), result.today().periodStart());
        assertEquals(now, result.today().periodEnd());
        assertEquals(2, result.today().invocationCount());
        assertEquals(150L, result.today().actualTotalTokens());
        assertEquals(Instant.parse("2026-07-31T16:00:00Z"), result.month().periodStart());
        assertEquals(700L, result.month().actualTotalTokens());
        assertNull(result.allTime().periodStart());
        assertNull(result.allTime().periodEnd());
        assertEquals(1300L, result.allTime().actualTotalTokens());

        verify(usageMapper).aggregateUserUsageBetween(7, todayStart, periodEnd);
        verify(usageMapper).aggregateUserUsageBetween(7, monthStart, periodEnd);
        verify(usageMapper).aggregateUserUsage(7);
    }

    @Test
    void keepsActualTokensNullWhenProviderUsageIsUnavailable() {
        Instant now = Instant.parse("2026-08-05T08:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        when(currentUserProvider.requireCurrentUser()).thenReturn(
                new AuthenticatedUser(9, "owner", null, "UTC"));
        when(usageMapper.aggregateUserUsageBetween(
                9,
                LocalDateTime.parse("2026-08-05T00:00:00"),
                LocalDateTime.parse("2026-08-05T08:00:00")))
                .thenReturn(row(1, 0, 1, null, null, null));
        when(usageMapper.aggregateUserUsageBetween(
                9,
                LocalDateTime.parse("2026-08-01T00:00:00"),
                LocalDateTime.parse("2026-08-05T08:00:00")))
                .thenReturn(null);
        when(usageMapper.aggregateUserUsage(9)).thenReturn(null);

        AnalysisUsageSummary result = new AnalysisUsageService(
                currentUserProvider, usageMapper, clock).getCurrentUserUsage();

        assertEquals(1, result.today().unavailableInvocationCount());
        assertNull(result.today().actualTotalTokens());
        assertEquals(0, result.month().invocationCount());
        assertNull(result.month().actualTotalTokens());
        assertEquals(0, result.allTime().invocationCount());
    }

    private UserUsageAggregateRow row(
            long invocationCount,
            long reportedCount,
            long unavailableCount,
            Long inputTokens,
            Long outputTokens,
            Long totalTokens) {
        UserUsageAggregateRow row = new UserUsageAggregateRow();
        row.setInvocationCount(invocationCount);
        row.setReportedInvocationCount(reportedCount);
        row.setUnavailableInvocationCount(unavailableCount);
        row.setInputTokens(inputTokens);
        row.setOutputTokens(outputTokens);
        row.setTotalTokens(totalTokens);
        return row;
    }
}
