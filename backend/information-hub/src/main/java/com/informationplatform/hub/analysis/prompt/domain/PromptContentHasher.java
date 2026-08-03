package com.informationplatform.hub.analysis.prompt.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** 对不可变 User Prompt 原文计算稳定 SHA-256 内容身份。 */
@Component
public class PromptContentHasher {

    /** 按原文 UTF-8 字节计算小写十六进制 SHA-256，不擅自修改空白或换行。 */
    public String hash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }
}
