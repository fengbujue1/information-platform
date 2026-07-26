package com.informationplatform.hub.ingestion.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CollectorRequest(
        /** 采集器实例或实现的稳定标识。 */
        @NotBlank @Size(max = 128) String collectorId,
        /** 本次上报数据所使用的采集器版本。 */
        @NotBlank @Size(max = 64) String collectorVersion) {}
