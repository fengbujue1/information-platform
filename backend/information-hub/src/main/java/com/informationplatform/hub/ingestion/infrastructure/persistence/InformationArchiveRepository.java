package com.informationplatform.hub.ingestion.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.information.infrastructure.persistence.mapper.InformationItemMapper;
import com.informationplatform.hub.information.infrastructure.persistence.mapper.InformationSnapshotMapper;
import com.informationplatform.hub.information.infrastructure.persistence.po.InformationItemPo;
import com.informationplatform.hub.information.infrastructure.persistence.po.InformationSnapshotPo;
import com.informationplatform.hub.ingestion.domain.ArchiveContent;
import com.informationplatform.hub.ingestion.domain.ArchiveState;
import com.informationplatform.hub.ingestion.domain.ContentFingerprint;
import com.informationplatform.hub.ingestion.domain.InformationFields;
import com.informationplatform.hub.ingestion.domain.JobFields;
import com.informationplatform.hub.job.infrastructure.persistence.mapper.JobInformationMapper;
import com.informationplatform.hub.job.infrastructure.persistence.po.JobInformationPo;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InformationArchiveRepository {

    /** 将数据库 JSON 数组反序列化为字符串列表的类型信息。 */
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    /** 信息主表 Mapper。 */
    private final InformationItemMapper informationItemMapper;

    /** 职位扩展表 Mapper。 */
    private final JobInformationMapper jobInformationMapper;

    /** 信息版本快照表 Mapper。 */
    private final InformationSnapshotMapper informationSnapshotMapper;

    /** 负责领域对象与数据库 JSON 字符串之间的转换。 */
    private final ObjectMapper objectMapper;

    public InformationArchiveRepository(
            InformationItemMapper informationItemMapper,
            JobInformationMapper jobInformationMapper,
            InformationSnapshotMapper informationSnapshotMapper,
            ObjectMapper objectMapper) {
        this.informationItemMapper = informationItemMapper;
        this.jobInformationMapper = jobInformationMapper;
        this.informationSnapshotMapper = informationSnapshotMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 按幂等键锁定并读取当前归档状态。
     *
     * <p>调用方事务提交前行锁持续有效，保证同一信息的合并和版本递增串行执行。
     */
    public Optional<ArchiveState> findForUpdate(
            String source, String informationType, String sourceItemId) {
        // 先锁定信息主表记录，再用同一主键读取职位扩展。
        InformationItemPo information = informationItemMapper.selectByIdentityForUpdate(
                source, informationType, sourceItemId);
        if (information == null) {
            return Optional.empty();
        }
        JobInformationPo job = jobInformationMapper.selectById(information.getId());
        return Optional.of(toArchiveState(information, job));
    }

    /** 插入信息主记录并返回数据库生成的主键。 */
    public long insertInformation(
            ArchiveContent content,
            ContentFingerprint fingerprint,
            int versionNo,
            Instant serverTime) {
        InformationItemPo po = toInformationItemPo(content, fingerprint, versionNo);
        po.setFirstSeenTime(toUtc(serverTime));
        po.setLastSeenTime(toUtc(serverTime));
        // MyBatis-Plus 会把数据库自增主键回填到 PO。
        requireSingleRow(informationItemMapper.insert(po), "information_item insert");
        if (po.getId() == null) {
            throw new IllegalStateException("Information item insert did not return an ID");
        }
        return po.getId();
    }

    /** 更新当前信息内容、版本和最近发现时间，不修改首次发现时间。 */
    public void updateInformation(
            long informationId,
            ArchiveContent content,
            ContentFingerprint fingerprint,
            int versionNo,
            Instant serverTime) {
        InformationItemPo po = toInformationItemPo(content, fingerprint, versionNo);
        po.setId(informationId);
        po.setLastSeenTime(toUtc(serverTime));
        requireSingleRow(informationItemMapper.updateById(po), "information_item update");
    }

    /** 按 informationId 插入或更新职位扩展数据。 */
    public void saveJob(long informationId, JobFields job) {
        JobInformationPo po = toJobInformationPo(informationId, job);
        // 主表行已由调用方锁定，因此同一信息的职位扩展写入不会并发交错。
        if (jobInformationMapper.selectById(informationId) == null) {
            requireSingleRow(jobInformationMapper.insert(po), "job_information insert");
        } else {
            int updated = jobInformationMapper.updateById(po);
            if (updated < 0 || updated > 1) {
                throw new IllegalStateException(
                        "job_information update affected " + updated + " rows");
            }
        }
    }

    /** 追加不可变的信息版本快照。 */
    public void insertSnapshot(
            long informationId,
            int versionNo,
            ArchiveContent content,
            ContentFingerprint fingerprint) {
        InformationFields information = content.information();
        InformationSnapshotPo po = new InformationSnapshotPo();
        po.setInformationId(informationId);
        po.setVersionNo(versionNo);
        po.setContentHash(fingerprint.hash());
        po.setTitle(information.title());
        po.setContent(information.content());
        po.setStandardizedPayload(json(fingerprint.standardizedPayload()));
        po.setRawPayload(json(information.rawPayload()));
        po.setCollectedAt(toUtc(information.collectedAt()));
        po.setCollectorId(information.collectorId());
        po.setCollectorVersion(information.collectorVersion());
        // 快照只插入不更新，唯一约束保证同一信息版本只能存在一次。
        requireSingleRow(
                informationSnapshotMapper.insert(po), "information_snapshot insert");
    }

    /** 将归档领域对象映射为信息主表 PO。 */
    private InformationItemPo toInformationItemPo(
            ArchiveContent content, ContentFingerprint fingerprint, int versionNo) {
        InformationFields information = content.information();
        InformationItemPo po = new InformationItemPo();
        po.setInformationType(information.informationType());
        po.setSource(information.source());
        po.setSourceItemId(information.sourceItemId());
        po.setSourceUrl(information.sourceUrl());
        po.setTitle(information.title());
        po.setContent(information.content());
        po.setPublishTime(toUtc(information.publishTime()));
        po.setCollectedAt(toUtc(information.collectedAt()));
        po.setContentHash(fingerprint.hash());
        po.setCurrentVersionNo(versionNo);
        po.setRawPayload(json(information.rawPayload()));
        po.setSchemaVersion(information.schemaVersion());
        po.setCollectorId(information.collectorId());
        po.setCollectorVersion(information.collectorVersion());
        po.setCollectionContext(jsonOrNull(information.collectionContext()));
        return po;
    }

    /** 将职位领域字段映射为职位扩展表 PO。 */
    private JobInformationPo toJobInformationPo(long informationId, JobFields job) {
        JobInformationPo po = new JobInformationPo();
        po.setInformationId(informationId);
        po.setSourceCompanyId(job.sourceCompanyId());
        po.setSourceRecruiterId(job.sourceRecruiterId());
        po.setCompanyName(job.companyName());
        po.setCompanyUrl(job.companyUrl());
        po.setCompanyScaleText(job.companyScaleText());
        po.setCompanyStageText(job.companyStageText());
        po.setCompanyIndustryText(job.companyIndustryText());
        po.setSalaryText(job.salaryText());
        po.setSalarySource(job.salarySource());
        po.setSalaryMinMonthlyYuan(job.salaryMinMonthlyYuan());
        po.setSalaryMaxMonthlyYuan(job.salaryMaxMonthlyYuan());
        po.setSalaryMonths(job.salaryMonths());
        po.setLocationName(job.locationName());
        po.setCityName(job.cityName());
        po.setAreaName(job.areaName());
        po.setBusinessDistrictName(job.businessDistrictName());
        po.setExperienceText(job.experienceText());
        po.setEducationText(job.educationText());
        po.setRecruiterName(job.recruiterName());
        po.setRecruiterTitle(job.recruiterTitle());
        po.setRecruiterActiveText(job.recruiterActiveText());
        po.setRemoteType(job.remoteType());
        po.setJobStatus(job.jobStatus());
        po.setDetailStatus(job.detailStatus());
        po.setDetailCollectedAt(toUtc(job.detailCollectedAt()));
        po.setSourceTags(jsonOrNull(job.sourceTags()));
        po.setSourceSkillTags(jsonOrNull(job.sourceSkillTags()));
        po.setWelfare(jsonOrNull(job.welfare()));
        return po;
    }

    /** 将主表和职位扩展表数据重建为当前归档状态。 */
    private ArchiveState toArchiveState(
            InformationItemPo information, JobInformationPo jobPo) {
        InformationFields informationFields = new InformationFields(
                information.getSchemaVersion(),
                information.getInformationType(),
                information.getSource(),
                information.getSourceItemId(),
                information.getSourceUrl(),
                information.getTitle(),
                information.getContent(),
                toInstant(information.getPublishTime()),
                toInstant(information.getCollectedAt()),
                information.getCollectorId(),
                information.getCollectorVersion(),
                jsonNode(information.getCollectionContext()),
                jsonNode(information.getRawPayload()));
        // 历史异常数据缺少职位扩展时使用空对象，交给合并规则补齐。
        JobFields jobFields = jobPo == null ? emptyJob() : toJobFields(jobPo);
        return new ArchiveState(
                information.getId(),
                information.getCurrentVersionNo(),
                information.getContentHash(),
                new ArchiveContent(informationFields, jobFields));
    }

    /** 将职位扩展 PO 转换为领域字段，并兼容数据库中的空状态。 */
    private JobFields toJobFields(JobInformationPo po) {
        return new JobFields(
                po.getSourceCompanyId(),
                po.getSourceRecruiterId(),
                po.getCompanyName(),
                po.getCompanyUrl(),
                po.getCompanyScaleText(),
                po.getCompanyStageText(),
                po.getCompanyIndustryText(),
                po.getSalaryText(),
                po.getSalarySource(),
                po.getSalaryMinMonthlyYuan(),
                po.getSalaryMaxMonthlyYuan(),
                po.getSalaryMonths(),
                po.getLocationName(),
                po.getCityName(),
                po.getAreaName(),
                po.getBusinessDistrictName(),
                po.getExperienceText(),
                po.getEducationText(),
                po.getRecruiterName(),
                po.getRecruiterTitle(),
                po.getRecruiterActiveText(),
                defaultValue(po.getRemoteType(), "UNKNOWN"),
                defaultValue(po.getJobStatus(), "UNKNOWN"),
                defaultValue(po.getDetailStatus(), "UNKNOWN"),
                toInstant(po.getDetailCollectedAt()),
                stringList(po.getSourceTags()),
                stringList(po.getSourceSkillTags()),
                stringList(po.getWelfare()));
    }

    /** 构造缺少职位扩展时使用的空领域对象。 */
    private JobFields emptyJob() {
        return new JobFields(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "UNKNOWN",
                "UNKNOWN",
                "UNKNOWN",
                null,
                null,
                null,
                null);
    }

    /** 将对象序列化为数据库 JSON 字符串。 */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("JSON value could not be serialized", exception);
        }
    }

    /** 将可选对象序列化为数据库 JSON 字符串。 */
    private String jsonOrNull(Object value) {
        return value == null ? null : json(value);
    }

    /** 将数据库 JSON 字符串反序列化为 Jackson 节点。 */
    private JsonNode jsonNode(String value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored JSON value could not be read", exception);
        }
    }

    /** 将数据库 JSON 数组字符串反序列化为字符串列表。 */
    private List<String> stringList(String value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored JSON array could not be read", exception);
        }
    }

    /** 将领域层 Instant 按 UTC 转换为数据库时间类型。 */
    private LocalDateTime toUtc(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    /** 将数据库时间按 UTC 转换为领域层 Instant。 */
    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    /** 数据库状态为空时返回兼容默认值。 */
    private String defaultValue(String value, String fallback) {
        return value == null ? fallback : value;
    }

    /** 校验必须且只能影响一行的数据库操作结果。 */
    private void requireSingleRow(int updated, String operation) {
        if (updated != 1) {
            throw new IllegalStateException(operation + " affected " + updated + " rows");
        }
    }
}
