# Analysis Batch V1

状态：Draft  
适用阶段：Phase 3

## 1. Trigger

```text
MANUAL
SCHEDULED
```

## 2. Preview

Manual 创建 Batch 前必须 Preview。

Preview 输入至少：

```text
promptProfileId
analysisDefinitionKey
windowDays
maxCandidates
maxEstimatedTokens
```

服务端解析当前用户、Profile Active Version 和平台硬限制。

Preview 不调用 AI Provider。

## 3. Preview 输出

至少：

```text
windowStart
windowEnd
totalInWindow
eligibleCount
alreadyAnalyzedCount
pendingCount
selectedCount
deferredByItemLimit
deferredByTokenBudget

estimatedInputTokens
estimatedOutputTokens
estimatedTotalTokens
estimateMethod
```

## 4. 时间窗口

Preview 返回绝对 windowStart/windowEnd。

Manual Confirm 必须基于同一个冻结窗口创建 Batch，不能重新计算“现在 - N 天”导致候选漂移。

具体 Preview Token / requestId 机制由 TASK-028 冻结。

## 5. Stable Ordering

候选必须稳定排序。

第一版具体字段由 Job Candidate Resolver 合同决定。

不能依赖无 ORDER BY 的数据库自然顺序。

## 6. Candidate Limits

处理顺序：

```text
window
→ eligibility
→ already analyzed skip
→ stable order
→ maxCandidates
→ tokenBudget
```

超限项记录 deferred 原因。

## 7. Batch 状态

至少需要表达：

```text
PENDING
RUNNING
COMPLETED
PARTIAL_FAILED
FAILED
NOOP
```

是否增加 COMPLETED_WITH_LIMIT 由 TASK-029 冻结。

## 8. Batch Items

Batch 创建时冻结需要执行和延期的候选信息，防止运行中数据变化造成候选集合漂移。

## 9. Async

Create Batch 立即返回 batchId。

浏览器通过查询获取进度，不保持长连接等待全部分析完成。

## 10. Usage

Batch Actual Token 由其 Model Invocations 聚合。

必须能展示：

```text
estimatedTotalTokens
actualTotalTokens
```

Actual 不可用时标明部分/不可用，不用 Estimate 替代。

## 11. Idempotency

一个 Item 对应的 Analysis 已成功时，不重复收费调用。

Batch 重启后不得重新执行已经 SUCCEEDED 的 Item。

## 12. Failure

单 Item 失败默认不要求整个 Batch 回滚。

Batch 结束时根据成功/失败情况得到最终状态。

## 13. Retry

Phase 3 不允许对 ambiguous timeout 无脑自动重试。

## 14. Access

用户只能操作自己的 Batch。
