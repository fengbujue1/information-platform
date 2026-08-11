# Analysis Batch V1

状态：Accepted
接受日期：2026-08-03
适用阶段：Phase 3
实施状态：TASK-028 已实现 Preview；TASK-029 已实现 Confirm、Batch、Budget Guard、Worker 与查询 API。

## 1. Trigger

```text
MANUAL
SCHEDULED
```

两种 Trigger 共用 Candidate Resolver、Batch Creator、Budget Guard、Worker 和 Usage 聚合。

## 2. Preview

Manual 创建 Batch 前必须 Preview。输入：

```text
promptProfileId
windowDays            default 3, hard max 14
maxCandidates         default 20, hard max 50
maxEstimatedTokens    default 75000, hard max 200000
```

Preview：

- 不调用 Provider；
- 不创建 Analysis/Invocation/Batch；
- 解析当前 Active Prompt Version；
- 使用当前 Definition Version；
- 以 `information_item.first_seen_time` 解析 `[windowStart, windowEnd)`；
- 按 `firstSeenTime DESC, id DESC` 稳定排序；
- 解析候选对应当前 Snapshot；
- 排除相同逻辑身份已成功项；
- 计算每项 Estimate 并应用限制。

已实现接口：

```text
POST /api/v1/ai/analysis-batches/preview
```

请求体使用上述四个字段，响应实现第 3 节汇总字段并显式返回 `pendingCount`。
候选版本、Estimate、顺序和决策指纹冻结在签名 Token 中，Confirm 无需信任客户端
回传这些内部字段。接口要求 Session + CSRF 和 Owner 隔离；未配置至少 32 字节的
`INFORMATION_HUB_PREVIEW_HMAC_SECRET` 时返回服务不可用。

## 3. Preview 输出与确认

至少返回：

```text
windowStart
windowEnd
totalInWindow
eligibleCount
alreadyAnalyzedCount
selectedCount
deferredByItemLimitCount
deferredByTokenBudgetCount
estimatedInputTokens
estimatedOutputTokens
estimatedTotalTokens
estimateMethod
expiresAt
previewToken
```

`previewToken` 是 10 分钟有效的 HMAC 签名载荷，包含 Owner、Profile/Version、Definition、绝对窗口、limits、有序候选/Estimate 指纹和随机 `manualRequestId`。

Confirm 必须重新计算相同窗口和指纹。过期、篡改或候选漂移返回稳定冲突错误，不创建 Batch。`(userId, manualRequestId)` 保证重复 Confirm 返回同一 Batch。

TASK-029 冻结接口：

```http
POST /api/v1/ai/analysis-batches/confirm
```

请求仅包含 `previewToken`。成功原子创建或复用 Batch 后返回 HTTP `202 Accepted`
和 `batchId`；HTTP 请求不执行完整批次。重复 Confirm 在 Token 有效期内返回同一 Batch。

Owner 查询接口：

```http
GET /api/v1/ai/analysis-batches?limit=20
GET /api/v1/ai/analysis-batches/{batchId}
GET /api/v1/ai/analysis-batches/{batchId}/progress
```

列表 `limit` 默认 20、范围 1～100。Detail 返回有序 Items；Progress 返回 Item 状态计数、
Provider Usage 报告/缺失数量和 Actual Token 聚合。跨 Owner 与不存在统一为 404。
TASK-030 起列表和详情对 Scheduled Batch 额外返回 `scheduleId` 与 UTC
`scheduledFor`；Manual Batch 对应字段为 `null`。

## 4. Estimate

```text
baseTokens = ceil(UTF-8 bytes / 3)
estimatedInputTokens = ceil((baseTokens + 64) × 1.20)
estimatedOutputTokens = definition.maxOutputTokens
estimatedTotalTokens = input + output
estimateMethod = UTF8_BYTES_DIV3_MARGIN20_V1
```

第一版 Definition `maxOutputTokens=1000`。

## 5. Batch 与 Items

Batch 冻结：

- trigger、owner、schedule/manual request；
- Profile/Prompt Version；
- Definition key/version；
- absolute window；
- limits/counts/Estimate；
-执行状态。

进入 Candidate Limit 的候选按顺序冻结为 Item。Token Budget 延后的候选也保存 Item 和 `TOKEN_BUDGET` 原因；Item Limit 之外只保存 Batch 聚合计数。

Batch Item 唯一：

```text
(batchId, snapshotId)
(batchId, selectionOrder)
```

## 6. 状态

Batch：

```text
PENDING
RUNNING
COMPLETED
PARTIAL_FAILED
FAILED
NOOP
```

没有可执行 Item（无候选或全部被 Token Budget 延后）时使用
`NOOP/NO_EXECUTABLE_ITEMS`。DEFERRED 不算失败；全部可执行 Item 成功或复用成功为
`COMPLETED`，成功/复用与失败并存为 `PARTIAL_FAILED`，全部执行失败或结果不确定为
`FAILED`。

Item：

```text
SELECTED
RUNNING
SUCCEEDED
FAILED
SKIPPED
DEFERRED
```

## 7. Async 与恢复

- 创建 Batch/Items 后立即返回，不在 HTTP 请求中跑完整批次；
- 单并发 Worker 周期轮询；
- `FOR UPDATE SKIP LOCKED` 领取；
- 领取和完成分别短事务；
- Provider 调用不持有事务；
- 进程中断留下的 Invocation 结果不确定时标记 UNKNOWN，不自动重复收费。
- Worker 默认关闭；只有同时启用服务端 Worker 和 Provider 后，Confirm 才允许创建有可执行
  Item 的 Batch，避免留下永远无法处理的 PENDING Batch。

## 8. Usage

Batch Actual Usage 只统计绑定其 Item 的 Invocation。Analysis 后续独立重试不得改变历史 Batch Usage。Provider 无 Usage 时保持 `UNAVAILABLE/NULL`。

## 9. Failure

单项失败不回滚其他已完成项。Batch 根据 Item 结果进入 `COMPLETED/PARTIAL_FAILED/FAILED`。错误信息脱敏，用户可显式重试失败 Analysis，但系统不自动重试不确定请求。

## 10. Access

Preview、Confirm、Batch、Item 和 Usage 都要求 Session Owner 隔离；所有写请求要求 CSRF。
