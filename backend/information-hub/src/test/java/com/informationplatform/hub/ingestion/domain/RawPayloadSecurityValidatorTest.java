package com.informationplatform.hub.ingestion.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.ingestion.application.IngestionRequestException;
import org.junit.jupiter.api.Test;

class RawPayloadSecurityValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RawPayloadSecurityValidator validator = new RawPayloadSecurityValidator();

    @Test
    void rejectsForbiddenFieldAtAnyDepth() throws Exception {
        IngestionRequestException exception = assertThrows(
                IngestionRequestException.class,
                () -> validator.validate(objectMapper.readTree(
                        "{\"detail\":{\"security_id\":\"secret\"}}")));

        assertEquals("UNSAFE_RAW_PAYLOAD", exception.getCode());
    }

    @Test
    void acceptsSanitizedBusinessPayload() throws Exception {
        assertDoesNotThrow(() -> validator.validate(objectMapper.readTree(
                "{\"list\":{\"encrypt_job_id\":\"abc+/=\"},\"detail\":{\"jd\":\"text\"}}")));
    }
}
