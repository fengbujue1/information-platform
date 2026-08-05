package com.informationplatform.hub.analysis.provider.infrastructure.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.informationplatform.hub.analysis.provider.application.AiProviderClient;
import com.informationplatform.hub.analysis.provider.domain.AiProviderErrorType;
import com.informationplatform.hub.analysis.provider.domain.AiProviderException;
import com.informationplatform.hub.analysis.provider.domain.AiProviderMessage;
import com.informationplatform.hub.analysis.provider.domain.AiProviderRequest;
import com.informationplatform.hub.analysis.provider.domain.AiProviderResult;
import com.informationplatform.hub.analysis.provider.domain.AiProviderRetryDisposition;
import com.informationplatform.hub.analysis.provider.domain.AiProviderUsage;
import com.informationplatform.hub.analysis.provider.infrastructure.config.AiProviderProperties;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenAiCompatibleChatClient implements AiProviderClient {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    /** 不记录请求正文或 Authorization 的 HTTP Client。 */
    private final RestClient restClient;

    /** 现有平台 Jackson 实例。 */
    private final ObjectMapper objectMapper;

    /** 仅服务端可见且默认禁用的 Provider 配置。 */
    private final AiProviderProperties properties;

    /** OpenAI-compatible Usage 到平台 Actual Usage 的独立适配器。 */
    private final OpenAiCompatibleUsageAdapter usageAdapter;

    public OpenAiCompatibleChatClient(
            @Qualifier("openAiCompatibleRestClient") RestClient restClient,
            ObjectMapper objectMapper,
            AiProviderProperties properties,
            OpenAiCompatibleUsageAdapter usageAdapter) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.usageAdapter = usageAdapter;
    }

    @Override
    public String providerId() {
        return "OPENAI_COMPATIBLE";
    }

    @Override
    public String modelName() {
        return properties.getModel();
    }

    /**
     * 执行一次 OpenAI-compatible Chat Completions 请求。
     *
     * <p>该方法不持有数据库事务、不记录秘密，也不自动重试；timeout 和无法确认结果的 5xx
     * 明确标记为禁止自动重试。
     */
    @Override
    public AiProviderResult execute(AiProviderRequest request) {
        validateRequest(request);
        URI endpoint = endpoint();
        ObjectNode requestBody = requestBody(request);
        long startedNanos = System.nanoTime();
        RawProviderResponse response;
        try {
            response = restClient
                    .post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .body(requestBody)
                    .exchange((ignoredRequest, clientResponse) -> new RawProviderResponse(
                            clientResponse.getStatusCode().value(),
                            firstRequestId(clientResponse.getHeaders()),
                            new String(
                                    clientResponse.getBody().readAllBytes(),
                                    StandardCharsets.UTF_8)));
        } catch (ResourceAccessException exception) {
            long latencyMs = elapsedMillis(startedNanos);
            if (hasCause(exception, SocketTimeoutException.class)) {
                throw failure(
                        AiProviderErrorType.TIMEOUT,
                        AiProviderRetryDisposition.AMBIGUOUS_DO_NOT_AUTO_RETRY,
                        "AI Provider request timed out",
                        null,
                        null,
                        AiProviderUsage.unavailable(),
                        latencyMs,
                        exception);
            }
            AiProviderRetryDisposition retryDisposition =
                    hasCause(exception, ConnectException.class)
                            ? AiProviderRetryDisposition.RETRY_WITH_BACKOFF
                            : AiProviderRetryDisposition.AMBIGUOUS_DO_NOT_AUTO_RETRY;
            throw failure(
                    AiProviderErrorType.NETWORK,
                    retryDisposition,
                    "AI Provider network request failed",
                    null,
                    null,
                    AiProviderUsage.unavailable(),
                    latencyMs,
                    exception);
        } catch (RestClientException exception) {
            throw failure(
                    AiProviderErrorType.NETWORK,
                    AiProviderRetryDisposition.AMBIGUOUS_DO_NOT_AUTO_RETRY,
                    "AI Provider network request failed",
                    null,
                    null,
                    AiProviderUsage.unavailable(),
                    elapsedMillis(startedNanos),
                    exception);
        }

        long latencyMs = elapsedMillis(startedNanos);
        if (response == null) {
            throw invalidResponse(null, AiProviderUsage.unavailable(), latencyMs, null);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw httpFailure(response, latencyMs);
        }
        return parseSuccess(response, latencyMs);
    }

    /** 在组装 URI 或 Authorization 前拒绝 disabled/不完整配置，保证禁用时零网络请求。 */
    @Override
    public void validateRequest(AiProviderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("AI Provider request must not be null");
        }
        if (!properties.isEnabled()) {
            throw failure(
                    AiProviderErrorType.DISABLED,
                    AiProviderRetryDisposition.NEVER,
                    "AI Provider is disabled",
                    null,
                    null,
                    AiProviderUsage.unavailable(),
                    0,
                    null);
        }
        if (properties.getBaseUrl().isBlank()
                || properties.getApiKey().isBlank()
                || properties.getModel().isBlank()) {
            throw failure(
                    AiProviderErrorType.CONFIGURATION,
                    AiProviderRetryDisposition.NEVER,
                    "AI Provider configuration is incomplete",
                    null,
                    null,
                    AiProviderUsage.unavailable(),
                    0,
                    null);
        }
        if (request.maxOutputTokens() > properties.getMaxOutputTokens()) {
            throw failure(
                    AiProviderErrorType.CONFIGURATION,
                    AiProviderRetryDisposition.NEVER,
                    "AI Provider request exceeds configured maxOutputTokens",
                    null,
                    null,
                    AiProviderUsage.unavailable(),
                    0,
                    null);
        }
    }

    /** 只允许无凭据、无查询参数的 HTTP(S) Base URL，避免秘密进入 URI 或日志。 */
    private URI endpoint() {
        try {
            URI base = new URI(properties.getBaseUrl());
            if (base.getScheme() == null
                    || (!base.getScheme().equalsIgnoreCase("http")
                            && !base.getScheme().equalsIgnoreCase("https"))
                    || base.getHost() == null
                    || base.getUserInfo() != null
                    || base.getQuery() != null
                    || base.getFragment() != null) {
                throw new URISyntaxException(properties.getBaseUrl(), "Unsupported Provider base URL");
            }
            String normalized = properties.getBaseUrl().replaceAll("/+$", "");
            return new URI(normalized + CHAT_COMPLETIONS_PATH);
        } catch (URISyntaxException exception) {
            // URI 解析异常可能回显原始配置，不能作为 cause 继续传播到日志。
            throw failure(
                    AiProviderErrorType.CONFIGURATION,
                    AiProviderRetryDisposition.NEVER,
                    "AI Provider baseUrl is invalid",
                    null,
                    null,
                    AiProviderUsage.unavailable(),
                    0,
                    null);
        }
    }

    private ObjectNode requestBody(AiProviderRequest request) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.getModel());
        body.put("max_tokens", request.maxOutputTokens());
        body.put("stream", false);
        ArrayNode messages = body.putArray("messages");
        for (AiProviderMessage message : request.messages()) {
            ObjectNode item = messages.addObject();
            item.put("role", message.role().wireValue());
            item.put("content", message.content());
        }
        return body;
    }

    /** 先解析 Usage 和请求元数据，再校验输出文本，确保后处理失败仍可保留 Provider Usage。 */
    private AiProviderResult parseSuccess(RawProviderResponse response, long latencyMs) {
        JsonNode root;
        try {
            root = objectMapper.readTree(response.body());
        } catch (IOException exception) {
            // Jackson 异常可能包含 Provider 原始响应片段，只保留稳定脱敏分类。
            throw invalidResponse(
                    response.requestIdHeader(),
                    AiProviderUsage.unavailable(),
                    latencyMs,
                    null);
        }
        if (root == null || !root.isObject()) {
            throw invalidResponse(
                    response.requestIdHeader(), AiProviderUsage.unavailable(), latencyMs, null);
        }

        String requestId = nullableText(root.get("id"));
        if (requestId == null) {
            requestId = response.requestIdHeader();
        }
        AiProviderUsage usage;
        try {
            usage = usageAdapter.adapt(root.get("usage"));
        } catch (IllegalArgumentException exception) {
            throw invalidResponse(requestId, AiProviderUsage.unavailable(), latencyMs, null);
        }

        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw invalidResponse(requestId, usage, latencyMs, null);
        }
        JsonNode choice = choices.get(0);
        JsonNode message = choice == null ? null : choice.get("message");
        JsonNode content = message == null ? null : message.get("content");
        if (content == null || !content.isTextual()) {
            throw invalidResponse(requestId, usage, latencyMs, null);
        }

        String model = nullableText(root.get("model"));
        if (model == null || model.isBlank()) {
            model = modelName();
        }
        return new AiProviderResult(
                providerId(),
                model,
                requestId,
                content.textValue(),
                nullableText(choice.get("finish_reason")),
                latencyMs,
                usage);
    }

    private AiProviderException httpFailure(RawProviderResponse response, long latencyMs) {
        int status = response.statusCode();
        if (status == 401 || status == 403) {
            return failure(
                    AiProviderErrorType.AUTHENTICATION,
                    AiProviderRetryDisposition.NEVER,
                    "AI Provider authentication failed",
                    status,
                    response.requestIdHeader(),
                    AiProviderUsage.unavailable(),
                    latencyMs,
                    null);
        }
        if (status == 429) {
            return failure(
                    AiProviderErrorType.RATE_LIMITED,
                    AiProviderRetryDisposition.RETRY_WITH_BACKOFF,
                    "AI Provider rate limit exceeded",
                    status,
                    response.requestIdHeader(),
                    AiProviderUsage.unavailable(),
                    latencyMs,
                    null);
        }
        if (status >= 500) {
            return failure(
                    AiProviderErrorType.UPSTREAM_SERVER_ERROR,
                    AiProviderRetryDisposition.AMBIGUOUS_DO_NOT_AUTO_RETRY,
                    "AI Provider server error",
                    status,
                    response.requestIdHeader(),
                    AiProviderUsage.unavailable(),
                    latencyMs,
                    null);
        }
        return failure(
                AiProviderErrorType.UPSTREAM_CLIENT_ERROR,
                AiProviderRetryDisposition.NEVER,
                "AI Provider rejected the request",
                status,
                response.requestIdHeader(),
                AiProviderUsage.unavailable(),
                latencyMs,
                null);
    }

    private AiProviderException invalidResponse(
            String requestId, AiProviderUsage usage, long latencyMs, Throwable cause) {
        return failure(
                AiProviderErrorType.INVALID_RESPONSE,
                AiProviderRetryDisposition.AMBIGUOUS_DO_NOT_AUTO_RETRY,
                "AI Provider returned an invalid response",
                null,
                requestId,
                usage,
                latencyMs,
                cause);
    }

    private AiProviderException failure(
            AiProviderErrorType type,
            AiProviderRetryDisposition retryDisposition,
            String message,
            Integer httpStatus,
            String requestId,
            AiProviderUsage usage,
            long latencyMs,
            Throwable cause) {
        return new AiProviderException(
                type,
                retryDisposition,
                message,
                httpStatus,
                requestId,
                usage,
                latencyMs,
                cause);
    }

    private String firstRequestId(HttpHeaders headers) {
        String value = headers.getFirst("x-request-id");
        return value != null ? value : headers.getFirst("request-id");
    }

    private String nullableText(JsonNode node) {
        return node != null && node.isTextual() ? node.textValue() : null;
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000);
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private record RawProviderResponse(
            /** HTTP 状态码。 */
            int statusCode,
            /** 响应头中的 Provider 请求 ID。 */
            String requestIdHeader,
            /** 仅在本方法内解析且从不记录的响应正文。 */
            String body) {}
}
