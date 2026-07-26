package com.informationplatform.hub.ingestion.api;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "information-hub.collector-api")
public class CollectorApiProperties {

    /** 采集器调用写入接口时使用的 Bearer Token。 */
    private String token = "";

    /** 单次采集请求允许的最大请求体字节数。 */
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
