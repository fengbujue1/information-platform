package com.informationplatform.hub.recommendation.job.run.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.recommendation.job.run.domain.JobRecommendationRunProfileSnapshot;
import com.informationplatform.hub.recommendation.run.application.RecommendationRunPersistenceException;
import org.springframework.stereotype.Component;

/** 在冻结 JOB Profile snapshot 领域对象与 MySQL JSON 字符串之间转换。 */
@Component
public class JobRecommendationRunProfileSnapshotCodec {

    /** 项目统一 Jackson 配置。 */
    private final ObjectMapper objectMapper;

    public JobRecommendationRunProfileSnapshotCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 编码完整、规范化且不可变的 JOB Profile snapshot。 */
    public String encode(JobRecommendationRunProfileSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw persistence("Recommendation Profile snapshot could not be encoded", exception);
        }
    }

    /** 解码 Run 已冻结的 JOB Profile snapshot，不读取当前 Profile。 */
    public JobRecommendationRunProfileSnapshot decode(String json) {
        try {
            return objectMapper.readValue(json, JobRecommendationRunProfileSnapshot.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw persistence("Recommendation Profile snapshot could not be decoded", exception);
        }
    }

    private RecommendationRunPersistenceException persistence(
            String message, Throwable cause) {
        return new RecommendationRunPersistenceException(message, cause);
    }
}
