package com.informationplatform.hub.recommendation.job.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.recommendation.application.JobRecommendationProfileHasher;
import com.informationplatform.hub.recommendation.application.JobRecommendationProfileNormalizer;
import com.informationplatform.hub.recommendation.application.RecommendationProfileRequestException;
import com.informationplatform.hub.recommendation.application.SaveRecommendationProfileCommand;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 验证 JOB Profile 规范化、限制与完整组合 hash 稳定性。 */
class JobRecommendationProfileNormalizerTest {

    private final JobRecommendationProfileNormalizer normalizer =
            new JobRecommendationProfileNormalizer();
    private final JobRecommendationProfileHasher hasher =
            new JobRecommendationProfileHasher(new ObjectMapper());

    @Test
    void normalizesArraysAndKeepsHashStableAcrossOrderAndDuplicates() {
        JobRecommendationProfilePreferences first = normalizer.normalize(command(
                List.of(" Java 后端 ", "大数据开发", "Java 后端"),
                List.of(" remote ", "HYBRID", "REMOTE")));
        JobRecommendationProfilePreferences second = normalizer.normalize(command(
                List.of("大数据开发", "Java 后端"),
                List.of("HYBRID", "REMOTE")));

        assertEquals(List.of("Java 后端", "大数据开发"), first.targetRoles());
        assertEquals(List.of("HYBRID", "REMOTE"), first.preferredRemoteTypes());
        assertEquals(
                hasher.hash("JOB", 12L, 7, 50, first),
                hasher.hash("JOB", 12L, 7, 50, second));
    }

    @Test
    void rejectsListCountItemLengthAndUnsupportedRemoteType() {
        List<String> tooMany = new ArrayList<>();
        for (int index = 0; index < 51; index++) {
            tooMany.add("role-" + index);
        }

        assertCode(
                "RECOMMENDATION_PROFILE_LIST_TOO_LARGE",
                () -> normalizer.normalize(command(tooMany, List.of("REMOTE"))));
        assertCode(
                "RECOMMENDATION_PROFILE_ITEM_TOO_LONG",
                () -> normalizer.normalize(command(List.of("岗".repeat(101)), List.of("REMOTE"))));
        assertCode(
                "RECOMMENDATION_PROFILE_REMOTE_TYPE_INVALID",
                () -> normalizer.normalize(command(List.of("Java"), List.of("UNKNOWN"))));
    }

    private SaveRecommendationProfileCommand command(
            List<String> targetRoles, List<String> remoteTypes) {
        return new SaveRecommendationProfileCommand(
                12L,
                7,
                50,
                targetRoles,
                List.of(" Spring Boot ", "Java", "Java"),
                List.of(" 成都 ", "成都"),
                remoteTypes,
                15_000,
                List.of(" 纯销售 ", "纯销售"));
    }

    private void assertCode(String code, org.junit.jupiter.api.function.Executable executable) {
        RecommendationProfileRequestException exception =
                assertThrows(RecommendationProfileRequestException.class, executable);
        assertEquals(code, exception.getCode());
    }
}
