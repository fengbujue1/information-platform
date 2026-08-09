package com.informationplatform.hub.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OperationalLogExceptionsTest {

    @Test
    void keepsTypeAndStackButDropsSensitiveMessagesAndCause() {
        IllegalArgumentException cause = new IllegalArgumentException("raw-payload-secret");
        IllegalStateException original =
                new IllegalStateException("prompt-secret", cause);

        RuntimeException sanitized = OperationalLogExceptions.sanitized(original);

        assertThat(sanitized.getMessage())
                .isEqualTo("Sanitized java.lang.IllegalStateException");
        assertThat(sanitized.getCause()).isNull();
        assertThat(sanitized.getSuppressed()).isEmpty();
        assertThat(sanitized.getStackTrace()).isEqualTo(original.getStackTrace());
        assertThat(sanitized.toString())
                .doesNotContain("prompt-secret", "raw-payload-secret");
    }
}
