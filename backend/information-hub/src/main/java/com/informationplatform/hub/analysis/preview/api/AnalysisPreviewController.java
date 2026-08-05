package com.informationplatform.hub.analysis.preview.api;

import com.informationplatform.hub.analysis.preview.api.dto.AnalysisPreviewResponse;
import com.informationplatform.hub.analysis.preview.api.dto.CreateAnalysisPreviewRequest;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewService;
import com.informationplatform.hub.common.api.ApiResponse;
import jakarta.validation.Valid;
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

    public AnalysisPreviewController(AnalysisPreviewService previewService) {
        this.previewService = previewService;
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
