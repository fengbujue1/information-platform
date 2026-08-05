package com.informationplatform.hub.analysis.preview.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.informationplatform.hub.analysis.preview.domain.PreviewCandidateEstimate;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Component;

/** 对稳定顺序候选、Estimate 和预算决策计算版本化 SHA-256 指纹。 */
@Component
public class CandidateFingerprintCalculator {

    private static final int FINGERPRINT_VERSION = 1;

    /** 用固定字段顺序生成规范 JSON。 */
    private final ObjectMapper objectMapper;

    public CandidateFingerprintCalculator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String calculate(List<PreviewCandidateEstimate> candidates) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("fingerprintVersion", FINGERPRINT_VERSION);
        ArrayNode items = root.putArray("candidates");
        for (PreviewCandidateEstimate candidate : candidates) {
            ObjectNode item = items.addObject();
            item.put("informationId", candidate.informationId());
            item.put("snapshotId", candidate.snapshotId());
            item.put("firstSeenTime", candidate.firstSeenTime().toString());
            item.put("estimatedInputTokens", candidate.estimatedInputTokens());
            item.put("estimatedOutputTokens", candidate.estimatedOutputTokens());
            item.put("estimatedTotalTokens", candidate.estimatedTotalTokens());
            item.put("selected", candidate.selected());
        }
        try {
            return HexFormat.of().formatHex(
                    digest().digest(objectMapper.writeValueAsBytes(root)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Preview fingerprint could not be serialized", exception);
        }
    }

    private MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
