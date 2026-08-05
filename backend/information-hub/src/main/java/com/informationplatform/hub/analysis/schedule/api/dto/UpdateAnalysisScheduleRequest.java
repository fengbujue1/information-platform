package com.informationplatform.hub.analysis.schedule.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

/** 完整更新每日 Analysis Schedule 配置的请求，不隐式改变启停状态。 */
public record UpdateAnalysisScheduleRequest(
        /** 用户可识别名称，同一 Owner 内唯一。 */
        @NotBlank @Size(max = 255) String name,
        /** 当前 Owner 下的 Prompt Profile 主键。 */
        @NotNull @Positive Long promptProfileId,
        /** 用户墙上时间；省略时重置为 02:00。 */
        LocalTime localTime,
        /** IANA 时区；省略时重置为账号时区。 */
        @Size(max = 64) String timezone,
        /** 回看窗口天数；省略时重置为 3。 */
        Integer windowDays,
        /** Candidate Limit；省略时重置为 20。 */
        Integer maxCandidates,
        /** Estimated Token Budget；省略时重置为 75000。 */
        Long maxEstimatedTokens) {
}
