package com.informationplatform.hub.ingestion.api;

import com.informationplatform.hub.common.api.ApiResponse;
import com.informationplatform.hub.ingestion.api.dto.InformationEnvelopeRequest;
import com.informationplatform.hub.ingestion.api.dto.IngestionResult;
import com.informationplatform.hub.ingestion.application.InformationIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/collector/items")
public class CollectorItemController {

    /** 负责采集协议校验、幂等合并和归档的应用服务。 */
    private final InformationIngestionService ingestionService;

    public CollectorItemController(InformationIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /** 接收统一采集协议，并根据是否新建返回 201 或 200。 */
    @PostMapping
    public ResponseEntity<ApiResponse<IngestionResult>> ingest(
            @Valid @RequestBody InformationEnvelopeRequest request) {
        // Controller 仅负责协议适配，业务处理全部交给应用服务。
        IngestionResult result = ingestionService.ingest(request);
        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("ITEM_CREATED", result));
        }
        return ResponseEntity.ok(ApiResponse.success("ITEM_UPDATED", result));
    }
}
