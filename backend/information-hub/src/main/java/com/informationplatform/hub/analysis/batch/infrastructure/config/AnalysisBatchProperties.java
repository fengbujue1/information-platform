package com.informationplatform.hub.analysis.batch.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Analysis Batch Worker 的服务端运行开关。 */
@Component
@ConfigurationProperties(prefix = "information-hub.ai.batch")
public class AnalysisBatchProperties {

    /** 是否允许后台 Worker 领取 Batch Item；默认关闭以避免意外产生费用。 */
    private boolean workerEnabled;

    public boolean isWorkerEnabled() {
        return workerEnabled;
    }

    public void setWorkerEnabled(boolean workerEnabled) {
        this.workerEnabled = workerEnabled;
    }
}
