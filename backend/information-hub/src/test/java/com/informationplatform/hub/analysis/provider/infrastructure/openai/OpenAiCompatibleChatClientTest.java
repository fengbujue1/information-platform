package com.informationplatform.hub.analysis.provider.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.provider.domain.AiProviderErrorType;
import com.informationplatform.hub.analysis.provider.domain.AiProviderException;
import com.informationplatform.hub.analysis.provider.domain.AiProviderMessage;
import com.informationplatform.hub.analysis.provider.domain.AiProviderMessageRole;
import com.informationplatform.hub.analysis.provider.domain.AiProviderRequest;
import com.informationplatform.hub.analysis.provider.domain.AiProviderRetryDisposition;
import com.informationplatform.hub.analysis.provider.domain.AiProviderUsageStatus;
import com.informationplatform.hub.analysis.provider.infrastructure.config.AiProviderProperties;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@ExtendWith(OutputCaptureExtension.class)
class OpenAiCompatibleChatClientTest {

    private static final String ENDPOINT = "https://provider.test/v1/chat/completions";
    private static final String API_KEY = "test-provider-secret";

    private AiProviderProperties properties;
    private MockRestServiceServer server;
    private OpenAiCompatibleChatClient client;

    @BeforeEach
    void setUp() {
        properties = enabledProperties();
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OpenAiCompatibleChatClient(
                builder.build(),
                new ObjectMapper(),
                properties,
                new OpenAiCompatibleUsageAdapter());
    }

