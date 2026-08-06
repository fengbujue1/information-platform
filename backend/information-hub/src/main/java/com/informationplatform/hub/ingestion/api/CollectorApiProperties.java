package com.informationplatform.hub.ingestion.api;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Collector 接入配置；值的来源、格式和安全要求见 CONFIGURATION.md。 */
@Component
@ConfigurationProperties(prefix = "information-hub.collector-api")
public class CollectorApiProperties {

    /** 采集器写入 Bearer Token；由管理员随机生成，并与 Collector 配置相同值。 */
    private String token = "";

    /** 单次采集请求体字节上限；正整数，默认 2 MiB（2097152 字节）。 */
    private long maxRequestBytes = 2 * 1024 * 1024;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        // 将未配置的 Token 统一为空字符串，便于认证过滤器判断服务是否可用。
        this.token = token == null ? "" : token;
    }

    public long getMaxRequestBytes() {
        return maxRequestBytes;
    }

    public void setMaxRequestBytes(long maxRequestBytes) {
        // 启动阶段拒绝无效配置，避免请求限流被意外关闭。
        if (maxRequestBytes <= 0) {
            throw new IllegalArgumentException("maxRequestBytes must be positive");
        }
        this.maxRequestBytes = maxRequestBytes;
    }
}
