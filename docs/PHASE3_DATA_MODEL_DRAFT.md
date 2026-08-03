# Phase 3 Data Model

状态：Accepted
接受日期：2026-08-03
说明：文件名保留 `_DRAFT` 以维持既有链接；物理字段以 `DATABASE_DESIGN_PHASE3_DRAFT.md` 为准。

## 1. 新增模型

Phase 3 需要 8 个持久化模型：

```text
UserAccount
PromptProfile
PromptVersion
InformationAnalysis
AnalysisSchedule
AnalysisBatch
AnalysisBatchItem
ModelInvocation
```

不新增 Preview、Session、Definition、System Prompt、Schedule Run、Usage Summary 或 Cost 表。

## 2. UserAccount

职责：

- 登录身份；
- Prompt/Analysis/Batch/Schedule Owner；
- 用户时区；
- User Usage 聚合键。

规则：

- 用户名 `trim + lower-case`，大小写不敏感唯一；
- 密码只保存安全摘要；
- 默认时区 `Asia/Shanghai`；
- 账号以 `ACTIVE/DISABLED` 管理，不硬删除；
- 初始账号只允许一次性受控 Bootstrap。

## 3. PromptProfile

职责：表示用户的一组长期关注点并选择一个 Analysis Definition。

规则：

- 一个用户多个 Profile；
- 同一用户内名称唯一；
- 保存 `analysisDefinitionKey`；
- `activeVersionId` 可空，指向所属 Profile 的当前 Version；
- Profile 停用而非硬删除；
- Active Version 归属关系由 Service 在事务内校验。

## 4. PromptVersion

职责：保存不可变 User Prompt 历史。

规则：

- `(promptProfileId, versionNo)` 唯一；
- `(promptProfileId, contentHash)` 唯一；
- 相同内容复用已有 Version；
- 版本一经创建不更新正文；
- 最大 8,000 字符由 API/Application Service 校验；
- 被 Profile、Analysis 或 Batch 引用时禁止物理删除。

## 5. InformationAnalysis

职责：保存对确定 Snapshot 的逻辑业务分析结果。

逻辑身份：

```text
userId
+ snapshotId
+ promptVersionId
+ analysisDefinitionKey
+ analysisDefinitionVersion
```

绑定：

- `informationId`：父 Information；
- `snapshotId`：不可变事实版本；
- `promptProfileId/promptVersionId`：用户输入版本；
- Definition key/version：平台规则版本。

状态：

```text
PENDING
RUNNING
SUCCEEDED
FAILED
```

失败信息以脱敏 `failureCode/failureMessage` 保存。结构化结果和 Estimate 在执行前允许为空。

重试语义：

- `SUCCEEDED` 复用；
- `FAILED` 显式重试同一 Analysis；
- 每次实际外部请求创建新 Invocation；
- Provider/Model 变化不创建新的逻辑 Analysis 身份。

## 6. ModelInvocation

职责：记录每一次真实 Provider 调用，是 Actual Token 的唯一事实源。

关系：

```text
ModelInvocation
→ InformationAnalysis
→ UserAccount
→ optional AnalysisBatchItem
```

状态：

```text
RUNNING
SUCCEEDED
FAILED
TIMEOUT
UNKNOWN
```

规则：

- `(analysisId, attemptNo)` 唯一；
- Actual token 仅来自 Provider Usage；
- Usage 缺失时 token 为 `NULL`、`usageStatus=UNAVAILABLE`；
- JSON/Schema 后处理失败时仍保留已报告 Usage；
- 保存 provider request id、finish reason、latency 和脱敏错误；
- 不保存完整 Provider 原始响应、Authorization 或 API Key；
- 不确定的 `TIMEOUT/UNKNOWN` 不自动盲重试。

`batchItemId` 冻结调用的批次归属，防止后来对同一 Analysis 的重试改变历史 Batch Usage。

## 7. AnalysisBatch

职责：冻结一次 Manual Confirm 或 Scheduled Trigger 的执行上下文和聚合统计。

冻结内容：

