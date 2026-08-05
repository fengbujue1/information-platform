package com.informationplatform.hub.analysis.schedule.api.dto;

import jakarta.validation.constraints.NotNull;

/** 启用或停用每日 Analysis Schedule 的请求。 */
public record UpdateAnalysisScheduleStatusRequest(
        /** 是否允许 Dispatcher 触发该 Schedule。 */
        @NotNull Boolean enabled) {
}
