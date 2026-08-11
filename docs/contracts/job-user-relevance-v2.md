# Job User Relevance V2

状态：Accepted / Implemented
接受日期：2026-08-07
Definition：`JOB_USER_RELEVANCE_V2`

## 1. 目标

V2 是 `JOB_USER_RELEVANCE_V1` 在真实 Provider 验收阶段产生的兼容演进版本。

真实 Provider 联调发现部分结构化响应在 V1 的 1000 output token 上限下可能被截断，因此增加新的 Definition Version，而不修改已经冻结的 V1。

## 2. 与 V1 的差异

V2 仅调整 Definition 执行输出预算：

```text
analysisDefinitionVersion = 2
maxOutputTokens = 5000
```

以下内容继续复用 V1：

```text
informationType = JOB
analysisPurpose = USER_RELEVANCE
systemPromptVersion = 1
outputSchemaVersion = 1
Input Projection = V1
Output Validation = V1
Source Content Boundary = V1
```

System Prompt 和 Output Schema 的业务语义没有变化。

## 3. 历史兼容

JOB_USER_RELEVANCE_V1 保持注册并保持：

```text
analysisDefinitionVersion = 1
maxOutputTokens = 1000
```

已有 Analysis、Batch Item 等冻结了 Definition Version 的历史事实继续按 V1 解析。

新的 Analysis 使用 AnalysisDefinitionRegistry.requireCurrent(...) 解析当前最高版本，因此默认使用 V2。

不得通过修改 V1 的 maxOutputTokens 来实现本次调整。

## 4. Provider 配置关系

Provider 的 max-output-tokens 是运行时允许上限，不代替 Definition 自身的输出预算。

当前推荐配置上限：

```text
Provider maxOutputTokens = 5000
V1 maxOutputTokens = 1000
V2 maxOutputTokens = 5000
```

实际发送给 Provider 的 max_tokens 由当前 Analysis Definition 决定。
