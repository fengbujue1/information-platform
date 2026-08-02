# Analysis Schedule V1

状态：Draft  
适用阶段：Phase 3

## 1. 目标

允许用户配置每天固定本地时间自动执行与 Manual Batch 相同的分析逻辑。

## 2. 默认值

```text
enabled = false
frequency = DAILY
localTime = 02:00
```

默认关闭，必须用户主动开启。

## 3. 配置

至少：

```text
name
promptProfileId
analysisDefinitionKey
informationType
windowDays
maxCandidates
maxEstimatedTokens
localTime
timezone
enabled
```

## 4. Timezone

使用 IANA 名称，例如：

```text
Asia/Shanghai
America/Los_Angeles
```

不使用固定 `UTC+8` 替代时区。

## 5. Prompt Version

Schedule 绑定 Profile。

触发时：

```text
resolve active prompt version
→ freeze into batch
```

之后 Profile 修改不影响已创建 Batch。

## 6. Engine Reuse

Schedule 不直接调用模型。

```text
Scheduler
→ Create Scheduled Batch
→ existing Batch Worker
```

必须复用 Manual 的：

- Candidate Resolver；
- Idempotency；
- Estimate；
- Budget；
- Worker；
- Provider；
- Usage。

## 7. Overlap

同一个 Schedule 同时最多一个活动 Batch。

如果计划时间到达时旧 Batch 仍 PENDING/RUNNING：

```text
skipReason = CONCURRENT_RUN
```

## 8. Misfire

Phase 3 默认：

```text
SKIP
```

服务器停机错过时间不自动补跑。

## 9. Trigger Idempotency

同一：

```text
scheduleId + scheduledFor
```

最多创建一次运行。

## 10. No Candidate

无待分析候选时不调用 Provider，并保留可查看运行结果。

## 11. Limits

Schedule 使用与 Manual 相同的：

- windowDays；
- maxCandidates；
- maxEstimatedTokens；
- platform hard limits。

不得拥有“定时任务专用无限额度”。

## 12. UI

必须支持：

- 查看；
- 创建；
- 编辑；
- 启用；
- 关闭；
- 测试当前配置 Preview；
- 查看上次运行；
- 查看下次运行；
- 跳转 Batch 历史。

## 13. Phase 3 限制

不支持：

- 任意 Cron；
- 每小时；
- 每分钟；
- 补跑历史多次；
- 任务依赖；
- 分布式调度。
