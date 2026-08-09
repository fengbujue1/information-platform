package com.informationplatform.hub.recommendation.job.candidate.infrastructure.persistence;

import java.time.LocalDateTime;

/** JOB Candidate 稳定查询的 Snapshot 与 Analysis 投影。 */
public class JobRecommendationCandidateRow {

    /** JOB Information 主键。 */
    private Long informationId;

    /** 当前 Snapshot 主键。 */
    private Long snapshotId;

    /** 成功 USER_RELEVANCE Analysis 主键。 */
    private Long analysisId;

    /** Analysis Definition 版本。 */
    private Integer analysisDefinitionVersion;

    /** Information 首次进入平台的 UTC 时间。 */
    private LocalDateTime firstSeenTime;

    /** 当前 Snapshot 标题。 */
    private String title;

    /** 当前 Snapshot 正文，可为空。 */
    private String content;

    /** 当前 Snapshot 标准化 JSON。 */
    private String standardizedPayload;

    /** 已通过 Definition Schema 校验的 Analysis JSON。 */
    private String analysisResult;

    /** USER_RELEVANCE 相关度分数，范围 0..100。 */
    private Integer aiRelevanceScore;

    public Long getInformationId() { return informationId; }
    public void setInformationId(Long informationId) { this.informationId = informationId; }
    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }
    public Long getAnalysisId() { return analysisId; }
    public void setAnalysisId(Long analysisId) { this.analysisId = analysisId; }
    public Integer getAnalysisDefinitionVersion() { return analysisDefinitionVersion; }
    public void setAnalysisDefinitionVersion(Integer analysisDefinitionVersion) {
        this.analysisDefinitionVersion = analysisDefinitionVersion;
    }
    public LocalDateTime getFirstSeenTime() { return firstSeenTime; }
    public void setFirstSeenTime(LocalDateTime firstSeenTime) {
        this.firstSeenTime = firstSeenTime;
    }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStandardizedPayload() { return standardizedPayload; }
    public void setStandardizedPayload(String standardizedPayload) {
        this.standardizedPayload = standardizedPayload;
    }
    public String getAnalysisResult() { return analysisResult; }
    public void setAnalysisResult(String analysisResult) { this.analysisResult = analysisResult; }
    public Integer getAiRelevanceScore() { return aiRelevanceScore; }
    public void setAiRelevanceScore(Integer aiRelevanceScore) {
        this.aiRelevanceScore = aiRelevanceScore;
    }
}
