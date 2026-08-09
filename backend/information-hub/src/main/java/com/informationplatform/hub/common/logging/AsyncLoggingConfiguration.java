package com.informationplatform.hub.common.logging;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** 为应用异步方法配置有界线程池和 MDC 传播，不影响系统 Scheduler 线程。 */
@Configuration
@EnableAsync
public class AsyncLoggingConfiguration implements AsyncConfigurer {

    /** 复制并清理 MDC 的任务装饰器。 */
    private final MdcTaskDecorator taskDecorator;

    public AsyncLoggingConfiguration(MdcTaskDecorator taskDecorator) {
        this.taskDecorator = taskDecorator;
    }

    /** 异步业务任务使用的统一执行器；任务完成后由装饰器清理线程 MDC。 */
    @Bean(name = "applicationTaskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("information-hub-async-");
        executor.setTaskDecorator(taskDecorator);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
