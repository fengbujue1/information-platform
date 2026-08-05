package com.informationplatform.hub.analysis.schedule.api;

import com.informationplatform.hub.analysis.preview.api.dto.AnalysisPreviewResponse;
import com.informationplatform.hub.analysis.schedule.api.dto.AnalysisScheduleResponse;
import com.informationplatform.hub.analysis.schedule.api.dto.SaveAnalysisScheduleRequest;
import com.informationplatform.hub.analysis.schedule.api.dto.UpdateAnalysisScheduleRequest;
import com.informationplatform.hub.analysis.schedule.api.dto.UpdateAnalysisScheduleStatusRequest;
import com.informationplatform.hub.analysis.schedule.application.AnalysisScheduleService;
import com.informationplatform.hub.analysis.schedule.domain.AnalysisScheduleCommand;
import com.informationplatform.hub.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供账号级每日 Analysis Schedule 配置与无副作用 Preview API。 */
@Validated
@RestController
@RequestMapping("/api/v1/ai/analysis-schedules")
public class AnalysisScheduleController {

    /** Schedule 应用服务。 */
    private final AnalysisScheduleService scheduleService;

    public AnalysisScheduleController(AnalysisScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /** 返回当前 Owner 的全部 Schedule。 */
    @GetMapping
    public ApiResponse<List<AnalysisScheduleResponse>> list() {
        return ApiResponse.success(
                "ANALYSIS_SCHEDULES_FOUND",
                scheduleService.list().stream()
                        .map(AnalysisScheduleResponse::from)
                        .toList());
    }

    /** 创建 Schedule；enabled 省略时默认关闭。 */
    @PostMapping
    public ApiResponse<AnalysisScheduleResponse> create(
            @Valid @RequestBody SaveAnalysisScheduleRequest request) {
        return ApiResponse.success(
                "ANALYSIS_SCHEDULE_CREATED",
                AnalysisScheduleResponse.from(scheduleService.create(
                        toCommand(request), request.enabled())));
    }

    /** 返回当前 Owner 下的指定 Schedule。 */
    @GetMapping("/{scheduleId}")
    public ApiResponse<AnalysisScheduleResponse> get(
            @PathVariable @Positive long scheduleId) {
        return ApiResponse.success(
                "ANALYSIS_SCHEDULE_FOUND",
                AnalysisScheduleResponse.from(scheduleService.get(scheduleId)));
    }

    /** 完整更新 Schedule 配置，保持现有启停状态。 */
    @PutMapping("/{scheduleId}")
    public ApiResponse<AnalysisScheduleResponse> update(
            @PathVariable @Positive long scheduleId,
            @Valid @RequestBody UpdateAnalysisScheduleRequest request) {
        return ApiResponse.success(
                "ANALYSIS_SCHEDULE_UPDATED",
                AnalysisScheduleResponse.from(
                        scheduleService.update(scheduleId, toCommand(request))));
    }

    /** 启用或停用 Schedule。 */
    @PutMapping("/{scheduleId}/status")
    public ApiResponse<AnalysisScheduleResponse> updateStatus(
            @PathVariable @Positive long scheduleId,
            @Valid @RequestBody UpdateAnalysisScheduleStatusRequest request) {
        return ApiResponse.success(
                "ANALYSIS_SCHEDULE_STATUS_UPDATED",
                AnalysisScheduleResponse.from(
                        scheduleService.updateStatus(scheduleId, request.enabled())));
    }

    /** 使用当前持久化配置执行 Preview，不创建 Batch 或调用 Provider。 */
    @PostMapping("/{scheduleId}/preview")
    public ApiResponse<AnalysisPreviewResponse> preview(
            @PathVariable @Positive long scheduleId) {
        return ApiResponse.success(
                "ANALYSIS_SCHEDULE_PREVIEW_CREATED",
                AnalysisPreviewResponse.from(scheduleService.preview(scheduleId)));
    }

    private AnalysisScheduleCommand toCommand(SaveAnalysisScheduleRequest request) {
        return new AnalysisScheduleCommand(
                request.name(),
                request.promptProfileId(),
                request.localTime(),
                request.timezone(),
                request.windowDays(),
                request.maxCandidates(),
                request.maxEstimatedTokens());
    }

    private AnalysisScheduleCommand toCommand(UpdateAnalysisScheduleRequest request) {
        return new AnalysisScheduleCommand(
                request.name(),
                request.promptProfileId(),
                request.localTime(),
                request.timezone(),
                request.windowDays(),
                request.maxCandidates(),
                request.maxEstimatedTokens());
    }
}
