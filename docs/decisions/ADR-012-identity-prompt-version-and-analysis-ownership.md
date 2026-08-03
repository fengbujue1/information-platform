# ADR-012：Identity MVP、账号级 Prompt Version 与 Snapshot 级 Analysis Ownership

状态：Accepted
接受日期：2026-08-03
日期：2026-08-02

## 背景

用户要求：

- Prompt 在页面配置；
- Prompt 跟登录账号走；
- 不同账号可以有不同关注点；
- Prompt 修改后历史 Analysis 仍可追溯；
- 用户可以查看自己的 Batch、Schedule 和 Token Usage。

因此 Phase 3 不能使用全局 Prompt，也不能只保存 `user_account.prompt`。

## 决策

### Identity

增加最小账号身份：

- username/password；
- secure password hash；
- session；
- current user；
- timezone；
- owner isolation。

浏览器使用同源 Session Cookie。第一版使用单实例内存 Session，不提前引入 Spring Session JDBC。

Collector Bearer Token 不改变。

现有 Job Query API 在 Phase 3 纳入 Session Auth；前端和 E2E 先登录后访问。Cookie 使用 HttpOnly、SameSite=Lax，生产 HTTPS 下 Secure；状态修改请求使用 CSRF。

初始账号只允许从服务端环境变量进行一次性受控 Bootstrap：仅用户表为空时创建，无默认凭据，秘密不入 Git/日志，创建后移除密码变量。

### Prompt

关系：

```text
User
→ Prompt Profile
→ Prompt Version
```

一个用户可有多个 Profile。

修改 Prompt = 创建新 Version，不 UPDATE 历史内容。

同一 Profile 的相同内容 hash 复用已有 Version。User Prompt 最大 8,000 字符。

### Analysis Ownership

Analysis 绑定：

```text
user
+ snapshot
+ prompt version
+ analysis definition version
```

成功的相同逻辑身份默认复用。

### Schedule

Schedule 绑定 Prompt Profile。

真正触发时读取当时 Active Version，并冻结到 Batch。

## 为什么 Analysis 绑定 Snapshot

职位等 Information 会产生历史版本。

只绑定 `information_id` 会让旧 AI 结果错误地看起来对应最新内容。

绑定 Snapshot 可以确保：

```text
source version
↔ prompt version
↔ analysis definition version
↔ model invocation
```

完整追溯。

## System Prompt 边界

普通用户只能修改 User Prompt。

System Prompt、Output Schema 和 Input Projection 由平台控制。

避免用户 Prompt 破坏结构化输出和安全约束。

## 正面影响

- 历史分析可复现。
- Prompt 修改不会污染旧结果。
- 多用户数据天然隔离。
- Schedule 可以自然跟随最新 Prompt。
- 后续可支持多 Prompt Profile。

## 负面影响

- Phase 3 必须增加 Identity。
- Prompt 保存逻辑比一个 TEXT 字段复杂。
- 现有 Web E2E 需要适配登录。

## 不在本 ADR

- 公共注册；
- OAuth；
- 多租户；
- 复杂 RBAC；
- 推荐画像；
- 账号计费。

## TASK-020 冻结结果

- Job Query API 统一要求 Session；
- Collector Bearer Token 过滤链保持独立；
- 使用一次性环境变量 Bootstrap；
- 用户名应用层 trim/lower，数据库大小写不敏感唯一；
- 默认 timezone 为 Asia/Shanghai；
- 不增加 Spring Session JDBC 表。
