package com.informationplatform.hub.analysis.candidate.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.candidate.domain.AnalysisCandidate;
import com.informationplatform.hub.analysis.candidate.domain.CandidateResolution;
import com.informationplatform.hub.analysis.candidate.domain.CandidateResolutionRequest;
import com.informationplatform.hub.analysis.candidate.domain.CandidateResolver;
import com.informationplatform.hub.analysis.candidate.infrastructure.persistence.CandidateWindowCountsRow;
import com.informationplatform.hub.analysis.candidate.infrastructure.persistence.JobCandidateQueryMapper;
import com.informationplatform.hub.analysis.candidate.infrastructure.persistence.JobCandidateRow;
import com.informationplatform.hub.analysis.definition.domain.AnalysisInformationType;
import com.informationplatform.hub.analysis.definition.domain.AnalysisSnapshotSource;
import com.informationplatform.hub.analysis.processing.application.AnalysisPersistenceException;
import java.util.List;
import org.springframework.stereotype.Component;

/** 唯一真实实现：按 JOB FIRST_INGESTED 解析当前 Snapshot 候选。 */
@Component
public class JobCandidateResolver implements CandidateResolver {

    /** JOB 候选只读查询。 */
    private final JobCandidateQueryMapper queryMapper;

    /** 标准化 Snapshot JSON 解析器。 */
    private final ObjectMapper objectMapper;

    public JobCandidateResolver(JobCandidateQueryMapper queryMapper, ObjectMapper objectMapper) {
        this.queryMapper = queryMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public AnalysisInformationType informationType() {
        return AnalysisInformationType.JOB;
    }

    /** 使用同一逻辑身份分别统计窗口并读取稳定排序的前 limit 个待分析候选。 */
    @Override
    public CandidateResolution resolve(CandidateResolutionRequest request) {
        CandidateWindowCountsRow counts = queryMapper.countWindow(
                request.userId(),
                request.promptVersionId(),
                request.definitionKey(),
                request.definitionVersion(),
                request.windowStart(),
                request.windowEnd());
        if (counts == null
                || counts.getTotalInWindow() == null
                || counts.getAlreadyAnalyzedCount() == null) {
            throw new AnalysisPersistenceException("Candidate window counts could not be read");
        }
        List<AnalysisCandidate> candidates = queryMapper.selectEligible(
                        request.userId(),
                        request.promptVersionId(),
                        request.definitionKey(),
                        request.definitionVersion(),
                        request.windowStart(),
                        request.windowEnd(),
                        request.maxCandidates())
                .stream()
                .map(this::toCandidate)
                .toList();
        long total = counts.getTotalInWindow();
        long already = counts.getAlreadyAnalyzedCount();
        return new CandidateResolution(total, already, total - already, candidates);
    }

    private AnalysisCandidate toCandidate(JobCandidateRow row) {
        try {
            AnalysisSnapshotSource source = new AnalysisSnapshotSource(
                    row.getSnapshotId(),
                    row.getInformationId(),
                    AnalysisInformationType.valueOf(row.getInformationType()),
                    row.getTitle(),
                    row.getContent(),
                    objectMapper.readTree(row.getStandardizedPayload()));
            return new AnalysisCandidate(
                    row.getInformationId(), row.getSnapshotId(), row.getFirstSeenTime(), source);
        } catch (IllegalArgumentException | JsonProcessingException exception) {
            throw new AnalysisPersistenceException(
                    "Candidate standardized payload could not be read", exception);
        }
    }
}