- owner/trigger；
- schedule 或 manual request id；
- information type；
- definition key/version；
- prompt profile/version；
- absolute window；
- requested limits；
-候选统计；
- Estimate；
-执行状态。

幂等：

```text
Manual:    UNIQUE(userId, manualRequestId)
Scheduled: UNIQUE(scheduleId, scheduledFor)
```

MySQL 允许 UNIQUE 中多个 `NULL`，因此 Manual 的 schedule 字段可为 `NULL`；Application Service/数据库 CHECK 仍需约束 trigger type 对应字段组合。

Batch 状态：

```text
PENDING
RUNNING
COMPLETED
PARTIAL_FAILED
FAILED
NOOP
```

分别保存 `deferredByItemLimitCount` 与 `deferredByTokenBudgetCount`，避免丢失延期原因。

## 8. AnalysisBatchItem

职责：冻结进入 Candidate Limit 后的有序候选和执行决策，支持审计与 Worker 恢复。

规则：

- `(batchId, snapshotId)` 唯一；
- `(batchId, selectionOrder)` 唯一；
- Snapshot 创建 Batch 后不漂移；
- `analysisId` 可空，执行或复用后关联；
- 保存 Item Estimate、decision reason、started/completed 时间；
- Item-limit 之外不逐项保存，只记 Batch 聚合数量；
- Token-budget 延后项需要保存，`decisionReason=TOKEN_BUDGET`。

状态：

```text
SELECTED
RUNNING
SUCCEEDED
FAILED
SKIPPED
DEFERRED
```

## 9. AnalysisSchedule

职责：保存每日规则并在到点时创建 Scheduled Batch。

规则：

- 只保存 `promptProfileId`，不冗余 information type/definition key；
- 触发时由 Profile/Definition Registry 解析并冻结到 Batch；
- 默认 disabled；
- 默认用户本地 02:00；
- timezone 为 IANA 名称；
- disabled 时 `nextRunAt=NULL`；
- 不保存 `lastTriggeredAt`，最近运行从 Batch 派生；
- Schedule 停用而非硬删除。

同一 Schedule 同时最多一个 `PENDING/RUNNING` Batch。重叠触发仍创建幂等 NOOP Batch，以保留计划点审计。

## 10. 既有 Information 关系

真实主键兼容：

```text
information_item.id       BIGINT UNSIGNED
information_snapshot.id   BIGINT UNSIGNED
```

Phase 3 FK 使用同类型并全部 `ON DELETE RESTRICT`。

当前 Snapshot 通过：

```text
information_item.current_version_no
→ information_snapshot(information_id, version_no)
```

Analysis 通过 `snapshotId` 绑定准确历史版本；不新增 `currentSnapshotId`。

## 11. 时间与窗口

- 所有时间点使用 UTC `DATETIME(3)`；
- 用户日程保存 IANA timezone 和本地 `TIME`；
- `FIRST_INGESTED` 映射 `information_item.first_seen_time`；
- 窗口采用 `[windowStart, windowEnd)`；
- 候选排序 `firstSeenTime DESC, informationId DESC`；
- Batch 必须保存绝对窗口，不能只保存相对天数。

## 12. FK 与删除

所有 Phase 3 历史链 FK 使用 `ON DELETE RESTRICT`。

账号、Profile、Schedule 使用状态停用；Prompt Version、Analysis、Invocation、Batch、Batch Item 作为审计事实保留。Phase 3 不实现用户历史硬删除。

## 13. Usage 与 Cost

User/Analysis/Batch Actual Usage 都从 `ModelInvocation` 聚合：

```text
User:       invocation.userId
Analysis:   invocation.analysisId
Batch:      invocation.batchItemId → item.batchId
```

不创建 Usage 汇总表。Phase 3 不保存金额或价格快照，不把 Token 推导值称为真实账单。

## 14. Definition 与 Preview

- Analysis Definition 是代码注册表；
- System Prompt 是代码/资源版本；
- Preview 是无副作用读与估算；
- Manual Preview 使用 10 分钟 HMAC token；
- 第一版不为以上概念建表。
