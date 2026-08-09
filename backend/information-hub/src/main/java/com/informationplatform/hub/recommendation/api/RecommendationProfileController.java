package com.informationplatform.hub.recommendation.api;

import com.informationplatform.hub.common.api.ApiResponse;
import com.informationplatform.hub.recommendation.api.dto.PutRecommendationProfileRequest;
import com.informationplatform.hub.recommendation.api.dto.RecommendationProfileResponse;
import com.informationplatform.hub.recommendation.application.RecommendationProfileChange;
import com.informationplatform.hub.recommendation.application.RecommendationProfileService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供当前 Owner 按 Information Type 访问 Recommendation Profile 的 API。 */
@Validated
@RestController
@RequestMapping("/api/v1/recommendation/profiles")
public class RecommendationProfileController {

    /** Recommendation Profile 应用服务。 */
    private final RecommendationProfileService profileService;

    public RecommendationProfileController(RecommendationProfileService profileService) {
        this.profileService = profileService;
    }

    /** 返回当前 Owner 的完整组合 Recommendation Profile。 */
    @GetMapping("/{informationType}")
    public ApiResponse<RecommendationProfileResponse> getProfile(
            @PathVariable String informationType) {
        return ApiResponse.success(
                "RECOMMENDATION_PROFILE_FOUND",
                RecommendationProfileResponse.from(profileService.getProfile(informationType)));
    }

    /** 创建或完整替换当前 Owner 的组合 Profile；不会触发 Analysis 或 Recommendation Run。 */
    @PutMapping("/{informationType}")
    public ApiResponse<RecommendationProfileResponse> saveProfile(
            @PathVariable String informationType,
            @Valid @RequestBody PutRecommendationProfileRequest request) {
        RecommendationProfileChange change =
                profileService.saveProfile(informationType, request.toCommand());
        return ApiResponse.success(
                change.created()
                        ? "RECOMMENDATION_PROFILE_CREATED"
                        : "RECOMMENDATION_PROFILE_UPDATED",
                RecommendationProfileResponse.from(change.profile()));
    }
}
