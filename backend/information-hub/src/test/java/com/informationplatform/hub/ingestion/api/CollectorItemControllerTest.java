package com.informationplatform.hub.ingestion.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.informationplatform.hub.common.api.ApiErrorWriter;
import com.informationplatform.hub.common.api.GlobalApiExceptionHandler;
import com.informationplatform.hub.ingestion.api.dto.IngestionResult;
import com.informationplatform.hub.ingestion.application.InformationIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CollectorItemController.class)
@Import({
    CollectorApiProperties.class,
    CollectorTokenAuthenticationFilter.class,
    CollectorRequestSizeFilter.class,
    ApiErrorWriter.class,
    GlobalApiExceptionHandler.class
})
@TestPropertySource(properties = {
    "information-hub.collector-api.token=test-collector-token",
    "information-hub.collector-api.max-request-bytes=1024"
})
class CollectorItemControllerTest {

    private static final String ENDPOINT = "/api/v1/collector/items";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InformationIngestionService ingestionService;

    @Test
    void createsItemWithStableResponse() throws Exception {
        when(ingestionService.ingest(any()))
                .thenReturn(new IngestionResult(1001, true, true, 1, true));

        mockMvc.perform(post(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-collector-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("ITEM_CREATED"))
                .andExpect(jsonPath("$.data.informationId").value(1001))
                .andExpect(jsonPath("$.data.versionNo").value(1));
    }

    @Test
    void rejectsMissingTokenWithoutCallingController() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COLLECTOR_AUTHENTICATION_FAILED"));
    }

    @Test
    void rejectsInvalidRequest() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-collector-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest().replace(
                                "\"title\": \"Java developer\"", "\"title\": \"\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-collector-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));
    }

    @Test
    void rejectsRequestBodyOverConfiguredLimit() throws Exception {
        String largeBody = validRequest().replace(
                "\"content\": \"JD\"",
                "\"content\": \"" + "x".repeat(1500) + "\"");

        mockMvc.perform(post(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-collector-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(largeBody))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("REQUEST_TOO_LARGE"));
    }

    private String validRequest() {
        return """
                {
                  "schemaVersion": 1,
                  "informationType": "JOB",
                  "source": "BOSS",
                  "sourceItemId": "encrypt+/=_中文",
                  "sourceUrl": "https://example.test/job",
                  "title": "Java developer",
                  "content": "JD",
                  "publishTime": null,
                  "collectedAt": "2026-07-26T08:00:00Z",
                  "collector": {
                    "collectorId": "test-collector",
                    "collectorVersion": "1.0"
                  },
                  "collectionContext": {
                    "runId": "test"
                  },
                  "extension": {
                    "sourceRecruiterId": "recruiter-id",
                    "remoteType": "UNKNOWN",
                    "jobStatus": "ACTIVE",
                    "detailStatus": "FETCHED",
                    "sourceTags": ["Java", "本科"],
                    "sourceSkillTags": ["Spring Boot"]
                  },
                  "rawPayload": {
                    "list": {
                      "encrypt_job_id": "encrypt+/=_中文"
                    },
                    "detail": {
                      "jd": "JD"
                    }
                  }
                }
                """;
    }
}
