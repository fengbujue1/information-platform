package com.informationplatform.hub.analysis.schedule.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

/** 创建或完整更新每日 Analysis Schedule 的请求。 */
public record SaveAnalysisScheduleRequest(
        /** 用户可识别名称，同一 Owner 内唯一。 */
        @NotBlank @Size(max = 255) String name,
        /** 当前 Owner 下的 Prompt Profile 主键。 */
        @NotNull @Positive Long promptProfileId,
        /** 用户墙上时间；省略时使用 02:00。 */
        LocalTime localTime,
        /** IANA 时区；省略时使用账号时区。 */
        @Size(max = 64) String timezone,
        /** 回看窗口天数；省略时使用 3，范围 1..14。 */
        Integer windowDays,
        /** Candidate Limit；省略时使用 20，范围 1..50。 */
        Integer maxCandidates,
        /** Estimated Token Budget；省略时使用 75000，范围 1..200000。 */
        Long maxEstimatedTokens,
        /** 仅创建时生效；省略时默认 false。 */
        Boolean enabled) {
}
