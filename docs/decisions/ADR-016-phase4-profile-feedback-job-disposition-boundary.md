# ADR-016：Recommendation Profile、Feedback 与 Job Disposition 分离

状态：Accepted

## 背景

Phase 4 同时需要：

1. 长期推荐偏好；
2. 对推荐的喜欢/不喜欢；
3. 实际求职过程中“已经联系 BOSS”“联系后确认不合适”的状态。

三者语义不同，不能混成一个字段。

## 决策

### Recommendation Profile

结构化长期偏好：

```text
targetRoles
skills
cities
remote
salary
excludedKeywords
```

绑定一个 AI Prompt Profile。

### Feedback

```text
NONE
INTERESTED
NOT_INTERESTED
```

表达：

> 用户对这个 Information 本身的推荐反馈。

### Job Disposition

```text
NONE
CONTACTED
CONTACTED_NOT_SUITABLE
```

表达：

> 用户在真实求职流程中对这个岗位处理到什么状态。

`CONTACTED`：

- 已经通过 BOSS 原始链接联系；
- 仍可能继续沟通；
- 不自动排除。

`CONTACTED_NOT_SUITABLE`：

- 已联系/沟通；
- 用户确认不合适；
- 当前 Feed 立即隐藏；
- 后续 Recommendation Candidate hard exclude。

### Hard exclusion

```text
feedbackState == NOT_INTERESTED
OR
jobDisposition == CONTACTED_NOT_SUITABLE
```

### 状态恢复

用户可以把状态改回 NONE，取消 hard exclusion。

## 数据粒度

Interaction 绑定：

```text
userId + informationId
```

不是 Snapshot，因此岗位内容更新后状态仍然保留。

## BOSS 同步

Phase 4 不自动读取 BOSS 聊天记录。

原因：

- 登录态和账号风控；
- 页面/API 稳定性；
- Collector 当前职责；
- 不应扩大 Phase 4 Scope。

由用户手动维护 disposition。

## 不采用

- CONTACTED 自动排除；
- 把 CONTACTED 直接等同 NOT_INTERESTED；
- Feedback 自动修改 Profile；
- Phase 4 训练学习模型。
