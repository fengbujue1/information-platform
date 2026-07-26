package com.informationplatform.hub.ingestion.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

@Component
public class CanonicalContentHasher {

    /** 用于构造标准化 JSON 和稳定序列化结果。 */
    private final ObjectMapper objectMapper;

    public CanonicalContentHasher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 生成用于幂等版本判断的哈希以及用于快照追溯的标准化数据。 */
    public ContentFingerprint fingerprint(ArchiveContent content) {
        // 快照保留可读标准化结构，哈希使用独立的稳定键排序结构。
        JsonNode standardizedPayload = standardizedPayload(content);
        byte[] canonicalJson = canonicalJson(content);
        return new ContentFingerprint(sha256(canonicalJson), standardizedPayload);
    }

    /**
     * 构造参与版本比较的稳定 JSON。
     *
     * <p>排除采集元数据、rawPayload、salarySource 和详情采集状态，数组排序去重以消除顺序噪声。
     */
    private byte[] canonicalJson(ArchiveContent content) {
        InformationFields information = content.information();
        JobFields job = content.job();
        // TreeMap 固定字段顺序，确保相同业务内容在不同 JVM 调用中得到相同字节序列。
        Map<String, Object> values = new TreeMap<>();
        values.put("area_name", canonical(job.areaName()));
        values.put("business_district_name", canonical(job.businessDistrictName()));
        values.put("city_name", canonical(job.cityName()));
        values.put("company_industry_text", canonical(job.companyIndustryText()));
        values.put("company_name", canonical(job.companyName()));
        values.put("company_scale_text", canonical(job.companyScaleText()));
        values.put("company_stage_text", canonical(job.companyStageText()));
        values.put("company_url", canonical(job.companyUrl()));
        values.put("content", canonical(information.content()));
        values.put("education_text", canonical(job.educationText()));
        values.put("experience_text", canonical(job.experienceText()));
        values.put("job_status", canonical(job.jobStatus()));
        values.put("location_name", canonical(job.locationName()));
        values.put("recruiter_active_text", canonical(job.recruiterActiveText()));
        values.put("recruiter_name", canonical(job.recruiterName()));
        values.put("recruiter_title", canonical(job.recruiterTitle()));
        values.put("remote_type", canonical(job.remoteType()));
        values.put("salary_max_monthly_yuan", job.salaryMaxMonthlyYuan());
        values.put("salary_min_monthly_yuan", job.salaryMinMonthlyYuan());
        values.put("salary_months", job.salaryMonths());
        values.put("salary_text", canonical(job.salaryText()));
        values.put("source_company_id", canonical(job.sourceCompanyId()));
        values.put("source_recruiter_id", canonical(job.sourceRecruiterId()));
        values.put("source_skill_tags", canonicalArray(job.sourceSkillTags()));
        values.put("source_tags", canonicalArray(job.sourceTags()));
        values.put("source_url", canonical(information.sourceUrl()));
        values.put("title", canonical(information.title()));
        values.put("welfare", canonicalArray(job.welfare()));
        try {
            return objectMapper.writeValueAsBytes(values);
        } catch (Exception exception) {
            throw new IllegalStateException("Canonical JSON could not be generated", exception);
        }
    }

    /** 构造快照中保存的通用信息与职位扩展标准化结构。 */
    private JsonNode standardizedPayload(ArchiveContent content) {
        InformationFields information = content.information();
        JobFields job = content.job();
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode common = root.putObject("information");
        common.put("schemaVersion", information.schemaVersion());
        common.put("informationType", information.informationType());
        common.put("source", information.source());
        common.put("sourceItemId", information.sourceItemId());
        put(common, "sourceUrl", information.sourceUrl());
        put(common, "title", information.title());
        put(common, "content", information.content());
        put(common, "publishTime", information.publishTime());

        ObjectNode extension = root.putObject("job");
        put(extension, "sourceCompanyId", job.sourceCompanyId());
        put(extension, "sourceRecruiterId", job.sourceRecruiterId());
        put(extension, "companyName", job.companyName());
        put(extension, "companyUrl", job.companyUrl());
        put(extension, "companyScaleText", job.companyScaleText());
        put(extension, "companyStageText", job.companyStageText());
        put(extension, "companyIndustryText", job.companyIndustryText());
        put(extension, "salaryText", job.salaryText());
        put(extension, "salarySource", job.salarySource());
        put(extension, "salaryMinMonthlyYuan", job.salaryMinMonthlyYuan());
        put(extension, "salaryMaxMonthlyYuan", job.salaryMaxMonthlyYuan());
        put(extension, "salaryMonths", job.salaryMonths());
        put(extension, "locationName", job.locationName());
        put(extension, "cityName", job.cityName());
        put(extension, "areaName", job.areaName());
        put(extension, "businessDistrictName", job.businessDistrictName());
        put(extension, "experienceText", job.experienceText());
        put(extension, "educationText", job.educationText());
        put(extension, "recruiterName", job.recruiterName());
        put(extension, "recruiterTitle", job.recruiterTitle());
        put(extension, "recruiterActiveText", job.recruiterActiveText());
        put(extension, "remoteType", job.remoteType());
        put(extension, "jobStatus", job.jobStatus());
        put(extension, "detailStatus", job.detailStatus());
        put(extension, "detailCollectedAt", job.detailCollectedAt());
        put(extension, "sourceTags", job.sourceTags());
        put(extension, "sourceSkillTags", job.sourceSkillTags());
        put(extension, "welfare", job.welfare());
        return root;
    }

    /** 将可选文本转换为统一的哈希输入表达。 */
    private String canonical(String value) {
        return EnvelopeNormalizer.normalizeOptionalText(value);
    }

    /** 对数组元素归一化、去重和排序，避免来源顺序变化触发新版本。 */
    private List<String> canonicalArray(List<String> values) {
        if (values == null) {
            return null;
        }
        TreeSet<String> sorted = new TreeSet<>(Comparator.naturalOrder());
        for (String value : values) {
            String canonical = canonical(value);
            if (canonical != null) {
                sorted.add(canonical);
            }
        }
        return new ArrayList<>(sorted);
    }

    /** 计算规范 JSON 的 SHA-256 十六进制摘要。 */
    private String sha256(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void put(ObjectNode node, String name, String value) {
        if (value == null) {
            node.putNull(name);
        } else {
            node.put(name, value);
        }
    }

    private void put(ObjectNode node, String name, Integer value) {
        if (value == null) {
            node.putNull(name);
        } else {
            node.put(name, value);
        }
    }

    private void put(ObjectNode node, String name, Instant value) {
        if (value == null) {
            node.putNull(name);
        } else {
            node.put(name, value.toString());
        }
    }

    private void put(ObjectNode node, String name, List<String> values) {
        if (values == null) {
            node.putNull(name);
            return;
        }
        ArrayNode array = node.putArray(name);
        values.forEach(array::add);
    }
}
