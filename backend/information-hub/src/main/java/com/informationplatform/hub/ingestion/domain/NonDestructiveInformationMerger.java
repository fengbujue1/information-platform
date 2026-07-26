package com.informationplatform.hub.ingestion.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NonDestructiveInformationMerger {

    /**
     * 按非破坏性规则合并当前数据与新采集数据。
     *
     * <p>新值缺失、为空或为空数组时保留历史有效值；rawPayload 始终保留最近一次完整上报。
     */
    public ArchiveContent merge(ArchiveContent current, ArchiveContent incoming) {
        if (current == null) {
            // 首次写入没有历史值可合并，只需要补齐平台默认状态。
            return applyNewItemDefaults(incoming);
        }

        InformationFields oldInformation = current.information();
        InformationFields newInformation = incoming.information();
        // 幂等键保持历史值，业务字段优先采用本次有效值，原始数据采用本次完整上报。
        InformationFields information = new InformationFields(
                newInformation.schemaVersion(),
                oldInformation.informationType(),
                oldInformation.source(),
                oldInformation.sourceItemId(),
                choose(newInformation.sourceUrl(), oldInformation.sourceUrl()),
                choose(newInformation.title(), oldInformation.title()),
                choose(newInformation.content(), oldInformation.content()),
                choose(newInformation.publishTime(), oldInformation.publishTime()),
                newInformation.collectedAt(),
                newInformation.collectorId(),
                newInformation.collectorVersion(),
                chooseJson(newInformation.collectionContext(), oldInformation.collectionContext()),
                newInformation.rawPayload());

        JobFields oldJob = current.job();
        JobFields newJob = incoming.job();
        // 职位扩展逐字段合并，防止列表页的稀疏数据清空详情页数据。
        JobFields job = new JobFields(
                choose(newJob.sourceCompanyId(), oldJob.sourceCompanyId()),
                choose(newJob.sourceRecruiterId(), oldJob.sourceRecruiterId()),
                choose(newJob.companyName(), oldJob.companyName()),
                choose(newJob.companyUrl(), oldJob.companyUrl()),
                choose(newJob.companyScaleText(), oldJob.companyScaleText()),
                choose(newJob.companyStageText(), oldJob.companyStageText()),
                choose(newJob.companyIndustryText(), oldJob.companyIndustryText()),
                choose(newJob.salaryText(), oldJob.salaryText()),
                choose(newJob.salarySource(), oldJob.salarySource()),
                choose(newJob.salaryMinMonthlyYuan(), oldJob.salaryMinMonthlyYuan()),
                choose(newJob.salaryMaxMonthlyYuan(), oldJob.salaryMaxMonthlyYuan()),
                choose(newJob.salaryMonths(), oldJob.salaryMonths()),
                choose(newJob.locationName(), oldJob.locationName()),
                choose(newJob.cityName(), oldJob.cityName()),
                choose(newJob.areaName(), oldJob.areaName()),
                choose(newJob.businessDistrictName(), oldJob.businessDistrictName()),
                choose(newJob.experienceText(), oldJob.experienceText()),
                choose(newJob.educationText(), oldJob.educationText()),
                choose(newJob.recruiterName(), oldJob.recruiterName()),
                choose(newJob.recruiterTitle(), oldJob.recruiterTitle()),
                choose(newJob.recruiterActiveText(), oldJob.recruiterActiveText()),
                choose(newJob.remoteType(), oldJob.remoteType()),
                choose(newJob.jobStatus(), oldJob.jobStatus()),
                choose(newJob.detailStatus(), oldJob.detailStatus()),
                choose(newJob.detailCollectedAt(), oldJob.detailCollectedAt()),
                chooseList(newJob.sourceTags(), oldJob.sourceTags()),
                chooseList(newJob.sourceSkillTags(), oldJob.sourceSkillTags()),
                chooseList(newJob.welfare(), oldJob.welfare()));

        return new ArchiveContent(information, job);
    }

    /** 为首次出现的职位补齐 UNKNOWN 状态，避免把未知状态误判为业务状态。 */
    private ArchiveContent applyNewItemDefaults(ArchiveContent incoming) {
        JobFields job = incoming.job();
        JobFields withDefaults = new JobFields(
                job.sourceCompanyId(),
                job.sourceRecruiterId(),
                job.companyName(),
                job.companyUrl(),
                job.companyScaleText(),
                job.companyStageText(),
                job.companyIndustryText(),
                job.salaryText(),
                job.salarySource(),
                job.salaryMinMonthlyYuan(),
                job.salaryMaxMonthlyYuan(),
                job.salaryMonths(),
                job.locationName(),
                job.cityName(),
                job.areaName(),
                job.businessDistrictName(),
                job.experienceText(),
                job.educationText(),
                job.recruiterName(),
                job.recruiterTitle(),
                job.recruiterActiveText(),
                choose(job.remoteType(), "UNKNOWN"),
                choose(job.jobStatus(), "UNKNOWN"),
                choose(job.detailStatus(), "UNKNOWN"),
                job.detailCollectedAt(),
                job.sourceTags(),
                job.sourceSkillTags(),
                job.welfare());
        return new ArchiveContent(incoming.information(), withDefaults);
    }

    /** 新值存在时采用新值，否则保留当前值。 */
    private <T> T choose(T incoming, T current) {
        return incoming == null ? current : incoming;
    }

    /** JSON 新值缺失或显式为 null 时保留当前值。 */
    private JsonNode chooseJson(JsonNode incoming, JsonNode current) {
        return incoming == null || incoming.isNull() ? current : incoming;
    }

    /** 列表新值缺失或为空时保留当前值。 */
    private List<String> chooseList(List<String> incoming, List<String> current) {
        return incoming == null || incoming.isEmpty() ? current : incoming;
    }
}
