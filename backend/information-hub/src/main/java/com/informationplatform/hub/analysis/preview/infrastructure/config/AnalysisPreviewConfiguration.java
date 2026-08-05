package com.informationplatform.hub.analysis.preview.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Preview 使用可替换 UTC Clock，保证窗口与 Token 时间可测试。 */
@Configuration
public class AnalysisPreviewConfiguration {

    @Bean
    Clock analysisPreviewClock() {
        return Clock.systemUTC();
    }
}
