package com.informationplatform.hub.analysis.preview.infrastructure.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 分析预览签名秘密与运行限制配置。 */
@Component
@ConfigurationProperties(prefix = "information-hub.ai.preview")
public class AnalysisPreviewProperties implements InitializingBean {

    /** 窗口天数的技术安全上限，避免时间计算溢出。 */
    static final int SAFE_MAX_WINDOW_DAYS = 3_650;
    /** 候选数量的技术安全上限，避免单次查询和内存占用失控。 */
    static final int SAFE_MAX_CANDIDATES = 10_000;
    /** Token 预算的技术安全上限，避免聚合计数溢出。 */
    static final long SAFE_MAX_ESTIMATED_TOKENS = 1_000_000_000L;

    /** 自行随机生成的 HMAC 秘密；仅在服务端使用，至少 32 个 UTF-8 字节。 */
    private String hmacSecret = "";
    /** 手动分析和定时分析默认候选窗口天数。 */
    private int defaultWindowDays = 3;
    /** 手动分析和定时分析允许的最大候选窗口天数。 */
    private int maxWindowDays = 14;
    /** 手动分析和定时分析默认最大候选数量。 */
    private int defaultMaxCandidates = 20;
    /** 手动分析和定时分析允许的最大候选数量。 */
    private int maxCandidates = 50;
    /** 手动分析和定时分析默认预估 Token 预算。 */
    private long defaultMaxEstimatedTokens = 75_000;
    /** 手动分析和定时分析允许的最大预估 Token 预算。 */
    private long maxEstimatedTokens = 200_000;

    public String getHmacSecret() {
        return hmacSecret;
    }

    public void setHmacSecret(String hmacSecret) {
        this.hmacSecret = hmacSecret == null ? "" : hmacSecret;
    }

    public int getDefaultWindowDays() {
        return defaultWindowDays;
    }

    public void setDefaultWindowDays(int defaultWindowDays) {
        this.defaultWindowDays = defaultWindowDays;
    }

    public int getMaxWindowDays() {
        return maxWindowDays;
    }

    public void setMaxWindowDays(int maxWindowDays) {
        this.maxWindowDays = maxWindowDays;
    }

    public int getDefaultMaxCandidates() {
        return defaultMaxCandidates;
    }

    public void setDefaultMaxCandidates(int defaultMaxCandidates) {
        this.defaultMaxCandidates = defaultMaxCandidates;
    }

    public int getMaxCandidates() {
        return maxCandidates;
    }

    public void setMaxCandidates(int maxCandidates) {
        this.maxCandidates = maxCandidates;
    }

    public long getDefaultMaxEstimatedTokens() {
        return defaultMaxEstimatedTokens;
    }

    public void setDefaultMaxEstimatedTokens(long defaultMaxEstimatedTokens) {
        this.defaultMaxEstimatedTokens = defaultMaxEstimatedTokens;
    }

    public long getMaxEstimatedTokens() {
        return maxEstimatedTokens;
    }

    public void setMaxEstimatedTokens(long maxEstimatedTokens) {
        this.maxEstimatedTokens = maxEstimatedTokens;
    }

    /** 启动时拒绝非正数、默认值超过上限或超过技术安全边界的配置。 */
    @Override
    public void afterPropertiesSet() {
        validateRange(
                "windowDays",
                defaultWindowDays,
                maxWindowDays,
                SAFE_MAX_WINDOW_DAYS);
        validateRange(
                "maxCandidates",
                defaultMaxCandidates,
                maxCandidates,
                SAFE_MAX_CANDIDATES);
        validateRange(
                "maxEstimatedTokens",
                defaultMaxEstimatedTokens,
                maxEstimatedTokens,
                SAFE_MAX_ESTIMATED_TOKENS);
    }

    private void validateRange(String name, long defaultValue, long maximum, long safeMaximum) {
        if (defaultValue <= 0 || maximum <= 0) {
            throw new IllegalStateException(name + " defaults and maxima must be positive");
        }
        if (defaultValue > maximum) {
            throw new IllegalStateException(name + " default must not exceed maximum");
        }
        if (maximum > safeMaximum) {
            throw new IllegalStateException(
                    name + " maximum exceeds technical safety boundary " + safeMaximum);
        }
    }

    /** 诊断输出永不暴露签名秘密。 */
    @Override
    public String toString() {
        return "AnalysisPreviewProperties{hmacSecret='<redacted>', configured="
                + !hmacSecret.isBlank()
                + ", defaultWindowDays=" + defaultWindowDays
                + ", maxWindowDays=" + maxWindowDays
                + ", defaultMaxCandidates=" + defaultMaxCandidates
                + ", maxCandidates=" + maxCandidates
                + ", defaultMaxEstimatedTokens=" + defaultMaxEstimatedTokens
                + ", maxEstimatedTokens=" + maxEstimatedTokens
                + '}';
    }
}
