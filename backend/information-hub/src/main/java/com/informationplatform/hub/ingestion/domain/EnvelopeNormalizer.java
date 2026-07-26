package com.informationplatform.hub.ingestion.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.informationplatform.hub.ingestion.api.dto.InformationEnvelopeRequest;
import com.informationplatform.hub.ingestion.api.dto.JobExtensionRequest;
import com.informationplatform.hub.ingestion.application.IngestionRequestException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EnvelopeNormalizer {

    /** 校验第一阶段协议边界，并将请求转换为统一的领域内容。 */
    public ArchiveContent normalize(InformationEnvelopeRequest request) {
        // DTO 注解只负责结构校验，这里继续校验阶段限定的协议枚举和 JSON 形态。
        validateEnvelopeBoundary(request);

        // 通用字段统一去除无意义空白，并把带时区时间转换为 Instant。
        InformationFields information = new InformationFields(
                request.schemaVersion(),
                request.informationType(),
                request.source(),
                request.sourceItemId(),
                normalizeOptionalText(request.sourceUrl()),
                normalizeRequiredText(request.title(), "title"),
                normalizeOptionalText(request.content()),
                request.publishTime() == null ? null : request.publishTime().toInstant(),
                request.collectedAt().toInstant(),
                normalizeRequiredText(request.collector().collectorId(), "collectorId"),
                normalizeRequiredText(request.collector().collectorVersion(), "collectorVersion"),
                copy(request.collectionContext()),
                copy(request.rawPayload()));

        JobExtensionRequest extension = request.extension();
        // 职位扩展与通用字段采用相同的空值和文本归一化规则。
        JobFields job = new JobFields(
                normalizeOptionalText(extension.sourceCompanyId()),
                normalizeOptionalText(extension.sourceRecruiterId()),
                normalizeOptionalText(extension.companyName()),
                normalizeOptionalText(extension.companyUrl()),
                normalizeOptionalText(extension.companyScaleText()),
                normalizeOptionalText(extension.companyStageText()),
                normalizeOptionalText(extension.companyIndustryText()),
                normalizeOptionalText(extension.salaryText()),
                normalizeOptionalText(extension.salarySource()),
                extension.salaryMinMonthlyYuan(),
                extension.salaryMaxMonthlyYuan(),
                extension.salaryMonths(),
                normalizeOptionalText(extension.locationName()),
                normalizeOptionalText(extension.cityName()),
                normalizeOptionalText(extension.areaName()),
                normalizeOptionalText(extension.businessDistrictName()),
                normalizeOptionalText(extension.experienceText()),
                normalizeOptionalText(extension.educationText()),
                normalizeOptionalText(extension.recruiterName()),
                normalizeOptionalText(extension.recruiterTitle()),
                normalizeOptionalText(extension.recruiterActiveText()),
                normalizeOptionalText(extension.remoteType()),
                normalizeOptionalText(extension.jobStatus()),
                normalizeOptionalText(extension.detailStatus()),
                extension.detailCollectedAt() == null
                        ? null
                        : extension.detailCollectedAt().toInstant(),
                normalizeList(extension.sourceTags()),
                normalizeList(extension.sourceSkillTags()),
                normalizeList(extension.welfare()));

        return new ArchiveContent(information, job);
    }

    /** 校验 Phase 1 当前允许的协议版本、信息类型、来源和 JSON 对象结构。 */
    private void validateEnvelopeBoundary(InformationEnvelopeRequest request) {
        if (request.schemaVersion() != 1) {
            throw new IngestionRequestException(
                    "UNSUPPORTED_SCHEMA_VERSION", "Only schemaVersion 1 is supported");
        }
        if (!"JOB".equals(request.informationType())) {
            throw new IngestionRequestException(
                    "UNSUPPORTED_INFORMATION_TYPE", "Only informationType JOB is supported");
        }
        if (!"BOSS".equals(request.source())) {
            throw new IngestionRequestException(
                    "UNSUPPORTED_SOURCE", "Only source BOSS is supported in Phase 1");
        }
        if (!request.rawPayload().isObject()) {
            throw new IngestionRequestException(
                    "INVALID_RAW_PAYLOAD", "rawPayload must be a JSON object");
        }
        if (request.collectionContext() != null && !request.collectionContext().isObject()) {
            throw new IngestionRequestException(
                    "INVALID_COLLECTION_CONTEXT", "collectionContext must be a JSON object");
        }
    }

    /** 归一化必填文本，并在文本归一化为空时返回稳定参数错误。 */
    private String normalizeRequiredText(String value, String fieldName) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new IngestionRequestException(
                    "VALIDATION_FAILED", fieldName + " must not be blank");
        }
        return normalized;
    }

    /** 统一换行符、删除行尾空白和整体首尾空白，并把空文本转换为 null。 */
    static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalizedLineEndings = value.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalizedLineEndings.split("\n", -1);
        List<String> normalizedLines = new ArrayList<>(lines.length);
        // 保留正文内部换行，仅删除每一行不具业务意义的尾部空白。
        for (String line : lines) {
            normalizedLines.add(line.stripTrailing());
        }
        String normalized = String.join("\n", normalizedLines).strip();
        return normalized.isEmpty() ? null : normalized;
    }

    /** 按首次出现顺序去重列表元素，并过滤归一化后的空值。 */
    private List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String item = normalizeOptionalText(value);
            if (item != null) {
                normalized.add(item);
            }
        }
        return normalized.isEmpty() ? null : List.copyOf(normalized);
    }

    /** 深拷贝请求 JSON，避免后续处理意外修改 Jackson 请求树。 */
    private JsonNode copy(JsonNode node) {
        return node == null ? null : node.deepCopy();
    }
}
