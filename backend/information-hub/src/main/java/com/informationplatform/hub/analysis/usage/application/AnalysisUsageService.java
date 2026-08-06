package com.informationplatform.hub.analysis.usage.application;

import com.informationplatform.hub.analysis.usage.domain.AnalysisUsagePeriod;
import com.informationplatform.hub.analysis.usage.domain.AnalysisUsageSummary;
import com.informationplatform.hub.analysis.usage.infrastructure.persistence.AnalysisUsageMapper;
import com.informationplatform.hub.analysis.usage.infrastructure.persistence.UserUsageAggregateRow;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.identity.domain.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 从 Invocation 唯一事实源按当前 Owner 聚合用户 Actual Token Usage。 */
@Service
public class AnalysisUsageService {

    /** 当前 Session Owner 与账号时区。 */
    private final CurrentUserProvider currentUserProvider;

    /** Provider Invocation Actual Usage 持久化查询。 */
    private final AnalysisUsageMapper usageMapper;

    /** 可替换 UTC 时钟，保证自然日和自然月边界可测试。 */
    private final Clock clock;

    public AnalysisUsageService(
            CurrentUserProvider currentUserProvider,
            AnalysisUsageMapper usageMapper,
            Clock clock) {
        this.currentUserProvider = currentUserProvider;
        this.usageMapper = usageMapper;
        this.clock = clock;
    }

    /**
     * 按账号 IANA 时区生成今日和本月的 UTC 半开区间，并返回 Owner 隔离的实时聚合。
     *
     * <p>Actual Token 仅累加 Provider 明确报告的字段；不会用 Estimate 或其他字段补算。
     */
    @Transactional(readOnly = true)
    public AnalysisUsageSummary getCurrentUserUsage() {
        AuthenticatedUser user = currentUserProvider.requireCurrentUser();
        ZoneId zoneId = ZoneId.of(user.timezone());
        Instant now = clock.instant();
        LocalDate localToday = now.atZone(zoneId).toLocalDate();
        Instant todayStart = localToday.atStartOfDay(zoneId).toInstant();
        Instant monthStart = localToday.withDayOfMonth(1).atStartOfDay(zoneId).toInstant();

        UserUsageAggregateRow today = usageMapper.aggregateUserUsageBetween(
                user.id(), toUtcLocalDateTime(todayStart), toUtcLocalDateTime(now));
        UserUsageAggregateRow month = usageMapper.aggregateUserUsageBetween(
                user.id(), toUtcLocalDateTime(monthStart), toUtcLocalDateTime(now));
        UserUsageAggregateRow allTime = usageMapper.aggregateUserUsage(user.id());

        return new AnalysisUsageSummary(
                zoneId.getId(),
                toPeriod(todayStart, now, today),
                toPeriod(monthStart, now, month),
                toPeriod(null, null, allTime));
    }

    private AnalysisUsagePeriod toPeriod(
            Instant periodStart,
            Instant periodEnd,
            UserUsageAggregateRow row) {
        return new AnalysisUsagePeriod(
                periodStart,
                periodEnd,
                valueOrZero(row == null ? null : row.getInvocationCount()),
                valueOrZero(row == null ? null : row.getReportedInvocationCount()),
                valueOrZero(row == null ? null : row.getUnavailableInvocationCount()),
                row == null ? null : row.getInputTokens(),
                row == null ? null : row.getOutputTokens(),
                row == null ? null : row.getTotalTokens());
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private LocalDateTime toUtcLocalDateTime(Instant value) {
        return LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }
}
