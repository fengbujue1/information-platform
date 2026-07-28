package com.informationplatform.hub.job.api;

import com.informationplatform.hub.common.api.ApiResponse;
import com.informationplatform.hub.job.api.dto.JobDetailResponse;
import com.informationplatform.hub.job.api.dto.JobListItemResponse;
import com.informationplatform.hub.job.api.dto.JobQueryRequest;
import com.informationplatform.hub.job.api.dto.JobSnapshotResponse;
import com.informationplatform.hub.job.api.dto.PageResponse;
import com.informationplatform.hub.job.application.JobQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobQueryController {

    /** 负责职位查询校验、数据库读取和安全响应映射的应用服务。 */
    private final JobQueryService queryService;

    public JobQueryController(JobQueryService queryService) {
        this.queryService = queryService;
    }

    /** 返回满足筛选和排序条件的职位分页摘要。 */
    @GetMapping
    public ApiResponse<PageResponse<JobListItemResponse>> list(
            @ModelAttribute JobQueryRequest request) {
        return ApiResponse.success("JOBS_FOUND", queryService.listJobs(request));
    }

    /** 返回指定平台信息主键对应的当前职位详情。 */
    @GetMapping("/{id}")
    public ApiResponse<JobDetailResponse> detail(@PathVariable("id") long informationId) {
        return ApiResponse.success("JOB_FOUND", queryService.getJob(informationId));
    }

    /** 返回指定职位按创建时间倒序排列的历史快照。 */
    @GetMapping("/{id}/snapshots")
    public ApiResponse<List<JobSnapshotResponse>> snapshots(
            @PathVariable("id") long informationId) {
        return ApiResponse.success(
                "JOB_SNAPSHOTS_FOUND",
                queryService.getSnapshots(informationId));
    }
}
