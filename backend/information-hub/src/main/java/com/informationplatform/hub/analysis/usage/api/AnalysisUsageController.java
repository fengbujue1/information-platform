package com.informationplatform.hub.analysis.usage.api;

import com.informationplatform.hub.analysis.usage.api.dto.AnalysisUsageResponse;
import com.informationplatform.hub.analysis.usage.application.AnalysisUsageService;
import com.informationplatform.hub.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供当前认证 Owner 的用户维度 Actual Token Usage 查询。 */
@RestController
@RequestMapping("/api/v1/ai/usage")
public class AnalysisUsageController {

    /** Owner 隔离的 Invocation Usage 聚合服务。 */
    private final AnalysisUsageService usageService;

    public AnalysisUsageController(AnalysisUsageService usageService) {
        this.usageService = usageService;
    }

    /** 返回按账号时区计算的今日、本月和累计 Provider Actual Usage。 */
    @GetMapping
    public ApiResponse<AnalysisUsageResponse> get() {
        return ApiResponse.success(
                "ANALYSIS_USAGE_FOUND",
                AnalysisUsageResponse.from(usageService.getCurrentUserUsage()));
    }
}
