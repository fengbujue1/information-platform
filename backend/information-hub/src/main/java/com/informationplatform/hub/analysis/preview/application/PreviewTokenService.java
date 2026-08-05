package com.informationplatform.hub.analysis.preview.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.preview.domain.PreviewTokenPayload;
import com.informationplatform.hub.analysis.preview.infrastructure.config.AnalysisPreviewProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** 使用 JDK HmacSHA256 签发和验证短期 Preview Token。 */
@Component
public class PreviewTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MIN_SECRET_BYTES = 32;
    private static final int MAX_TOKEN_CHARS = 32_768;

    /** Token JSON 序列化器。 */
    private final ObjectMapper objectMapper;
    /** 只存在服务端的 HMAC 配置。 */
    private final AnalysisPreviewProperties properties;
    /** UTC 到期校验时钟。 */
    private final Clock clock;

    public PreviewTokenService(
            ObjectMapper objectMapper, AnalysisPreviewProperties properties, Clock clock) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    /** 返回 base64url(payload).base64url(signature)，载荷可读但不可篡改。 */
    public String issue(PreviewTokenPayload payload) {
        try {
            byte[] serialized = objectMapper.writeValueAsBytes(payload);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(serialized);
            String encodedSignature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(sign(serialized));
            return encodedPayload + "." + encodedSignature;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Preview Token could not be serialized", exception);
        }
    }

    /** 验证格式、恒定时间签名与到期时间，供 TASK-029 Confirm 复用。 */
    public PreviewTokenPayload verify(String token) {
        if (token == null || token.isBlank() || token.length() > MAX_TOKEN_CHARS) {
            throw conflict("PREVIEW_TOKEN_INVALID", "Preview Token is invalid");
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw conflict("PREVIEW_TOKEN_INVALID", "Preview Token is invalid");
        }
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[0]);
            byte[] suppliedSignature = Base64.getUrlDecoder().decode(parts[1]);
            if (!MessageDigest.isEqual(sign(payloadBytes), suppliedSignature)) {
                throw conflict("PREVIEW_TOKEN_INVALID", "Preview Token is invalid");
            }
            PreviewTokenPayload payload =
                    objectMapper.readValue(payloadBytes, PreviewTokenPayload.class);
            if (payload.tokenVersion() != 1
                    || payload.expiresAt() == null
                    || payload.issuedAt() == null
                    || !payload.expiresAt().isAfter(clock.instant())) {
                throw conflict("PREVIEW_TOKEN_EXPIRED", "Preview Token has expired");
            }
            return payload;

        } catch (IllegalArgumentException | IOException exception) {
            throw conflict("PREVIEW_TOKEN_INVALID", "Preview Token is invalid");
        }
    }

    /** 验证签名、到期时间和 Session Owner，供 TASK-029 Confirm 直接复用。 */
    public PreviewTokenPayload verifyForOwner(String token, long userId) {
        PreviewTokenPayload payload = verify(token);
        if (userId <= 0 || payload.userId() != userId) {
            throw conflict(
                    "PREVIEW_TOKEN_OWNER_MISMATCH",
                    "Preview Token does not belong to the current user");
        }
        return payload;
    }

    private byte[] sign(byte[] payload) {
        byte[] secret = requireSecret();
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(payload);
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA256 is unavailable", exception);
        }
    }

    private byte[] requireSecret() {
        byte[] secret = properties.getHmacSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < MIN_SECRET_BYTES) {
            throw new AnalysisPreviewConfigurationException(
                    "Preview HMAC secret must contain at least 32 UTF-8 bytes");
        }
        return secret;
    }

    private AnalysisPreviewConflictException conflict(String code, String message) {
        return new AnalysisPreviewConflictException(code, message);
    }
}
