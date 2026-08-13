package com.informationplatform.hub.analysis.preview.api;

import com.informationplatform.hub.analysis.preview.api.dto.AnalysisPreviewLimitsResponse;
import com.informationplatform.hub.analysis.preview.api.dto.AnalysisPreviewResponse;
import com.informationplatform.hub.analysis.preview.api.dto.CreateAnalysisPreviewRequest;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewLimitPolicy;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewService;
import com.informationplatform.hub.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供认证用户的无 Provider Manual Analysis Preview。 */
@RestController
@RequestMapping("/api/v1/ai/analysis-batches")
public class AnalysisPreviewController {

    /** Preview 编排服务。 */
    private final AnalysisPreviewService previewService;
    /** 手动分析和定时分析共享的限制策略。 */
    private final AnalysisPreviewLimitPolicy limitPolicy;

    public AnalysisPreviewController(
            AnalysisPreviewService previewService, AnalysisPreviewLimitPolicy limitPolicy) {
        this.previewService = previewService;
        this.limitPolicy = limitPolicy;
    }

    /** 返回当前分析入口的非敏感默认值和平台上限。 */
    @GetMapping("/limits")
    public ApiResponse<AnalysisPreviewLimitsResponse> limits() {
        return ApiResponse.success(
                "ANALYSIS_PREVIEW_LIMITS_RETRIEVED",
                AnalysisPreviewLimitsResponse.from(limitPolicy.metadata()));
    }

    /** 返回候选统计、Estimate 与 10 分钟 HMAC Token，不创建任何业务记录。 */
    @PostMapping("/preview")
    public ApiResponse<AnalysisPreviewResponse> preview(
            @Valid @RequestBody CreateAnalysisPreviewRequest request) {
        return ApiResponse.success(
                "ANALYSIS_PREVIEW_CREATED",
                AnalysisPreviewResponse.from(previewService.preview(
                        request.promptProfileId(),
                        request.windowDays(),
                        request.maxCandidates(),
                        request.maxEstimatedTokens())));
    }
}
