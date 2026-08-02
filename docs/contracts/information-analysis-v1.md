# Information Analysis V1

状态：Draft  
适用阶段：Phase 3

## 1. 目的

定义通用 Information Analysis 的身份、状态、结果、幂等和 Usage 规则。

## 2. 逻辑身份

默认：

```text
userId
+ snapshotId
+ promptVersionId
+ analysisDefinitionKey
+ analysisDefinitionVersion
```

相同身份已有 `SUCCEEDED` 时普通请求不得重复调用模型。

## 3. 绑定

必须保存：

- userId；
- informationId；
- snapshotId；
- informationType；
- promptProfileId；
- promptVersionId；
- definitionKey/version；
- analysisPurpose。

## 4. 状态

至少：

```text
PENDING
RUNNING
SUCCEEDED
FAILED
```

如需增加 UNKNOWN / REVIEW_REQUIRED，由 TASK-027 根据 ambiguous timeout 方案决定。

## 5. Result

通用表保存：

```text
resultJson
summary
relevanceScore (nullable)
```

具体 JSON Schema 由 Analysis Definition 指定。

非法 JSON / Schema 不得标记 SUCCEEDED。

## 6. Provider Metadata

通过 Model Invocation 记录：

- provider；
- model；
- providerRequestId；
- latency；
- usage；
- attempt；
- error。

## 7. Token

Analysis 保存 Estimate。

Actual Usage 的事实来源是 Model Invocation。

Provider 返回 Usage 后，即使后续业务解析失败，也必须保留 Usage。

## 8. Provider 切换

Provider / Model 不参与默认逻辑幂等。

改变全局模型配置不能自动让所有成功 Analysis 失效。

如需重新分析：

- 显式 reanalysis；
- 或 Definition 升级。

Phase 3 是否开放 reanalysis UI 由 TASK-027 决定，默认非必需。

## 9. Input

不默认读取 rawPayload。

Definition 决定标准化字段投影。

## 10. 来源事实

Analysis 结果不得写回 Information / Job / Snapshot 来源事实。

## 11. 查询权限

普通用户只能读取自己的 Analysis。

## 12. 普通 GET

查询 Analysis 不允许暗中触发模型调用。
