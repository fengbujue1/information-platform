package com.informationplatform.hub.analysis.definition.job;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record JobUserRelevanceInput(
        /** 快照冻结的职位标题。 */
        String title,
        /** 快照冻结的职位正文。 */
        String content,
        /** 公司名称。 */
        String companyName,
        /** 来源薪资文本。 */
        String salaryText,
        /** 来源展示的完整地点。 */
        String locationName,
        /** 工作城市。 */
        String cityName,
        /** 工作经验要求文本。 */
        String experienceText,
        /** 学历要求文本。 */
        String educationText,
        /** 办公方式。 */
        String remoteType,
        /** 来源职位状态。 */
        String jobStatus,
        /** 来源职位标签；缺失或显式 null 时保持 null。 */
        List<String> sourceTags,
        /** 来源技能标签；缺失或显式 null 时保持 null。 */
        List<String> sourceSkillTags,
        /** 来源福利标签；缺失或显式 null 时保持 null。 */
        List<String> welfare) {

    public JobUserRelevanceInput {
        sourceTags = immutableNullableCopy(sourceTags);
        sourceSkillTags = immutableNullableCopy(sourceSkillTags);
        welfare = immutableNullableCopy(welfare);
    }

    private static List<String> immutableNullableCopy(List<String> values) {
        return values == null ? null : List.copyOf(values);
    }
}
