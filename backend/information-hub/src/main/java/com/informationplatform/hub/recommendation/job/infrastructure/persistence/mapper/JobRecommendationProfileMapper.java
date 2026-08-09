package com.informationplatform.hub.recommendation.job.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.po.JobRecommendationProfilePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 提供 JOB Recommendation Profile 扩展的基础持久化能力。 */
@Mapper
public interface JobRecommendationProfileMapper extends BaseMapper<JobRecommendationProfilePo> {

    /** 完整替换 JOB Extension，显式允许将可空薪资清空。 */
    @Update("""
            UPDATE job_recommendation_profile
            SET target_roles = #{targetRoles},
                preferred_skills = #{preferredSkills},
                preferred_cities = #{preferredCities},
                preferred_remote_types = #{preferredRemoteTypes},
                salary_min_monthly_yuan = #{salaryMinMonthlyYuan},
                excluded_keywords = #{excludedKeywords}
            WHERE profile_id = #{profileId}
            """)
    int updateFullProfile(
            @Param("profileId") long profileId,
            @Param("targetRoles") String targetRoles,
            @Param("preferredSkills") String preferredSkills,
            @Param("preferredCities") String preferredCities,
            @Param("preferredRemoteTypes") String preferredRemoteTypes,
            @Param("salaryMinMonthlyYuan") Integer salaryMinMonthlyYuan,
            @Param("excludedKeywords") String excludedKeywords);
}
