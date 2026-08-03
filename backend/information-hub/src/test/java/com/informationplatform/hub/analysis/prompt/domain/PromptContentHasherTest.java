package com.informationplatform.hub.analysis.prompt.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/** 验证 Prompt 原文 UTF-8 SHA-256 内容身份稳定且不修改空白。 */
class PromptContentHasherTest {

    private final PromptContentHasher hasher = new PromptContentHasher();

    @Test
    void hashesExactUtf8ContentDeterministically() {
        assertEquals(
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                hasher.hash("hello"));
        assertEquals(hasher.hash("关注 Java 与稳定性"), hasher.hash("关注 Java 与稳定性"));
        assertNotEquals(hasher.hash("line\n"), hasher.hash("line\r\n"));
        assertNotEquals(hasher.hash(" prompt"), hasher.hash("prompt"));
    }
}
