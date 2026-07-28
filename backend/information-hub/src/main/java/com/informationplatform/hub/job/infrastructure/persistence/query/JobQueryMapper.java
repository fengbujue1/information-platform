package com.informationplatform.hub.job.infrastructure.persistence.query;

import com.informationplatform.hub.job.application.JobQueryCriteria;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 执行职位列表、详情和历史快照的只读关联查询。 */
@Mapper
public interface JobQueryMapper {

    /** 统计满足筛选条件的职位总数。 */
    long countJobs(@Param("criteria") JobQueryCriteria criteria);

    /** 按白名单排序和分页条件查询职位摘要。 */
    List<JobQueryRow> selectJobs(@Param("criteria") JobQueryCriteria criteria);

    /** 按信息主键查询单个当前职位详情。 */
    JobQueryRow selectJobById(@Param("informationId") long informationId);

    /** 按创建时间倒序查询职位的全部版本快照。 */
    List<JobSnapshotQueryRow> selectSnapshots(
            @Param("informationId") long informationId);
}
