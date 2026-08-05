package com.informationplatform.hub.analysis.processing.api;

import com.informationplatform.hub.analysis.processing.api.dto.ExecuteInformationAnalysisRequest;
import com.informationplatform.hub.analysis.processing.api.dto.InformationAnalysisResponse;
import com.informationplatform.hub.analysis.processing.application.InformationAnalysisService;
import com.informationplatform.hub.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供认证用户的单条 Information Analysis 执行与查询 API。 */
@Validated
@RestController
@RequestMapping("/api/v1/ai/analyses")
public class InformationAnalysisController {

    /** 单条 Analysis 编排服务。 */
    private final InformationAnalysisService analysisService;

    public InformationAnalysisController(InformationAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /** 执行、显式重试或复用同一逻辑身份的 Analysis。 */
    @PostMapping
    public ApiResponse<InformationAnalysisResponse> execute(
            @Valid @RequestBody ExecuteInformationAnalysisRequest request) {
        return ApiResponse.success(
                "INFORMATION_ANALYSIS_RESOLVED",
                InformationAnalysisResponse.from(analysisService.execute(
                        request.informationId(),
                        request.snapshotId(),
                        request.promptProfileId(),
                        request.shouldRetryFailed())));
    }

    /** 按 Owner 查询 Analysis；该 GET 不触发 Provider。 */
    @GetMapping("/{analysisId}")
    public ApiResponse<InformationAnalysisResponse> get(
            @PathVariable @Positive long analysisId) {
        return ApiResponse.success(
                "INFORMATION_ANALYSIS_FOUND",
                InformationAnalysisResponse.from(analysisService.get(analysisId)));
    }
}
