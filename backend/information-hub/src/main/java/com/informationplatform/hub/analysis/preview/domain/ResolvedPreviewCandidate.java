package com.informationplatform.hub.analysis.preview.domain;

import com.informationplatform.hub.analysis.candidate.domain.AnalysisCandidate;
import com.informationplatform.hub.analysis.processing.domain.AnalysisTokenEstimate;
import com.informationplatform.hub.analysis.provider.domain.AiProviderRequest;

/** Confirm 可冻结的单个候选、Estimate、预算决策和安全 Provider 请求。 */
public record ResolvedPreviewCandidate(
        /** 当前候选及其不可变 Snapshot 投影。 */
        AnalysisCandidate candidate,
        /** 本候选的版本化 Estimated Token。 */
        AnalysisTokenEstimate estimate,
        /** 本候选是否位于 Token Budget 的稳定前缀内。 */
        boolean selected,
        /** 使用冻结 Prompt/Definition/Snapshot 组装的 Provider 请求。 */
        AiProviderRequest providerRequest) {

    public ResolvedPreviewCandidate {
        if (candidate == null || estimate == null || providerRequest == null) {
            throw new IllegalArgumentException("Resolved preview candidate is invalid");
        }
    }

    /** 转换为候选指纹的稳定输入，不包含正文或 Prompt。 */
    public PreviewCandidateEstimate fingerprintValue() {
        return new PreviewCandidateEstimate(
                candidate.informationId(),
                candidate.snapshotId(),
                candidate.firstSeenTime(),
                estimate.inputTokens(),
                estimate.outputTokens(),
                estimate.totalTokens(),
                selected);
    }
}
