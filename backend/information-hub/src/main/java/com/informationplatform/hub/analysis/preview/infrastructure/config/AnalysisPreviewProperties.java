package com.informationplatform.hub.analysis.preview.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Preview HMAC 服务端秘密配置。 */
@Component
@ConfigurationProperties(prefix = "information-hub.ai.preview")
public class AnalysisPreviewProperties {

    /** 仅存在服务端内存中的 HMAC 秘密，至少 32 UTF-8 字节。 */
    private String hmacSecret = "";

    public String getHmacSecret() {
        return hmacSecret;
    }

    public void setHmacSecret(String hmacSecret) {
        this.hmacSecret = hmacSecret == null ? "" : hmacSecret;
    }

    /** 诊断输出永不暴露签名秘密。 */
    @Override
    public String toString() {
        return "AnalysisPreviewProperties{hmacSecret='<redacted>', configured="
                + !hmacSecret.isBlank() + '}';
    }
}
