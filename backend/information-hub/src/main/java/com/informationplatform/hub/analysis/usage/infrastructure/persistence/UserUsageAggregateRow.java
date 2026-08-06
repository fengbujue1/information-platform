package com.informationplatform.hub.analysis.usage.infrastructure.persistence;

/** 当前用户在指定时间范围内的 Provider Actual Usage 聚合行。 */
public class UserUsageAggregateRow {

    /** 范围内全部 Invocation 数。 */
    private Long invocationCount;

    /** Provider 已报告 Usage 的 Invocation 数。 */
    private Long reportedInvocationCount;

    /** Provider 未报告 Usage 的 Invocation 数。 */
    private Long unavailableInvocationCount;

    /** Provider 报告的 Actual 输入 Token 合计，完全未知时为空。 */
    private Long inputTokens;

    /** Provider 报告的 Actual 输出 Token 合计，完全未知时为空。 */
    private Long outputTokens;

    /** Provider 报告的 Actual 总 Token 合计，完全未知时为空。 */
    private Long totalTokens;

    public Long getInvocationCount() {
        return invocationCount;
    }

    public void setInvocationCount(Long invocationCount) {
        this.invocationCount = invocationCount;
    }

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
