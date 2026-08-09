package com.informationplatform.hub.ingestion.application;

import com.informationplatform.hub.ingestion.api.dto.InformationEnvelopeRequest;
import com.informationplatform.hub.ingestion.api.dto.IngestionResult;
import com.informationplatform.hub.ingestion.domain.ArchiveContent;
import com.informationplatform.hub.ingestion.domain.ArchiveState;
import com.informationplatform.hub.ingestion.domain.CanonicalContentHasher;
import com.informationplatform.hub.ingestion.domain.ContentFingerprint;
import com.informationplatform.hub.ingestion.domain.EnvelopeNormalizer;
import com.informationplatform.hub.ingestion.domain.NonDestructiveInformationMerger;
import com.informationplatform.hub.ingestion.domain.RawPayloadSecurityValidator;
import com.informationplatform.hub.ingestion.infrastructure.persistence.InformationArchiveRepository;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class InformationIngestionService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(InformationIngestionService.class);

    /** 并发首次写入发生唯一键竞争时允许的最大尝试次数。 */
    private static final int MAX_IDENTITY_RACE_ATTEMPTS = 2;

    /** 将接入 DTO 转换为规范化领域对象。 */
    private final EnvelopeNormalizer normalizer;

    /** 拒绝原始数据中的密码、Token、Cookie 等敏感字段。 */
    private final RawPayloadSecurityValidator rawPayloadSecurityValidator;

    /** 按非破坏性规则合并新旧信息。 */
    private final NonDestructiveInformationMerger merger;

    /** 计算标准化内容及其稳定哈希。 */
    private final CanonicalContentHasher contentHasher;

    /** 组合信息主表、职位扩展表和快照表的持久化适配器。 */
    private final InformationArchiveRepository repository;

    /** 保证三表读取、合并和写入处于同一事务。 */
    private final TransactionTemplate transactionTemplate;

    public InformationIngestionService(
            EnvelopeNormalizer normalizer,
            RawPayloadSecurityValidator rawPayloadSecurityValidator,
            NonDestructiveInformationMerger merger,
            CanonicalContentHasher contentHasher,
            InformationArchiveRepository repository,
            PlatformTransactionManager transactionManager) {
        this.normalizer = normalizer;
        this.rawPayloadSecurityValidator = rawPayloadSecurityValidator;
        this.merger = merger;
        this.contentHasher = contentHasher;
        this.repository = repository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 执行一次采集信息接入。
     *
     * <p>先在事务外完成纯计算和安全校验，再在事务中执行行锁、合并和三表原子写入。
     */
    public IngestionResult ingest(InformationEnvelopeRequest request) {
        long startedNanos = System.nanoTime();
        LOGGER.info(
                "Collector submission received, source={}, informationType={}, itemCount=1",
                request.source(),
                request.informationType());
        // 先统一文本、时间和数组表达，避免等价数据产生不同版本。
        ArchiveContent incoming = normalizer.normalize(request);
        // 在任何数据库写入前递归检查原始数据，防止敏感凭据落库。
        rawPayloadSecurityValidator.validate(incoming.information().rawPayload());
        LOGGER.info(
                "Collector submission validation passed, source={}, informationType={}, itemCount=1",
                incoming.information().source(),
                incoming.information().informationType());

        DuplicateKeyException identityRace = null;
        for (int attempt = 0; attempt < MAX_IDENTITY_RACE_ATTEMPTS; attempt++) {
            try {
                // 同一事务覆盖幂等行锁、主表、扩展表和必要的版本快照。
                IngestionResult result =
                        transactionTemplate.execute(status -> ingestInTransaction(incoming));
                if (result == null) {
                    throw new IllegalStateException("Ingestion transaction returned no result");
                }
                logArchiveCompleted(result, startedNanos);
                return result;
            } catch (DuplicateKeyException exception) {
                // 两个首次请求可能同时未查到记录；唯一键失败后重试即可进入更新路径。
                identityRace = exception;
            }
        }
        throw identityRace;
    }

    private void logArchiveCompleted(IngestionResult result, long startedNanos) {
        int inserted = result.created() ? 1 : 0;
        int updated = !result.created() && result.contentChanged() ? 1 : 0;
        int unchanged = !result.created() && !result.contentChanged() ? 1 : 0;
        LOGGER.info(
                "Information archive completed, informationId={}, received=1, inserted={}, updated={}, unchanged={}, snapshotsCreated={}, durationMs={}",
                result.informationId(),
                inserted,
                updated,
                unchanged,
                result.snapshotCreated() ? 1 : 0,
                elapsedMillis(startedNanos));
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000);
    }

    /** 在单一事务内完成幂等判断、非破坏性合并和版本归档。 */
    private IngestionResult ingestInTransaction(ArchiveContent incoming) {
        // 锁定现有幂等记录，串行化同一来源信息的版本判断。
        Optional<ArchiveState> current = repository.findForUpdate(
                incoming.information().source(),
                incoming.information().informationType(),
                incoming.information().sourceItemId());
        Instant serverTime = Instant.now();

        if (current.isEmpty()) {
            // 新信息补齐默认状态，并创建主记录、业务扩展和首个快照。
            ArchiveContent merged = merger.merge(null, incoming);
            ContentFingerprint fingerprint = contentHasher.fingerprint(merged);
            int versionNo = 1;
            long informationId =
                    repository.insertInformation(merged, fingerprint, versionNo, serverTime);
            repository.saveJob(informationId, merged.job());
            repository.insertSnapshot(informationId, versionNo, merged, fingerprint);
            return new IngestionResult(informationId, true, true, versionNo, true);
        }

        ArchiveState currentState = current.get();
        // 空值不覆盖历史有效值，随后用标准化哈希判断是否产生新版本。
        ArchiveContent merged = merger.merge(currentState.content(), incoming);
        ContentFingerprint fingerprint = contentHasher.fingerprint(merged);
        boolean contentChanged = !fingerprint.hash().equals(currentState.contentHash());
        int versionNo =
                contentChanged
                        ? Math.addExact(currentState.currentVersionNo(), 1)
                        : currentState.currentVersionNo();

        repository.updateInformation(
                currentState.informationId(), merged, fingerprint, versionNo, serverTime);
        repository.saveJob(currentState.informationId(), merged.job());
        // 只有标准化业务内容变化时才追加不可变快照。
        if (contentChanged) {
            repository.insertSnapshot(
                    currentState.informationId(), versionNo, merged, fingerprint);
        }

        return new IngestionResult(
                currentState.informationId(),
                false,
                contentChanged,
                versionNo,
                contentChanged);
    }
}
