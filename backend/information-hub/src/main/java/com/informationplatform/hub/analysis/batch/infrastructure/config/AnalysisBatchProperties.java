package com.informationplatform.hub.analysis.batch.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Analysis Batch Worker 的服务端运行开关。 */
@Component
@ConfigurationProperties(prefix = "information-hub.ai.batch")
public class AnalysisBatchProperties {

    /** 是否允许 Worker 领取 Batch Item；部署管理员确认 Provider 完整后开启，默认关闭。 */
    private boolean workerEnabled;

    public boolean isWorkerEnabled() {
        return workerEnabled;
    }

    public void setWorkerEnabled(boolean workerEnabled) {
        this.workerEnabled = workerEnabled;
    }
}
