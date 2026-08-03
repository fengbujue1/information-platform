# Information Analysis V1

状态：Accepted
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
