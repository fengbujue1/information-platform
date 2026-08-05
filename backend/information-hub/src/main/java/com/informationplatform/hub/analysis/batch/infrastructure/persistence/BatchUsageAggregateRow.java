package com.informationplatform.hub.analysis.batch.infrastructure.persistence;

/** 从绑定 Batch Item 的 Invocation 聚合 Actual Usage。 */
public class BatchUsageAggregateRow {

    /** 报告 Provider Usage 的 Invocation 数。 */
    private Long reportedInvocationCount;

    /** 未报告 Provider Usage 的 Invocation 数。 */
    private Long unavailableInvocationCount;

    /** Actual 输入 Token 合计，完全未知时为空。 */
    private Long inputTokens;

    /** Actual 输出 Token 合计，完全未知时为空。 */
    private Long outputTokens;

    /** Actual 总 Token 合计，完全未知时为空。 */
    private Long totalTokens;

    public Long getReportedInvocationCount() {
        return reportedInvocationCount;
    }

    public void setReportedInvocationCount(Long reportedInvocationCount) {
        this.reportedInvocationCount = reportedInvocationCount;
    }

    public Long getUnavailableInvocationCount() {
        return unavailableInvocationCount;
    }

    public void setUnavailableInvocationCount(Long unavailableInvocationCount) {
        this.unavailableInvocationCount = unavailableInvocationCount;
    }

    public Long getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Long inputTokens) {
        this.inputTokens = inputTokens;
    }

    public Long getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Long outputTokens) {
        this.outputTokens = outputTokens;
    }

    public Long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Long totalTokens) {
        this.totalTokens = totalTokens;
    }
}
