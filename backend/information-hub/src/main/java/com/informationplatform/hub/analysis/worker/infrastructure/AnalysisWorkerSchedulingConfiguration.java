package com.informationplatform.hub.analysis.worker.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 启用模块化单体内的低并发 Analysis Batch Worker。 */
@Configuration
@EnableScheduling
public class AnalysisWorkerSchedulingConfiguration {
}
