package com.informationplatform.hub.recommendation.job.candidate.infrastructure.persistence;

import com.informationplatform.hub.recommendation.job.candidate.domain.JobRecommendationCandidateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 执行 JOB Recommendation Candidate 的只读稳定查询。 */
@Mapper
public interface JobRecommendationCandidateQueryMapper {

    /** 统计 Interaction 与关键词 hard exclusion 前的初始候选 Information 数。 */
    long countCandidates(
            @Param("request") JobRecommendationCandidateRequest request,
            @Param("compatibleVersions") List<Integer> compatibleVersions);

    /** 查询 hard exclusion 后的候选及全部兼容成功 Analysis，供应用层选择最高版本。 */
    List<JobRecommendationCandidateRow> selectEligibleCandidates(
            @Param("request") JobRecommendationCandidateRequest request,
            @Param("compatibleVersions") List<Integer> compatibleVersions);
}
