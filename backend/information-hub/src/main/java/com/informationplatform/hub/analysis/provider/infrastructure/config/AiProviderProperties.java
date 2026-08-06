package com.informationplatform.hub.analysis.provider.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** OpenAI-compatible Provider 配置；地址、Key 和模型均以 Provider 官方资料为准。 */
@Component
@ConfigurationProperties(prefix = "information-hub.ai.provider")
public class AiProviderProperties {

    /** 是否允许调用真实 Provider；默认关闭。 */
    private boolean enabled;

    /** OpenAI-compatible 基础地址；从官方文档获取，通常含 /v1，不含 /chat/completions。 */
    private String baseUrl = "";

    /** Provider 控制台创建的 API Key；仅存在服务端内存或部署 Secret。 */
    private String apiKey = "";

    /** Provider 官方模型列表中的 API 模型 ID，不是页面展示名称。 */
    private String model = "";

    /** Provider HTTP 连接和读取超时；Spring Duration，示例 30s，必须为正。 */
    private Duration timeout = Duration.ofSeconds(30);

    /** Provider 配置允许的单次最大输出 Token，范围 1 至 1000。 */
    private int maxOutputTokens = 1000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = normalize(baseUrl);
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = normalize(model);
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("AI Provider timeout must be positive");
        }
        this.timeout = timeout;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        if (maxOutputTokens <= 0 || maxOutputTokens > 1000) {
            throw new IllegalArgumentException(
                    "AI Provider maxOutputTokens must be between 1 and 1000");
        }
        this.maxOutputTokens = maxOutputTokens;
    }

    /**
     * 配置诊断字符串永不包含 API Key 或未经校验的 Base URL 原文。
     *
     * <p>即使配置对象被框架或诊断代码意外记录，也只显示秘密占位符和非敏感配置状态。
     */
    @Override
    public String toString() {
        return "AiProviderProperties{"
                + "enabled=" + enabled
                + ", baseUrlConfigured=" + !baseUrl.isBlank()
                + ", apiKey='<redacted>'"
                + ", model='" + model + '\''
                + ", timeout=" + timeout
                + ", maxOutputTokens=" + maxOutputTokens
                + '}';
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
