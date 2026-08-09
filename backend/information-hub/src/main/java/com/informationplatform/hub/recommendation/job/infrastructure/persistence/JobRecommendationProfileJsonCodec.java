package com.informationplatform.hub.recommendation.job.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.recommendation.application.RecommendationProfilePersistenceException;
import java.util.List;
import org.springframework.stereotype.Component;

/** 在 JOB Profile 领域数组与 MySQL JSON 列字符串之间转换。 */
@Component
public class JobRecommendationProfileJsonCodec {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    /** 项目统一 Jackson 配置。 */
    private final ObjectMapper objectMapper;

    public JobRecommendationProfileJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 将规范化字符串数组编码为 JSON。 */
    public String encode(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw persistence("JOB Profile preferences could not be encoded", exception);
        }
    }

    /** 将数据库 JSON 解码为只读字符串数组；历史 NULL 兼容为空数组。 */
    public List<String> decode(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(json, STRING_LIST);
            return values == null ? List.of() : List.copyOf(values);
        } catch (JsonProcessingException | NullPointerException exception) {
            throw persistence("JOB Profile preferences could not be decoded", exception);
        }
    }

    private RecommendationProfilePersistenceException persistence(
            String message, Throwable cause) {
        return new RecommendationProfilePersistenceException(message, cause);
    }
}