    @Test
    void sendsCompatibleRequestAndParsesMetadataAndCompleteUsage() {
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + API_KEY))
                .andExpect(jsonPath("$.model").value("qwen-plus"))
                .andExpect(jsonPath("$.max_tokens").value(100))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("job data"))
                .andRespond(withSuccess(
                        """
                        {
                          "id": "provider-request-1",
                          "model": "qwen-plus-2026",
                          "choices": [{
                            "message": {"role": "assistant", "content": "{\\"score\\":80}"},
                            "finish_reason": "stop"
                          }],
                          "usage": {
                            "prompt_tokens": 120,
                            "completion_tokens": 30,
                            "total_tokens": 150,
                            "prompt_tokens_details": {"cached_tokens": 20},
                            "completion_tokens_details": {"reasoning_tokens": 5}
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        var result = client.execute(request());

        assertThat(result.provider()).isEqualTo("OPENAI_COMPATIBLE");
        assertThat(client.modelName()).isEqualTo("qwen-plus");
        assertThat(result.modelName()).isEqualTo("qwen-plus-2026");
        assertThat(result.providerRequestId()).isEqualTo("provider-request-1");
        assertThat(result.outputText()).isEqualTo("{\"score\":80}");
        assertThat(result.finishReason()).isEqualTo("stop");
        assertThat(result.latencyMs()).isGreaterThanOrEqualTo(0);
        assertThat(result.usage().status()).isEqualTo(AiProviderUsageStatus.REPORTED);
        assertThat(result.usage().inputTokens()).isEqualTo(120);
        assertThat(result.usage().outputTokens()).isEqualTo(30);
        assertThat(result.usage().totalTokens()).isEqualTo(150);
        assertThat(result.usage().cachedInputTokens()).isEqualTo(20);
        assertThat(result.usage().reasoningTokens()).isEqualTo(5);
        server.verify();
    }

    @Test
    void keepsAllTokenFieldsNullWhenProviderDoesNotReportUsage() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(
                        """
                        {
                          "id": "provider-request-2",
                          "choices": [{
                            "message": {"content": "{}"},
                            "finish_reason": "stop"
                          }]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        var usage = client.execute(request()).usage();

        assertThat(usage.status()).isEqualTo(AiProviderUsageStatus.UNAVAILABLE);
        assertThat(usage.inputTokens()).isNull();
        assertThat(usage.outputTokens()).isNull();
        assertThat(usage.totalTokens()).isNull();
        assertThat(usage.cachedInputTokens()).isNull();
        assertThat(usage.reasoningTokens()).isNull();
        server.verify();
    }

    @Test
    void preservesReportedUsageWhenAssistantContentIsInvalid() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(
                        """
                        {
                          "id": "provider-request-3",
                          "choices": [{"message": {}}],
                          "usage": {
                            "prompt_tokens": 10,
                            "completion_tokens": 2,
                            "total_tokens": 12
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.execute(request()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(AiProviderErrorType.INVALID_RESPONSE);
                    assertThat(exception.retryDisposition())
                            .isEqualTo(AiProviderRetryDisposition.AMBIGUOUS_DO_NOT_AUTO_RETRY);
                    assertThat(exception.providerRequestId()).isEqualTo("provider-request-3");
                    assertThat(exception.usage().status()).isEqualTo(AiProviderUsageStatus.REPORTED);
                    assertThat(exception.usage().totalTokens()).isEqualTo(12);
                });
        server.verify();
    }

    @Test
    void invalidJsonDoesNotRetainRawProviderResponseInException() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(
                        "{\"secret\":\"" + API_KEY + "\"",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.execute(request()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(AiProviderErrorType.INVALID_RESPONSE);
                    assertThat(exception.retryDisposition())
                            .isEqualTo(AiProviderRetryDisposition.AMBIGUOUS_DO_NOT_AUTO_RETRY);
                    assertThat(exception).hasMessageNotContaining(API_KEY);
                    assertThat(exception.getCause()).isNull();
                });
        server.verify();
    }

    @Test
    void disabledProviderFailsBeforeAnyNetworkRequest() {
        properties.setEnabled(false);

        assertThatThrownBy(() -> client.execute(request()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(AiProviderErrorType.DISABLED);
                    assertThat(exception.retryDisposition())
                            .isEqualTo(AiProviderRetryDisposition.NEVER);
                });
        server.verify();
    }

    @Test
    void rejectsIncompleteConfigurationAndRequestAboveServerLimitBeforeNetwork() {
        properties.setApiKey(" ");

        assertThatThrownBy(() -> client.execute(request()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.errorType())
                            .isEqualTo(AiProviderErrorType.CONFIGURATION);
                    assertThat(exception.retryDisposition())
                            .isEqualTo(AiProviderRetryDisposition.NEVER);
                });
        server.verify();

        setUp();
        properties.setMaxOutputTokens(50);

        assertThatThrownBy(() -> client.execute(request()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.errorType())
                            .isEqualTo(AiProviderErrorType.CONFIGURATION);
                    assertThat(exception.retryDisposition())
                            .isEqualTo(AiProviderRetryDisposition.NEVER);
                });
        server.verify();
    }

    @Test
    void classifiesRateLimitAndServerErrorWithoutLeakingSecrets(CapturedOutput output) {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .header("x-request-id", "rate-request")
                        .body("{\"error\":\"" + API_KEY + "\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.execute(request()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(AiProviderErrorType.RATE_LIMITED);
                    assertThat(exception.retryDisposition())
                            .isEqualTo(AiProviderRetryDisposition.RETRY_WITH_BACKOFF);
                    assertThat(exception.httpStatus()).isEqualTo(429);
                    assertThat(exception.providerRequestId()).isEqualTo("rate-request");
                    assertThat(exception).hasMessageNotContaining(API_KEY);
                });
        assertThat(output).doesNotContain(API_KEY);
        server.verify();

        setUp();
        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> client.execute(request()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.errorType())
                            .isEqualTo(AiProviderErrorType.UPSTREAM_SERVER_ERROR);
                    assertThat(exception.retryDisposition())
                            .isEqualTo(AiProviderRetryDisposition.AMBIGUOUS_DO_NOT_AUTO_RETRY);
                });
        server.verify();
    }

    @Test
    void classifiesTimeoutAsAmbiguousAndNeverRetriesItAutomatically() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withException(new SocketTimeoutException("socket timed out")));

        assertThatThrownBy(() -> client.execute(request()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(AiProviderErrorType.TIMEOUT);
                    assertThat(exception.retryDisposition())
                            .isEqualTo(AiProviderRetryDisposition.AMBIGUOUS_DO_NOT_AUTO_RETRY);
                    assertThat(exception.getMessage()).isEqualTo("AI Provider request timed out");
                });
        server.verify();
    }

    @Test
    void rejectsInvalidConfigurationAndInvalidUsageWithoutDerivingTokens() {
        properties.setBaseUrl("https://user:password@provider.test/v1");
        assertThatThrownBy(() -> client.execute(request()))
                .isInstanceOfSatisfying(AiProviderException.class, exception ->
                        assertThat(exception.errorType())
                                .isEqualTo(AiProviderErrorType.CONFIGURATION));
        server.verify();

        setUp();
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(
                        """
                        {
                          "choices": [{"message": {"content": "{}"}}],
                          "usage": {"prompt_tokens": -1}
                        }
                        """,
                        MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.execute(request()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(AiProviderErrorType.INVALID_RESPONSE);
                    assertThat(exception.usage().status())
                            .isEqualTo(AiProviderUsageStatus.UNAVAILABLE);
                });
        server.verify();
    }

    private AiProviderRequest request() {
        return new AiProviderRequest(
                List.of(
                        new AiProviderMessage(AiProviderMessageRole.SYSTEM, "platform rules"),
                        new AiProviderMessage(AiProviderMessageRole.USER, "job data")),
                100);
    }

    private AiProviderProperties enabledProperties() {
        AiProviderProperties value = new AiProviderProperties();
        value.setEnabled(true);
        value.setBaseUrl("https://provider.test/v1/");
        value.setApiKey(API_KEY);
        value.setModel("qwen-plus");
        value.setTimeout(Duration.ofSeconds(5));
        value.setMaxOutputTokens(1000);
        return value;
    }
}
