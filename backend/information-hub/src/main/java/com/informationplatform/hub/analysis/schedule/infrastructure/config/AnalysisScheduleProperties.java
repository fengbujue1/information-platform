package com.informationplatform.hub.analysis.schedule.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 每日 Analysis Schedule Dispatcher 的服务端运行参数。 */
@Component
@ConfigurationProperties(prefix = "information-hub.ai.schedule")
public class AnalysisScheduleProperties {

    /** 是否启用 Dispatcher；默认启用，但每条新 Schedule 仍默认关闭。 */
    private boolean dispatcherEnabled = true;

    /** 允许延迟触发原计划点的最大时间，默认 5 分钟。 */
    private Duration misfireGrace = Duration.ofMinutes(5);

    /** 单次轮询最多处理的不同 Schedule 数量。 */
    private int maxSchedulesPerPoll = 100;

    public boolean isDispatcherEnabled() {
        return dispatcherEnabled;
    }

    public void setDispatcherEnabled(boolean dispatcherEnabled) {
        this.dispatcherEnabled = dispatcherEnabled;
    }

    public Duration getMisfireGrace() {
        return misfireGrace;
    }

    public void setMisfireGrace(Duration misfireGrace) {
        this.misfireGrace = misfireGrace;
    }

    public int getMaxSchedulesPerPoll() {
        return maxSchedulesPerPoll;
    }

    public void setMaxSchedulesPerPoll(int maxSchedulesPerPoll) {
        this.maxSchedulesPerPoll = maxSchedulesPerPoll;
    }
}
