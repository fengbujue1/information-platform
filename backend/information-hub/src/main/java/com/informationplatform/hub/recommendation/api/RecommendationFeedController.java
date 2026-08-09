package com.informationplatform.hub.recommendation.api;

import com.informationplatform.hub.common.api.ApiResponse;
import com.informationplatform.hub.recommendation.api.dto.RecommendationFeedResponse;
import com.informationplatform.hub.recommendation.feed.application.RecommendationFeedService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 提供 Owner/Information Type 隔离的预计算 Recommendation Feed。 */
@RestController
@RequestMapping("/api/v1/recommendations/{informationType}")
public class RecommendationFeedController {

    /** Feed 只读应用服务。 */
    private final RecommendationFeedService feedService;

    public RecommendationFeedController(RecommendationFeedService feedService) {
        this.feedService = feedService;
    }

    /** 返回最近成功 Run 的当前可见分页 Item，不触发实时计算。 */
    @GetMapping("/feed")
    public ApiResponse<RecommendationFeedResponse> feed(
            @PathVariable String informationType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(
                "RECOMMENDATION_FEED_FOUND",
                RecommendationFeedResponse.from(feedService.get(informationType, page, pageSize)));
    }
}
