package com.informationplatform.hub.common.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** 验证浏览器公共 API 错误消息的中文目录与安全兜底。 */
class BrowserApiErrorMessagesTest {

    @Test
    void returnsActionableChineseForKnownCode() {
        assertEquals(
                "所选提示词方案已停用，请先启用后再保存定时分析",
                BrowserApiErrorMessages.message("ANALYSIS_SCHEDULE_PROFILE_DISABLED"));
    }

    @Test
    void returnsActionableChineseForConfiguredPreviewLimits() {
        assertEquals(
                "候选时间范围超出当前平台允许范围",
                BrowserApiErrorMessages.message("PREVIEW_WINDOW_DAYS_INVALID"));
        assertEquals(
                "最大候选数量超出当前平台允许范围",
                BrowserApiErrorMessages.message("PREVIEW_MAX_CANDIDATES_INVALID"));
        assertEquals(
                "预估 Token 预算超出当前平台允许范围",
                BrowserApiErrorMessages.message("PREVIEW_MAX_ESTIMATED_TOKENS_INVALID"));
    }

    @Test
    void returnsSafeChineseFallbackForUnknownCode() {
        assertEquals("请求失败，请稍后重试", BrowserApiErrorMessages.message("UNKNOWN_CODE"));
    }
}
