package com.informationplatform.hub.analysis.provider.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiCompatibleProviderConfiguration {

    /** 构造只供 OpenAI-compatible Adapter 使用的带超时 RestClient。 */
    @Bean
    RestClient openAiCompatibleRestClient(
            RestClient.Builder builder, AiProviderProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getTimeout());
        requestFactory.setReadTimeout(properties.getTimeout());
        return builder.requestFactory(requestFactory).build();
    }
}
