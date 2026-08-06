package com.informationplatform.hub.analysis.usage.infrastructure.persistence;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 查询当前 Owner 的 Provider Invocation Actual Usage 聚合。 */
@Mapper
public interface AnalysisUsageMapper {

    /** 按 Owner 聚合全部 Provider Invocation Actual Usage。 */
    @Select("""
            SELECT
                COUNT(*) AS invocation_count,
                SUM(CASE WHEN usage_status = 'REPORTED' THEN 1 ELSE 0 END)
                    AS reported_invocation_count,
                SUM(CASE WHEN usage_status <> 'REPORTED' THEN 1 ELSE 0 END)
                    AS unavailable_invocation_count,
                SUM(CASE WHEN usage_status = 'REPORTED'
                    THEN input_tokens ELSE NULL END) AS input_tokens,
                SUM(CASE WHEN usage_status = 'REPORTED'
                    THEN output_tokens ELSE NULL END) AS output_tokens,
                SUM(CASE WHEN usage_status = 'REPORTED'
                    THEN total_tokens ELSE NULL END) AS total_tokens
            FROM ai_model_invocation
            WHERE user_id = #{userId}
            """)
    UserUsageAggregateRow aggregateUserUsage(@Param("userId") long userId);

    /** 按 Owner 和 UTC 半开区间聚合 Provider Invocation Actual Usage。 */
    @Select("""
            SELECT
                COUNT(*) AS invocation_count,
                SUM(CASE WHEN usage_status = 'REPORTED' THEN 1 ELSE 0 END)
                    AS reported_invocation_count,
                SUM(CASE WHEN usage_status <> 'REPORTED' THEN 1 ELSE 0 END)
                    AS unavailable_invocation_count,
                SUM(CASE WHEN usage_status = 'REPORTED'
                    THEN input_tokens ELSE NULL END) AS input_tokens,
                SUM(CASE WHEN usage_status = 'REPORTED'
                    THEN output_tokens ELSE NULL END) AS output_tokens,
                SUM(CASE WHEN usage_status = 'REPORTED'
                    THEN total_tokens ELSE NULL END) AS total_tokens
            FROM ai_model_invocation
            WHERE user_id = #{userId}
              AND created_at >= #{periodStart}
              AND created_at < #{periodEnd}
            """)
    UserUsageAggregateRow aggregateUserUsageBetween(
            @Param("userId") long userId,
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd);
}
