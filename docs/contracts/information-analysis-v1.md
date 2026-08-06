# Information Analysis V1

状态：Accepted
实施状态：TASK-027 已实现（2026-08-05）
接受日期：2026-08-03
适用阶段：Phase 3

## 1. 逻辑身份

```text
userId
+ snapshotId
+ promptVersionId
+ analysisDefinitionKey
+ analysisDefinitionVersion
```

Provider 和 Model 不属于逻辑身份。

## 2. 绑定与一致性

Analysis 必须绑定：

- 当前认证用户；
- `information_item.id`；
- 不可变 `information_snapshot.id`；
- Prompt Profile / Version；
- Definition key/version；
- information type/purpose。

Service 在事务中验证 Snapshot 属于 Information、Version 属于 Profile、Profile 属于用户。所有历史 FK 使用 `ON DELETE RESTRICT`。

## 3. 状态与重试

```text
PENDING
RUNNING
SUCCEEDED
FAILED
```

- 相同身份 `SUCCEEDED`：复用；
- 相同身份 `FAILED`：仅显式重试，复用同一 Analysis；
- 每次真实 Provider 请求追加一个 attempt 递增的 Invocation；
- 不确定 timeout 不自动重试；
- 新 Prompt Version 或 Definition Version 才产生新的逻辑身份。

## 4. Result

`resultJson` 只保存 Output Schema 验证成功的数据。

第一版可保存查询投影：

- `relevanceScore`：0..100；
- `summary`：从 `resultJson` 提取的页面摘要。

失败保存稳定、脱敏的 `failureCode/failureMessage`。不得把无效 Provider 原始响应写入数据库。

## 5. Provider Metadata 与 Usage

Provider Request 元数据和 Usage 全部属于 `ModelInvocation`，不塞进 Analysis：

- provider/model；
- request id；
- attempt/status/finish reason；
- latency；
- error code/message；
- input/output/total/cached/reasoning tokens；
- usage status。

Actual Token 只能来自 Provider Usage。Estimate 保存在明确的 `estimated*` 字段，不能替代 Actual。

## 6. Input

模型输入来自 Snapshot 对应的标准化投影：

- 不默认发送 `rawPayload`；
- 不读取认证信息或采集秘密；
- 来源文本作为不可信数据分隔；
- 输入不足时允许模型返回 missing data，不得编造来源事实。

## 7. 查询权限

所有读取通过认证用户过滤 Owner。普通 GET 不触发 AI，不创建 Invocation，不产生 Token。

Phase 3 可新增按 `snapshotId` 的内部 Reader，但不得把 Snapshot raw payload 暴露给前端。

## 8. HTTP API

执行、显式重试或复用单条 Analysis：

```http
POST /api/v1/ai/analyses
```

请求字段：

- `informationId`：必填；
- `snapshotId`：可选，为空时解析 Information 当前 Snapshot；
- `promptProfileId`：必填，必须属于当前认证用户；
- `retryFailed`：可选，仅 `true` 时允许重试同一逻辑身份的 FAILED Analysis。

按 Owner 查询既有 Analysis：

```http
GET /api/v1/ai/analyses/{analysisId}
```

POST 使用同源 Session 与 CSRF；Owner 只来自认证上下文，不接受客户端传入。跨 Owner 与不存在统一按 404 处理。AI disabled 或 Provider 配置不完整在任何 Analysis/Invocation 写入及网络访问前失败。

## 9. Estimate 与事务边界

- 单条 Estimate 使用版本化方法 `UTF8_BYTES_DIV3_MARGIN20_V1`，只写入 `estimated*` 字段；
- 输入估算覆盖稳定消息的 role、换行和 content UTF-8 字节，输出估算使用冻结的 `maxOutputTokens`；
- 准备事务冻结 Owner Profile、Active Prompt Version、Definition、Snapshot、逻辑身份与 Invocation；
- Provider HTTP 调用在数据库事务外执行；
- Provider 成功、输出校验失败、Provider 失败分别在新的短事务中完成状态与 Usage 持久化；
- Provider 已返回 Usage 但输出校验失败时，Invocation 保持成功并保留 Actual Usage，Analysis 标记 FAILED；
- timeout/unknown 不自动重试。
## 10. 用户维度 Usage API

TASK-031 为 Phase 3 Web 补齐当前 Owner 的实时 Actual Usage 查询：

```http
GET /api/v1/ai/usage
```

响应一次返回 `today`、`month` 和 `allTime`，每个范围包含：

- Invocation 总数；
- `REPORTED/UNAVAILABLE` 数量；
- Actual input/output/total token；
- 有时间范围的 UTC `periodStart/periodEnd`。

今日和本月按当前账号 IANA timezone 计算自然日/月边界，再转换为 UTC 半开区间。时间归属使用 `ai_model_invocation.created_at`，并复用 `(user_id, created_at)` 索引；累计范围不限制时间。

该接口只从当前 Session Owner 的 `ai_model_invocation` 聚合。只有 `usage_status=REPORTED` 的 Provider 字段进入 Actual 合计；完全未知时 token 保持 `NULL`，不得使用 Analysis/Batch Estimate 补算。接口为只读 GET，不要求 CSRF，但要求认证 Session。跨 Owner 数据不会进入聚合。

第一版不创建 Usage Summary/Cost 表，不提供金额、价格或账单语义。
