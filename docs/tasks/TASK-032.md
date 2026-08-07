# TASK-032：真实模型、定时任务与 Phase 3 E2E 验收

状态：DONE  
所属阶段：Phase 3  
优先级：P0  
负责人：User + Codex  
完成日期：2026-08-07

## 1. 目标

使用 JOB 数据、Fake Provider 和受控真实 Provider 验证 Phase 3 从登录到 Prompt、Analysis、Preview、Batch、Usage、Schedule 的完整链路，并完成 Phase 3 收尾。

## 2. 已覆盖范围

自动化：

- Identity；
- Prompt Version；
- Fake Provider；
- Single Analysis；
- Preview；
- Batch；
- Budget；
- Schedule；
- Web E2E。

真实 Provider 联调：

- Provider 参数；
- 少量 JOB Snapshot；
- Estimated Token；
- Actual Token；
- Structured Output；
- timeout；
- output token budget；
- Usage；
- Schedule；
- 泄露边界。

## 3. 验收结果

- [x] Java 本地测试通过
- [x] Vue typecheck / unit test / build 通过
- [x] Browser E2E 通过
- [x] Fake Provider Full-stack E2E 通过
- [x] Small-scale real Provider 受控联调
- [x] Estimated Token 路径验证
- [x] Actual Token / Provider Usage 路径验证
- [x] 失败调用 Usage 规则验证
- [x] Manual Batch limits 验证
- [x] Schedule 默认关闭规则验证
- [x] Schedule trigger / idempotency / overlap / misfire 规则验证
- [x] 用户 Usage 查询
- [x] rawPayload / API Key 安全边界检查
- [x] README / ROADMAP / CURRENT_STATUS 收尾
- [x] Phase 3 标记完成
- [x] Phase 4 未自动启动

CI 不作为本次 TASK / Phase 3 关闭的阻塞条件，后续作为独立工程维护事项处理。

## 4. 关键实现结果

完整主链：

```text
Login
→ Prompt Profile / Version
→ Snapshot
→ Single Analysis / Preview
→ Manual / Scheduled Batch
→ Worker
→ Provider
→ Structured Output
→ Information Analysis
→ Model Invocation
→ Actual Usage
→ Web
```

## 5. 验收期间修复

### Batch 状态

前端 Batch 终态已与后端 Contract 对齐：

```text
COMPLETED
PARTIAL_FAILED
```

### Provider timeout

真实 Provider 联调后默认 timeout 调整为：

```text
2m
```

### JOB_USER_RELEVANCE V2

原 V1：

```text
version = 1
maxOutputTokens = 1000
```

真实 Provider 联调发现部分结构化输出可能被 1000 token 截断，因此新增：

```text
version = 2
maxOutputTokens = 5000
```

V1 不修改。

V2 复用：

- Input Projection V1；
- System Prompt Version 1；
- Output Schema Version 1；
- Validator V1。

新的 Analysis 使用 Registry 当前最高版本。

## 6. 安全默认值

最终保持：

```text
Provider enabled = false
Batch Worker enabled = false
Schedule enabled = false
```

真实调用必须显式开启。

API Key、Bootstrap 密码、Cookie、Collector Token、完整 Provider 原始响应不得进入 Git、前端或日志。

## 7. Phase 3 完成确认

- [x] Scope 已实现
- [x] Architecture 已实现
- [x] Data Model / Database 已实现
- [x] ADR / Contract 已落地
- [x] Identity 已实现
- [x] Prompt 已实现
- [x] Analysis 已实现
- [x] Preview / Batch / Budget 已实现
- [x] Schedule 已实现
- [x] Web 已实现
- [x] Actual Usage 已实现
- [x] Fake Provider E2E 已实现
- [x] 真实 Provider 受控联调已完成
- [x] Phase 3 文档已收尾
- [x] Phase 3 结束
- [x] Phase 4 未自动启动

## 8. 后续

Phase 3 归档后：

- 不再新增 Phase 3 功能；
- 缺陷、安全、依赖和运行参数维护可以继续；
- CI 单独作为工程维护事项；
- 推荐、画像、Top N、反馈学习进入独立 Phase 4；
- Retrieval / RAG / Vector Search 按真实需求另行规划。
