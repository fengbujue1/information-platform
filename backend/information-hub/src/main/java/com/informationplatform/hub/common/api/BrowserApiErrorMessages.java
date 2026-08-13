package com.informationplatform.hub.common.api;

import java.util.Map;

/** 为浏览器公共 API 提供稳定、脱敏且可直接展示的中文错误消息。 */
public final class BrowserApiErrorMessages {

    private static final String DEFAULT_MESSAGE = "请求失败，请稍后重试";

    private static final Map<String, String> MESSAGES = Map.ofEntries(
            Map.entry("INVALID_JOB_PAGE", "页码必须从 1 开始"),
            Map.entry("INVALID_JOB_PAGE_SIZE", "每页数量必须在 1 到 100 之间"),
            Map.entry("INVALID_JOB_SORT_FIELD", "选择的排序字段不受支持"),
            Map.entry("INVALID_JOB_SORT_DIRECTION", "排序方向必须为升序或降序"),
            Map.entry("INVALID_JOB_SALARY", "薪资条件不能小于 0"),
            Map.entry("INVALID_JOB_SALARY_RANGE", "最低薪资不能高于最高薪资"),
            Map.entry("INVALID_JOB_FILTER", "筛选文本过长，请缩短后重试"),
            Map.entry("INVALID_JOB_ID", "职位 ID 必须为正整数"),            Map.entry("VALIDATION_FAILED", "请求参数校验失败"),
            Map.entry("INVALID_JSON", "请求内容格式不正确"),
            Map.entry("REQUEST_TOO_LARGE", "请求内容超过允许大小"),
            Map.entry("INVALID_REQUEST_PARAMETER", "请求参数格式不正确"),
            Map.entry("JOB_NOT_FOUND", "职位不存在或已不可用"),
            Map.entry("JOB_QUERY_FAILED", "职位信息暂时无法查询"),
            Map.entry("AUTHENTICATION_FAILED", "用户名或密码错误"),
            Map.entry("AUTHENTICATION_REQUIRED", "登录状态已失效，请重新登录"),
            Map.entry("ACCESS_DENIED", "没有权限执行此操作"),
            Map.entry("PROMPT_PROFILE_NAME_REQUIRED", "请输入提示词方案名称"),
            Map.entry("PROMPT_PROFILE_NAME_TOO_LONG", "提示词方案名称过长"),
            Map.entry("PROMPT_PROFILE_NAME_CONFLICT", "当前账号已存在同名提示词方案"),
            Map.entry("PROMPT_PROFILE_NOT_FOUND", "提示词方案不存在或无权访问"),
            Map.entry("PROMPT_VERSION_NOT_FOUND", "提示词版本不存在或不属于该方案"),
            Map.entry("PROMPT_CONTENT_REQUIRED", "请输入用户提示词"),
            Map.entry("PROMPT_CONTENT_TOO_LONG", "用户提示词超过允许长度"),
            Map.entry("INVALID_PROMPT_PROFILE_STATUS", "提示词方案状态不受支持"),
            Map.entry("INVALID_PROMPT_RESOURCE_ID", "提示词资源 ID 必须为正整数"),
            Map.entry("UNSUPPORTED_ANALYSIS_DEFINITION", "当前分析定义不受支持"),
            Map.entry("PROMPT_PERSISTENCE_FAILED", "提示词数据暂时无法保存，请稍后重试"),
            Map.entry("ANALYSIS_NOT_FOUND", "分析结果不存在或无权访问"),
            Map.entry("INFORMATION_ANALYSIS_NOT_FOUND", "分析结果不存在或无权访问"),
            Map.entry("ANALYSIS_BATCH_NOT_FOUND", "分析批次不存在或无权访问"),
            Map.entry("ANALYSIS_BATCH_REQUEST_INVALID", "分析批次请求参数无效"),
            Map.entry("ANALYSIS_BATCH_UNAVAILABLE", "分析批次当前不可执行"),
            Map.entry("ANALYSIS_BATCH_PERSISTENCE_FAILED", "分析批次暂时无法保存，请稍后重试"),
            Map.entry("ANALYSIS_PREVIEW_TOKEN_EXPIRED", "预览已过期，请重新预览"),
            Map.entry("ANALYSIS_PREVIEW_TOKEN_INVALID", "预览已失效，请重新预览"),
            Map.entry("ANALYSIS_PREVIEW_DRIFTED", "候选或配置已经变化，请重新预览"),
            Map.entry("ANALYSIS_PREVIEW_NOT_CONFIGURED", "分析预览服务尚未配置"),
            Map.entry("PREVIEW_WINDOW_DAYS_INVALID", "候选时间范围超出当前平台允许范围"),
            Map.entry("PREVIEW_MAX_CANDIDATES_INVALID", "最大候选数量超出当前平台允许范围"),
            Map.entry("PREVIEW_MAX_ESTIMATED_TOKENS_INVALID", "预估 Token 预算超出当前平台允许范围"),
            Map.entry("ANALYSIS_PROVIDER_DISABLED", "AI 模型服务当前未启用"),
            Map.entry("ANALYSIS_WORKER_DISABLED", "分析 Worker 当前未启用"),
            Map.entry("ANALYSIS_CONFIGURATION_INVALID", "分析配置当前不可执行"),
            Map.entry("ANALYSIS_PERSISTENCE_FAILED", "分析结果暂时无法保存，请稍后重试"),
            Map.entry("ANALYSIS_SCHEDULE_NOT_FOUND", "定时分析不存在或无权访问"),
            Map.entry("ANALYSIS_SCHEDULE_NAME_CONFLICT", "当前账号已存在同名定时分析"),
            Map.entry("ANALYSIS_SCHEDULE_PROFILE_ID_INVALID", "请选择有效的提示词方案"),
            Map.entry("ANALYSIS_SCHEDULE_PROFILE_DISABLED", "所选提示词方案已停用，请先启用后再保存定时分析"),
            Map.entry("ANALYSIS_SCHEDULE_ACTIVE_VERSION_REQUIRED", "所选提示词方案没有生效版本，请先创建并启用版本"),
            Map.entry("ANALYSIS_SCHEDULE_NAME_REQUIRED", "请输入定时分析名称"),
            Map.entry("ANALYSIS_SCHEDULE_NAME_TOO_LONG", "定时分析名称过长"),
            Map.entry("ANALYSIS_SCHEDULE_TIMEZONE_INVALID", "请输入有效的 IANA 时区"),
            Map.entry("ANALYSIS_SCHEDULE_RESOURCE_ID_INVALID", "定时分析 ID 必须为正整数"),
            Map.entry("ANALYSIS_SCHEDULE_PERSISTENCE_FAILED", "定时分析暂时无法保存，请稍后重试"),
            Map.entry("RECOMMENDATION_PROFILE_NOT_FOUND", "尚未配置职位推荐画像"),
            Map.entry("RECOMMENDATION_PROMPT_PROFILE_NOT_FOUND", "绑定的提示词方案不存在"),
            Map.entry("RECOMMENDATION_PROMPT_PROFILE_INCOMPATIBLE", "请选择用于职位相关性分析的提示词方案"),
            Map.entry("RECOMMENDATION_PROMPT_PROFILE_DISABLED", "绑定的提示词方案已停用"),
            Map.entry("RECOMMENDATION_PROMPT_PROFILE_VERSION_REQUIRED", "绑定的提示词方案尚无生效版本"),
            Map.entry("RECOMMENDATION_PROMPT_ACTIVE_VERSION_REQUIRED", "绑定的提示词方案必须启用并具有生效版本"),
            Map.entry("RECOMMENDATION_RUN_IN_PROGRESS", "已有一次手动刷新正在执行，请等待完成"),
            Map.entry("RECOMMENDATION_RUN_NOT_FOUND", "推荐刷新记录不存在或无权访问"),
            Map.entry("RECOMMENDATION_INFORMATION_TYPE_UNSUPPORTED", "当前只支持职位推荐"),
            Map.entry("RECOMMENDATION_FEED_PAGE_INVALID", "推荐页码必须从 1 开始"),
            Map.entry("RECOMMENDATION_FEED_PAGE_SIZE_INVALID", "推荐每页数量必须在 1 到 100 之间"),
            Map.entry("RECOMMENDATION_INTERACTION_ATTRIBUTION_INVALID", "该推荐项已失效，请刷新页面后重试"),
            Map.entry("RECOMMENDATION_PROFILE_PERSISTENCE_FAILED", "推荐画像暂时无法保存，请稍后重试"),
            Map.entry("RECOMMENDATION_RUN_PERSISTENCE_FAILED", "推荐刷新暂时无法执行，请稍后重试"),
            Map.entry("RECOMMENDATION_FEED_PERSISTENCE_FAILED", "推荐列表暂时无法读取，请稍后重试"),
            Map.entry("RECOMMENDATION_INTERACTION_PERSISTENCE_FAILED", "推荐状态暂时无法保存，请稍后重试"),
            Map.entry("INTERNAL_ERROR", "服务暂时不可用，请稍后重试"));

    private BrowserApiErrorMessages() {}

    /** 按稳定错误码返回中文展示文案，未知错误码使用统一脱敏兜底。 */
    public static String message(String code) {
        return MESSAGES.getOrDefault(code, DEFAULT_MESSAGE);
    }
}
