package com.informationplatform.hub.analysis.schedule.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

/** 按 IANA 时区和用户墙上时间计算每日未来计划点。 */
@Component
public class AnalysisScheduleTimeCalculator {

    /**
     * 返回严格晚于基准时刻的下一次 UTC 计划点。
     *
     * <p>DST gap 向后移动到首个有效时间；overlap 固定选择较早 offset，保证每天最多一次。
     */
    public Instant nextRunAfter(LocalTime localTime, ZoneId zoneId, Instant after) {
        LocalDate date = after.atZone(zoneId).toLocalDate();
        for (int offsetDays = 0; offsetDays < 3; offsetDays++) {
            LocalDateTime localDateTime = LocalDateTime.of(
                    date.plusDays(offsetDays), localTime);
            ZonedDateTime candidate = resolve(localDateTime, zoneId);
            if (candidate.toInstant().isAfter(after)) {
                return candidate.toInstant().truncatedTo(ChronoUnit.MILLIS);
            }
        }
        throw new IllegalStateException("Unable to calculate the next daily schedule time");
    }

    private ZonedDateTime resolve(LocalDateTime localDateTime, ZoneId zoneId) {
        ZoneRules rules = zoneId.getRules();
        List<ZoneOffset> offsets = rules.getValidOffsets(localDateTime);
        if (offsets.size() == 1) {
            return ZonedDateTime.ofLocal(localDateTime, zoneId, offsets.get(0));
        }
        if (offsets.size() == 2) {
            return ZonedDateTime.ofLocal(localDateTime, zoneId, offsets.get(0));
        }
        ZoneOffsetTransition transition = rules.getTransition(localDateTime);
        if (transition == null) {
            throw new IllegalArgumentException("Timezone transition could not be resolved");
        }
        return transition.getDateTimeAfter().atZone(zoneId);
    }
}
