package com.informationplatform.hub.recommendation.api;

import com.informationplatform.hub.common.api.ApiResponse;
import com.informationplatform.hub.recommendation.api.dto.ManualRecommendationRefreshResponse;
import com.informationplatform.hub.recommendation.api.dto.RecommendationRunResponse;
import com.informationplatform.hub.recommendation.run.application.RecommendationRunService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 提供 Manual Refresh 与 Owner/Information Type 隔离的 Recommendation Run API。 */
@Validated
@RestController
@RequestMapping("/api/v1/recommendations/{informationType}")
public class RecommendationRunController {

    /** Recommendation Run 应用服务。 */
    private final RecommendationRunService runService;

    public RecommendationRunController(RecommendationRunService runService) {
        this.runService = runService;
    }

    /** 冻结当前 Profile 和 Prompt Active Version，创建 PENDING Run 并返回 202。 */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<ManualRecommendationRefreshResponse>> refresh(
            @PathVariable String informationType) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(
                        "RECOMMENDATION_RUN_ACCEPTED",
                        ManualRecommendationRefreshResponse.from(
                                runService.refresh(informationType))));
    }

    /** 返回当前 Owner/Type 最近 Run。 */
    @GetMapping("/runs")
    public ApiResponse<List<RecommendationRunResponse>> list(
            @PathVariable String informationType,
            @RequestParam(required = false) @Min(1) @Max(100) Integer limit) {
        return ApiResponse.success(
                "RECOMMENDATION_RUNS_FOUND",
                runService.list(informationType, limit).stream()
                        .map(RecommendationRunResponse::from)
                        .toList());
    }

    /** 返回当前 Owner/Type 的 Run 状态和冻结输入身份。 */
    @GetMapping("/runs/{runId}")
    public ApiResponse<RecommendationRunResponse> detail(
            @PathVariable String informationType,
            @PathVariable @Positive long runId) {
        return ApiResponse.success(
                "RECOMMENDATION_RUN_FOUND",
                RecommendationRunResponse.from(runService.get(informationType, runId)));
    }
}
