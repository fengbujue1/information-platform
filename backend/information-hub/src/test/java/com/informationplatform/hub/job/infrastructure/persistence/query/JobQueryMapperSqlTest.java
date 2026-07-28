package com.informationplatform.hub.job.infrastructure.persistence.query;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.informationplatform.hub.job.application.JobQueryCriteria;
import com.informationplatform.hub.job.application.JobSortField;
import com.informationplatform.hub.job.application.SortDirection;
import java.util.Map;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class JobQueryMapperSqlTest {

    /** 职位分页查询 Mapper 方法的完整 MyBatis 标识。 */
    private static final String SELECT_JOBS =
            "com.informationplatform.hub.job.infrastructure.persistence.query"
                    + ".JobQueryMapper.selectJobs";

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @ParameterizedTest
    @CsvSource({
        "FIRST_SEEN_TIME, i.first_seen_time",
        "LAST_SEEN_TIME, i.last_seen_time",
        "PUBLISH_TIME, i.publish_time",
        "SALARY_MIN_MONTHLY_YUAN, j.salary_min_monthly_yuan"
    })
    void rendersOnlyWhitelistedOrderColumns(
            JobSortField sortField, String expectedColumn) {
        String sql = boundSql(criteria(sortField, SortDirection.ASC)).getSql();
        String normalized = normalize(sql);

        assertTrue(normalized.contains("ORDER BY " + expectedColumn + " ASC"));
        assertTrue(normalized.contains("i.id ASC"));
        assertFalse(sql.contains("${"));
    }

    @Test
    void rendersFiltersAsBoundParametersAndNeverSelectsRawPayload() {
        JobQueryCriteria criteria = new JobQueryCriteria(
                1,
                20,
                0,
                "keyword",
                "company",
                "成都",
                20000,
                30000,
                "BOSS",
                "ACTIVE",
                "REMOTE",
                JobSortField.FIRST_SEEN_TIME,
                SortDirection.DESC);

        BoundSql boundSql = boundSql(criteria);
        String sql = normalize(boundSql.getSql());

        assertTrue(sql.contains("LOCATE(?, i.title)"));
        assertTrue(sql.contains("j.salary_max_monthly_yuan >= ?"));
        assertTrue(sql.contains("j.salary_min_monthly_yuan <= ?"));
        assertTrue(sql.contains("LIMIT ? OFFSET ?"));
        assertFalse(sql.contains("keyword"));
        assertFalse(sql.contains("raw_payload"));
        assertFalse(sql.matches("(?s).*SELECT.*\\bi\\.content\\b.*FROM.*"));
    }

    private BoundSql boundSql(JobQueryCriteria criteria) {
        MappedStatement statement =
                sqlSessionFactory.getConfiguration().getMappedStatement(SELECT_JOBS);
        return statement.getBoundSql(Map.of("criteria", criteria));
    }

    private JobQueryCriteria criteria(
            JobSortField sortField, SortDirection direction) {
        return new JobQueryCriteria(
                1,
                20,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                sortField,
                direction);
    }

    private String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
