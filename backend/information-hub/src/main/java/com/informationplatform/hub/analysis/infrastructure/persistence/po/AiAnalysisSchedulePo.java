package com.informationplatform.hub.analysis.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 映射用户配置的每日分析计划。 */
@TableName("ai_analysis_schedule")
public class AiAnalysisSchedulePo {

    /** 分析计划自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 拥有该计划的用户账号主键。 */
    private Long userId;

    /** 用户可识别的计划名称。 */
    private String name;

    /** 计划执行时使用的 Prompt 配置档案主键。 */
    private Long promptProfileId;

    /** 是否启用计划，默认关闭。 */
    private Boolean enabled;

    /** 用户时区中的每日执行时间，默认 02:00。 */
    private LocalTime localTime;

    /** 解释每日执行时间所使用的 IANA 时区名称。 */
    private String timezone;

    /** 候选信息回看窗口天数，默认 3 天。 */
    private Integer windowDays;

    /** 单批最多冻结的候选数量，默认 20 条。 */
    private Integer maxCandidates;

    /** 单批允许的预估 Token 上限，默认 75000。 */
    private Long maxEstimatedTokens;

    /** 下一次应触发时间，按 UTC 保存，未启用时允许为空。 */
    private LocalDateTime nextRunAt;

    /** 分析计划创建时间，按 UTC 保存。 */
    private LocalDateTime createdAt;

    /** 分析计划最近更新时间，按 UTC 保存。 */
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getPromptProfileId() {
        return promptProfileId;
    }

    public void setPromptProfileId(Long promptProfileId) {
        this.promptProfileId = promptProfileId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalTime getLocalTime() {
        return localTime;
    }

    public void setLocalTime(LocalTime localTime) {
        this.localTime = localTime;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Integer getWindowDays() {
        return windowDays;
    }

    public void setWindowDays(Integer windowDays) {
        this.windowDays = windowDays;
    }

    public Integer getMaxCandidates() {
        return maxCandidates;
    }

    public void setMaxCandidates(Integer maxCandidates) {
        this.maxCandidates = maxCandidates;
    }

    public Long getMaxEstimatedTokens() {
        return maxEstimatedTokens;
    }

    public void setMaxEstimatedTokens(Long maxEstimatedTokens) {
        this.maxEstimatedTokens = maxEstimatedTokens;
    }

    public LocalDateTime getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(LocalDateTime nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
