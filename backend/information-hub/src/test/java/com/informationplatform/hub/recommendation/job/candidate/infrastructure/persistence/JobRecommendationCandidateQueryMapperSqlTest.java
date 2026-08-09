package com.informationplatform.hub.recommendation.job.candidate.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.informationplatform.hub.recommendation.job.candidate.domain.JobRecommendationCandidateRequest;
import com.informationplatform.hub.recommendation.job.domain.JobRecommendationProfilePreferences;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 验证 Candidate 动态 SQL 只使用绑定参数并包含全部冻结身份与 hard exclusion。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class JobRecommendationCandidateQueryMapperSqlTest {

    private static final String SELECT_ELIGIBLE =
            "com.informationplatform.hub.recommendation.job.candidate.infrastructure.persistence"
                    + ".JobRecommendationCandidateQueryMapper.selectEligibleCandidates";

    @Autowired private SqlSessionFactory sqlSessionFactory;

    @Test
    void rendersStableBoundCandidateQueryWithoutRawPayload() {
        JobRecommendationCandidateRequest request = new JobRecommendationCandidateRequest(
                9L,
                12L,
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 8, 0, 0),
                new JobRecommendationProfilePreferences(
                        List.of(), List.of(), List.of(), List.of(), null,
                        List.of("纯销售", "外包")));
        MappedStatement statement = sqlSessionFactory.getConfiguration()
                .getMappedStatement(SELECT_ELIGIBLE);
        BoundSql boundSql = statement.getBoundSql(Map.of(
                "request", request,
                "compatibleVersions", List.of(1, 2)));
        String sql = boundSql.getSql().replaceAll("\\s+", " ").trim();

        assertTrue(sql.contains("snapshot.version_no = item.current_version_no"));
        assertTrue(sql.contains("analysis.prompt_version_id = ?"));
        assertTrue(sql.contains("analysis.analysis_definition_version IN ( ? , ? )"));
        assertTrue(sql.contains("interaction_core.feedback_state = 'NOT_INTERESTED'"));
        assertTrue(sql.contains("disposition.job_disposition = 'CONTACTED_NOT_SUITABLE'"));
        assertTrue(sql.contains("LOCATE(?, snapshot.title)"));
        assertTrue(sql.contains("ORDER BY item.first_seen_time DESC, item.id DESC"));
        assertFalse(sql.contains("CONTACTED'"));
        assertFalse(sql.contains("raw_payload"));
        assertFalse(sql.contains("纯销售"));
        assertFalse(sql.contains("${"));
    }
}
