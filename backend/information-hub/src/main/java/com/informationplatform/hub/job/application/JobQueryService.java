package com.informationplatform.hub.job.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.job.api.dto.JobDetailResponse;
import com.informationplatform.hub.job.api.dto.JobListItemResponse;
import com.informationplatform.hub.job.api.dto.JobQueryRequest;
import com.informationplatform.hub.job.api.dto.JobSnapshotResponse;
import com.informationplatform.hub.job.api.dto.PageResponse;
import com.informationplatform.hub.job.infrastructure.persistence.query.JobQueryMapper;
import com.informationplatform.hub.job.infrastructure.persistence.query.JobQueryRow;
import com.informationplatform.hub.job.infrastructure.persistence.query.JobSnapshotQueryRow;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobQueryService {

    /** 默认从第一页开始查询。 */
    private static final int DEFAULT_PAGE = 1;

    /** 默认每页返回的职位数量。 */
    private static final int DEFAULT_SIZE = 20;

    /** 单页允许返回的最大职位数量。 */
    private static final int MAX_SIZE = 100;

    /** 防止异常长文本查询参数造成无意义的数据库负担。 */
    private static final int MAX_FILTER_LENGTH = 255;

    /** 执行固定参数和固定排序列的职位查询。 */
    private final JobQueryMapper queryMapper;

    /** 将数据库 JSON 字段转换为结构化响应。 */
    private final ObjectMapper objectMapper;

    public JobQueryService(JobQueryMapper queryMapper, ObjectMapper objectMapper) {
        this.queryMapper = queryMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 校验并执行职位分页查询。
     *
     * <p>统计和分页读取处于同一个只读事务，避免并发写入时返回互相矛盾的总数和列表。
     */
    @Transactional(readOnly = true)
    public PageResponse<JobListItemResponse> listJobs(JobQueryRequest request) {
        JobQueryCriteria criteria = normalize(request);
        try {
            long total = queryMapper.countJobs(criteria);
            List<JobListItemResponse> items = total == 0
                    ? List.of()
                    : queryMapper.selectJobs(criteria).stream()
                            .map(this::toListItem)
                            .toList();
            long totalPages = total == 0 ? 0 : ((total - 1) / criteria.size()) + 1;
            return new PageResponse<>(
                    criteria.page(),
                    criteria.size(),
                    total,
                    totalPages,
                    items);
        } catch (DataAccessException exception) {
            throw new JobQueryPersistenceException(exception);
        }
    }

    /** 查询当前职位详情，不存在时返回稳定的业务异常。 */
    @Transactional(readOnly = true)
    public JobDetailResponse getJob(long informationId) {
        requirePositiveId(informationId);
        try {
            JobQueryRow row = queryMapper.selectJobById(informationId);
            if (row == null) {
                throw new JobNotFoundException(informationId);
            }
            return toDetail(row);
        } catch (DataAccessException exception) {
            throw new JobQueryPersistenceException(exception);
        }
    }

    /** 校验职位存在后，按快照创建时间倒序返回全部历史版本。 */
    @Transactional(readOnly = true)
    public List<JobSnapshotResponse> getSnapshots(long informationId) {
        requirePositiveId(informationId);
        try {
            if (queryMapper.selectJobById(informationId) == null) {
                throw new JobNotFoundException(informationId);
            }
            return queryMapper.selectSnapshots(informationId).stream()
                    .map(this::toSnapshot)
                    .toList();
        } catch (DataAccessException exception) {
            throw new JobQueryPersistenceException(exception);
        }
    }

    /** 标准化可选筛选值并冻结排序枚举，防止外部字符串进入 ORDER BY。 */
    private JobQueryCriteria normalize(JobQueryRequest request) {
        int page = request.page() == null ? DEFAULT_PAGE : request.page();
        int size = request.size() == null ? DEFAULT_SIZE : request.size();
        if (page < 1) {
            throw new JobQueryRequestException(
                    "INVALID_JOB_PAGE", "page must be greater than or equal to 1");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new JobQueryRequestException(
                    "INVALID_JOB_PAGE_SIZE", "size must be between 1 and 100");
        }
        validateSalary(request.salaryMin(), "salaryMin");
        validateSalary(request.salaryMax(), "salaryMax");
        if (request.salaryMin() != null
                && request.salaryMax() != null
                && request.salaryMin() > request.salaryMax()) {
            throw new JobQueryRequestException(
                    "INVALID_JOB_SALARY_RANGE",
                    "salaryMin must be less than or equal to salaryMax");
        }

        String sortBy = normalizedOrDefault(request.sortBy(), "firstSeenTime");
        String sortDirection = normalizedOrDefault(request.sortDirection(), "desc");
        return new JobQueryCriteria(
                page,
                size,
                ((long) page - 1) * size,
                normalizeFilter(request.keyword(), "keyword"),
                normalizeFilter(request.company(), "company"),
                normalizeFilter(request.city(), "city"),
                request.salaryMin(),
                request.salaryMax(),
                normalizeFilter(request.source(), "source"),
                normalizeFilter(request.jobStatus(), "jobStatus"),
                normalizeFilter(request.remoteType(), "remoteType"),
                JobSortField.fromExternalName(sortBy),
                SortDirection.fromExternalName(sortDirection));
    }

    /** 将列表查询结果转换为不包含正文和 rawPayload 的轻量 DTO。 */
    private JobListItemResponse toListItem(JobQueryRow row) {
        return new JobListItemResponse(
                row.getId(),
                row.getSource(),
                row.getSourceItemId(),
                row.getSourceUrl(),
                row.getTitle(),
                row.getCompanyName(),
                row.getSalaryText(),
                row.getSalaryMinMonthlyYuan(),
                row.getSalaryMaxMonthlyYuan(),
                row.getSalaryMonths(),
                row.getLocationName(),
                row.getCityName(),
                row.getExperienceText(),
                row.getEducationText(),
                row.getRemoteType(),
                row.getJobStatus(),
                toInstant(row.getPublishTime()),
                toInstant(row.getFirstSeenTime()),
                toInstant(row.getLastSeenTime()),
                row.getCurrentVersionNo());
    }

    /** 将完整标准化关联记录转换为不包含 rawPayload 的详情 DTO。 */
    private JobDetailResponse toDetail(JobQueryRow row) {
        return new JobDetailResponse(
                row.getId(),
                row.getSource(),
                row.getSourceItemId(),
                row.getSourceUrl(),
                row.getTitle(),
                row.getContent(),
                toInstant(row.getPublishTime()),
                toInstant(row.getCollectedAt()),
                toInstant(row.getFirstSeenTime()),
                toInstant(row.getLastSeenTime()),
                row.getCurrentVersionNo(),
                row.getCollectorId(),
                row.getCollectorVersion(),
                row.getSourceCompanyId(),
                row.getSourceRecruiterId(),
                row.getCompanyName(),
                row.getCompanyUrl(),
                row.getCompanyScaleText(),
                row.getCompanyStageText(),
                row.getCompanyIndustryText(),
                row.getSalaryText(),
                row.getSalarySource(),
                row.getSalaryMinMonthlyYuan(),
                row.getSalaryMaxMonthlyYuan(),
                row.getSalaryMonths(),
                row.getLocationName(),
                row.getCityName(),
                row.getAreaName(),
                row.getBusinessDistrictName(),
                row.getExperienceText(),
                row.getEducationText(),
                row.getRecruiterName(),
                row.getRecruiterTitle(),
                row.getRecruiterActiveText(),
                row.getRemoteType(),
                row.getJobStatus(),
                row.getDetailStatus(),
                toInstant(row.getDetailCollectedAt()),
                parseJson(row.getSourceTags()),
                parseJson(row.getSourceSkillTags()),
                parseJson(row.getWelfare()));
    }

    /** 将快照记录转换为安全响应，刻意不读取或返回 rawPayload。 */
    private JobSnapshotResponse toSnapshot(JobSnapshotQueryRow row) {
        return new JobSnapshotResponse(
                row.getId(),
                row.getVersionNo(),
                row.getContentHash(),
                row.getTitle(),
                row.getContent(),
                parseJson(row.getStandardizedPayload()),
                toInstant(row.getCollectedAt()),
                row.getCollectorId(),
                row.getCollectorVersion(),
                toInstant(row.getCreatedAt()));
    }

    private void requirePositiveId(long informationId) {
        if (informationId < 1) {
            throw new JobQueryRequestException(
                    "INVALID_JOB_ID", "Job id must be greater than or equal to 1");
        }
    }

    private void validateSalary(Integer value, String field) {
        if (value != null && value < 0) {
            throw new JobQueryRequestException(
                    "INVALID_JOB_SALARY", field + " must be greater than or equal to 0");
        }
    }

    private String normalizeFilter(String value, String field) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > MAX_FILTER_LENGTH) {
            throw new JobQueryRequestException(
                    "INVALID_JOB_FILTER", field + " must not exceed 255 characters");
        }
        return normalized;
    }

    private String normalizedOrDefault(String value, String defaultValue) {
        String normalized = normalizeFilter(value, "sort");
        return normalized == null ? defaultValue : normalized;
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private JsonNode parseJson(String value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new JobQueryPersistenceException(exception);
        }
    }
}
