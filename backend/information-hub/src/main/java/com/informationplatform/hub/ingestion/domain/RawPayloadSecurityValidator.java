package com.informationplatform.hub.ingestion.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.informationplatform.hub.ingestion.application.IngestionRequestException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RawPayloadSecurityValidator {

    /** 归一化后禁止出现在原始数据任意层级的敏感字段名。 */
    private static final Set<String> FORBIDDEN_KEYS = Set.of(
            "authorization",
            "authorizationheader",
            "browsercredentials",
            "browserlocalcredentials",
            "chromeprofile",
            "cookie",
            "cookies",
            "credentials",
            "lid",
            "logintoken",
            "password",
            "refreshtoken",
            "securityid",
            "token",
            "accesstoken");

    /** 递归检查原始数据，并在发现敏感字段时拒绝整个接入请求。 */
    public void validate(JsonNode rawPayload) {
        // 返回字段路径便于采集器定位问题，但不读取或输出敏感字段值。
        String forbiddenPath = findForbiddenPath(rawPayload, "rawPayload");
        if (forbiddenPath != null) {
            throw new IngestionRequestException(
                    "UNSAFE_RAW_PAYLOAD",
                    "rawPayload contains a forbidden sensitive field at " + forbiddenPath);
        }
    }

    /** 深度优先遍历对象和数组，返回首个命中的敏感字段路径。 */
    private String findForbiddenPath(JsonNode node, String path) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            for (Map.Entry<String, JsonNode> field : node.properties()) {
                String fieldPath = path + "." + field.getKey();
                // 字段名归一化后匹配，覆盖大小写、下划线和连字符变体。
                if (FORBIDDEN_KEYS.contains(normalizeKey(field.getKey()))) {
                    return fieldPath;
                }
                String nested = findForbiddenPath(field.getValue(), fieldPath);
                if (nested != null) {
                    return nested;
                }
            }
        } else if (node.isArray()) {
            for (int index = 0; index < node.size(); index++) {
                String nested = findForbiddenPath(node.get(index), path + "[" + index + "]");
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    /** 删除非字母数字字符并统一小写，避免敏感字段通过命名变体绕过。 */
    private String normalizeKey(String key) {
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
