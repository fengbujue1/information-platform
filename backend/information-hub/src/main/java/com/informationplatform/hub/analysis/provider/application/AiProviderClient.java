package com.informationplatform.hub.analysis.provider.application;

import com.informationplatform.hub.analysis.provider.domain.AiProviderRequest;
import com.informationplatform.hub.analysis.provider.domain.AiProviderResult;

/** 与具体模型供应商解耦的单次 AI 请求边界。 */
public interface AiProviderClient {

    /** 返回用于 Invocation 元数据的稳定 Provider 标识。 */
    String providerId();

    /** 返回本次 Client 配置使用的模型标识，供调用前创建 Invocation 元数据。 */
    String modelName();

    /** 在持久化 RUNNING Invocation 前校验请求与配置；不得发起网络调用。 */
    void validateRequest(AiProviderRequest request);

    /** 执行一次请求；实现不得在内部进行自动重试。 */
    AiProviderResult execute(AiProviderRequest request);
}
