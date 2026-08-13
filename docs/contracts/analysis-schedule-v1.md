# Analysis Schedule V1

状态：Accepted
接受日期：2026-08-03
适用阶段：Phase 3
实施状态：TASK-030 已实现。

## 1. 默认值与限制

```text
enabled = false
localTime = 02:00:00
timezone = user's IANA timezone
windowDays = 3       (hard max 14)
maxCandidates = 20  (hard max 50)
maxEstimatedTokens = 75000 (hard max 200000)
```

disabled 时 `nextRunAt=NULL`。

## 2. 配置

Schedule 保存：

```text
id
owner
name
promptProfileId
enabled
localTime
timezone
windowDays
maxCandidates
maxEstimatedTokens
nextRunAt
createdAt
updatedAt
```

不冗余 information type、definition key 或 `lastTriggeredAt`。触发时从 Profile/Registry 解析并冻结到 Batch；历史运行从 Batch 查询。

## 3. Timezone

- timezone 必须是有效 IANA Zone ID；
- local time 是用户墙上时间；
- `nextRunAt/scheduledFor` 按 UTC 存储；
- 修改 timezone/local time/enabled 时重新计算下一个未来执行点；
- 不使用服务器本地时区推断用户日程。
- DST gap 向后移动到首个有效墙上时间；
- DST overlap 选择较早 offset，同一本地日期最多触发一次。

## 4. Prompt Version

Schedule 跟随 Profile Active Version。每次触发：

```text
resolve Profile
→ resolve Active Prompt Version
→ resolve Definition current version
→ freeze all IDs/versions into Batch
```

已经创建的 Batch 不因 Profile 后续切换而变化。

## 5. Dispatcher

- 每 30～60 秒扫描一次；
- 查询 `enabled=1 AND next_run_at<=now`；
- 使用 `(enabled,next_run_at,id)` 索引；
- 对 due row 加行锁并重新校验；
- `scheduledFor = nextRunAt`；
- 创建 Batch/NOOP 和推进 `nextRunAt` 在同一明确事务；
- Schedule 不直接调用 Provider。

## 6. Overlap

同一 Schedule 同时最多一个 `PENDING/RUNNING` Batch。存在重叠时为当前 `scheduledFor` 创建 `NOOP` Batch，`skipReason=CONCURRENT_RUN`，然后推进下次执行。

## 7. Misfire

允许 5 分钟 grace：

- grace 内：按原 `scheduledFor` 触发；
- 超过 grace：不补跑历史，创建 NOOP 或记录本计划点跳过并推进到下一个未来执行点；
- 不循环追赶停机期间所有计划。

TASK-030 使用 `NOOP/MISFIRE` 保留可构造 Batch 的历史计划点。若 Profile/Version
配置已经损坏到无法满足 Batch 非空 FK，则不调用 Provider，推进下一未来计划点并返回
脱敏调度结果，避免 due row 永久阻塞。

## 8. 幂等

```text
UNIQUE(scheduleId, scheduledFor)
```

数据库唯一冲突按已创建处理，不作为无限重试故障。Manual Batch 使用另一独立唯一键。

## 9. No Candidate / Invalid Config

无候选、Profile 停用、无 Active Version 或 Definition 不可用时不得调用 Provider，使用 NOOP/FAILED Batch 表达稳定原因并推进日程。

## 10. API 与 UI

用户可以：

- 新建/修改/启停 Schedule；
- 查看下一次执行时间；
- 测试当前配置 Preview；
- 查看历史 Batch。

测试配置复用 Preview，不创建 Invocation。所有操作要求 Session Owner 隔离，写操作要求 CSRF。

TASK-030 冻结 API：

```http
GET  /api/v1/ai/analysis-schedules
POST /api/v1/ai/analysis-schedules
GET  /api/v1/ai/analysis-schedules/{scheduleId}
PUT  /api/v1/ai/analysis-schedules/{scheduleId}
PUT  /api/v1/ai/analysis-schedules/{scheduleId}/status
POST /api/v1/ai/analysis-schedules/{scheduleId}/preview
```

创建请求允许省略 `enabled`，省略时固定为 `false`。完整更新不隐式改变启停状态；
启停必须使用独立 status 接口。Schedule 不提供物理删除，停用使用
`enabled=false` 且 `nextRunAt=NULL`。列表和详情从最近 Scheduled Batch 派生
`lastRun`，不冗余 `lastTriggeredAt`。

Dispatcher 默认启用并每 30 秒扫描一次；每条 Schedule 仍必须由用户显式启用。
若存在可执行候选但 Worker 或 Provider 未配置，则创建
`NOOP/WORKER_DISABLED` 或 `NOOP/PROVIDER_UNAVAILABLE`，不会留下无法消费的
`PENDING` Batch。

浏览器调用失败时继续返回稳定错误码和既有 HTTP 状态码，公开 `message` 使用安全中文。客户端以 `code` 做程序判断，不能依赖 `message`。例如提示词方案已停用时返回 `ANALYSIS_SCHEDULE_PROFILE_DISABLED`，并给出“所选提示词方案已停用，请先启用后再保存定时分析”的可行动中文提示。
## 11. 不包含

- Cron 表达式；
- 每小时/每周复杂规则；
- 多 Worker 分布式调度；
- Spring Batch/Quartz；
- Schedule Run 独立表；
- 通知。
