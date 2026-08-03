package com.informationplatform.hub.analysis.prompt.api;

import com.informationplatform.hub.analysis.prompt.api.dto.ActivatePromptVersionRequest;
import com.informationplatform.hub.analysis.prompt.api.dto.CreatePromptProfileRequest;
import com.informationplatform.hub.analysis.prompt.api.dto.CreatePromptVersionRequest;
import com.informationplatform.hub.analysis.prompt.api.dto.PromptProfileResponse;
import com.informationplatform.hub.analysis.prompt.api.dto.PromptVersionResponse;
import com.informationplatform.hub.analysis.prompt.api.dto.UpdatePromptProfileStatusRequest;
import com.informationplatform.hub.analysis.prompt.application.PromptProfileService;
import com.informationplatform.hub.analysis.prompt.domain.PromptVersionChange;
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

/** 提供账号级 Prompt Profile 与不可变 Version Contract API。 */
@Validated
@RestController
@RequestMapping("/api/v1/ai/prompt-profiles")
public class PromptProfileController {

    /** Prompt Profile 与 Version 应用服务。 */
    private final PromptProfileService promptProfileService;

    public PromptProfileController(PromptProfileService promptProfileService) {
        this.promptProfileService = promptProfileService;
    }

    /** 返回当前登录账号的全部 Prompt Profile。 */
    @GetMapping
    public ApiResponse<List<PromptProfileResponse>> listProfiles() {
        return ApiResponse.success(
                "PROMPT_PROFILES_FOUND",
                promptProfileService.listProfiles().stream()
                        .map(PromptProfileResponse::from)
                        .toList());
    }

    /** 创建当前登录账号的 Prompt Profile。 */
    @PostMapping
    public ApiResponse<PromptProfileResponse> createProfile(
            @Valid @RequestBody CreatePromptProfileRequest request) {
        return ApiResponse.success(
                "PROMPT_PROFILE_CREATED",
                PromptProfileResponse.from(promptProfileService.createProfile(
                        request.name(), request.analysisDefinitionKey())));
    }

    /** 返回当前 Owner 下的指定 Prompt Profile。 */
    @GetMapping("/{profileId}")
    public ApiResponse<PromptProfileResponse> getProfile(
            @PathVariable @Positive long profileId) {
        return ApiResponse.success(
                "PROMPT_PROFILE_FOUND",
                PromptProfileResponse.from(promptProfileService.getProfile(profileId)));
    }

    /** 创建或复用不可变 Version，并在同一事务中将其切换为 Active。 */
    @PostMapping("/{profileId}/versions")
    public ApiResponse<PromptVersionResponse> createVersion(
            @PathVariable @Positive long profileId,
            @Valid @RequestBody CreatePromptVersionRequest request) {
        PromptVersionChange change =
                promptProfileService.createVersion(profileId, request.content());
        return ApiResponse.success(
                change.created() ? "PROMPT_VERSION_CREATED" : "PROMPT_VERSION_REUSED",
                PromptVersionResponse.from(change.version()));
    }

    /** 返回指定 Owner Profile 的不可变 Version 历史。 */
    @GetMapping("/{profileId}/versions")
    public ApiResponse<List<PromptVersionResponse>> listVersions(
            @PathVariable @Positive long profileId) {
        return ApiResponse.success(
                "PROMPT_VERSIONS_FOUND",
                promptProfileService.listVersions(profileId).stream()
                        .map(PromptVersionResponse::from)
                        .toList());
    }

    /** 切换到同一 Owner、同一 Profile 下的历史 Version。 */
    @PutMapping("/{profileId}/active-version")
    public ApiResponse<PromptProfileResponse> activateVersion(
            @PathVariable @Positive long profileId,
            @Valid @RequestBody ActivatePromptVersionRequest request) {
        return ApiResponse.success(
                "PROMPT_ACTIVE_VERSION_UPDATED",
                PromptProfileResponse.from(
                        promptProfileService.activateVersion(profileId, request.versionId())));
    }

    /** 使用状态停用或重新启用 Profile，不物理删除历史。 */
    @PutMapping("/{profileId}/status")
    public ApiResponse<PromptProfileResponse> updateStatus(
            @PathVariable @Positive long profileId,
            @Valid @RequestBody UpdatePromptProfileStatusRequest request) {
        return ApiResponse.success(
                "PROMPT_PROFILE_STATUS_UPDATED",
                PromptProfileResponse.from(
                        promptProfileService.updateStatus(profileId, request.status())));
    }
}
