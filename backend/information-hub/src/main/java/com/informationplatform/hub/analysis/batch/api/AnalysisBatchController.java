package com.informationplatform.hub.analysis.batch.api;

import com.informationplatform.hub.analysis.batch.api.dto.AnalysisBatchProgressResponse;
import com.informationplatform.hub.analysis.batch.api.dto.AnalysisBatchResponse;
import com.informationplatform.hub.analysis.batch.api.dto.ConfirmAnalysisBatchRequest;
import com.informationplatform.hub.analysis.batch.application.AnalysisBatchService;
import com.informationplatform.hub.common.api.ApiResponse;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 提供 Manual Confirm 和 Owner 安全 Batch 查询 API。 */
@Validated
@RestController
@RequestMapping("/api/v1/ai/analysis-batches")
public class AnalysisBatchController {

    /** Batch 应用服务。 */
    private final AnalysisBatchService batchService;

    public AnalysisBatchController(AnalysisBatchService batchService) {
        this.batchService = batchService;
    }

    /** 原子冻结 Batch/Items 后快速返回，完整批次由后台 Worker 异步执行。 */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<AnalysisBatchResponse>> confirm(
            @Valid @RequestBody ConfirmAnalysisBatchRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(
                        "ANALYSIS_BATCH_ACCEPTED",
                        AnalysisBatchResponse.from(
                                batchService.confirm(request.previewToken()))));
    }

    /** 返回当前 Owner 最近的 Batch。 */
    @GetMapping
    public ApiResponse<List<AnalysisBatchResponse>> list(
            @RequestParam(required = false) @Min(1) @Max(100) Integer limit) {
        return ApiResponse.success(
                "ANALYSIS_BATCHES_FOUND",
                batchService.list(limit).stream().map(AnalysisBatchResponse::from).toList());
    }

    /** 返回当前 Owner 的 Batch 及全部冻结 Items。 */
    @GetMapping("/{batchId}")
    public ApiResponse<AnalysisBatchResponse> detail(
            @PathVariable @Positive long batchId) {
        return ApiResponse.success(
                "ANALYSIS_BATCH_FOUND",
                AnalysisBatchResponse.from(batchService.get(batchId)));
    }

    /** 返回当前 Owner 的实时 Item 进度和 Actual Usage。 */
    @GetMapping("/{batchId}/progress")
    public ApiResponse<AnalysisBatchProgressResponse> progress(
            @PathVariable @Positive long batchId) {
        return ApiResponse.success(
                "ANALYSIS_BATCH_PROGRESS_FOUND",
                AnalysisBatchProgressResponse.from(batchService.progress(batchId)));
    }
}
