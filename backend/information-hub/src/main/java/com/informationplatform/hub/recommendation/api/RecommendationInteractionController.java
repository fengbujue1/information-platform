package com.informationplatform.hub.recommendation.api;

import com.informationplatform.hub.common.api.ApiResponse;
import com.informationplatform.hub.recommendation.api.dto.PutJobDispositionRequest;
import com.informationplatform.hub.recommendation.api.dto.PutRecommendationFeedbackRequest;
import com.informationplatform.hub.recommendation.api.dto.RecommendationInteractionAttributionRequest;
import com.informationplatform.hub.recommendation.api.dto.RecommendationInteractionResponse;
import com.informationplatform.hub.recommendation.application.RecommendationInteractionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供当前 Owner 的 View、Feedback 与 JOB disposition 写接口。 */
@Validated
@RestController
@RequestMapping("/api/v1/recommendation/interactions")
public class RecommendationInteractionController {

    /** Recommendation Interaction 应用服务。 */
    private final RecommendationInteractionService interactionService;

    public RecommendationInteractionController(
            RecommendationInteractionService interactionService) {
        this.interactionService = interactionService;
    }

    /** 累计一次查看；请求体可省略，也可提供 Recommendation Item 归因。 */
    @PostMapping("/{informationId}/view")
    public ApiResponse<RecommendationInteractionResponse> recordView(
            @PathVariable @Positive long informationId,
            @Valid @RequestBody(required = false)
                    RecommendationInteractionAttributionRequest request) {
        Long recommendationItemId = request == null ? null : request.recommendationItemId();
        return ApiResponse.success(
                "RECOMMENDATION_INTERACTION_VIEW_RECORDED",
                RecommendationInteractionResponse.from(
                        interactionService.recordView(informationId, recommendationItemId)));
    }

    /** 完整替换通用 Feedback，不隐式修改 JOB disposition。 */
    @PutMapping("/{informationId}/feedback")
    public ApiResponse<RecommendationInteractionResponse> replaceFeedback(
            @PathVariable @Positive long informationId,
            @Valid @RequestBody PutRecommendationFeedbackRequest request) {
        return ApiResponse.success(
                "RECOMMENDATION_FEEDBACK_UPDATED",
                RecommendationInteractionResponse.from(interactionService.replaceFeedback(
                        informationId,
                        request.feedbackState(),
                        request.recommendationItemId())));
    }

    /** 完整替换 JOB disposition，不隐式修改通用 Feedback。 */
    @PutMapping("/{informationId}/job-disposition")
    public ApiResponse<RecommendationInteractionResponse> replaceJobDisposition(
            @PathVariable @Positive long informationId,
            @Valid @RequestBody PutJobDispositionRequest request) {
        return ApiResponse.success(
                "RECOMMENDATION_JOB_DISPOSITION_UPDATED",
                RecommendationInteractionResponse.from(
                        interactionService.replaceJobDisposition(
                                informationId,
                                request.jobDisposition(),
                                request.recommendationItemId())));
    }
}
