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
    void returnsSafeChineseFallbackForUnknownCode() {
        assertEquals("请求失败，请稍后重试", BrowserApiErrorMessages.message("UNKNOWN_CODE"));
    }
}