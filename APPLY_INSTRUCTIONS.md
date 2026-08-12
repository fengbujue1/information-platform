# Phase 3 收尾文档包使用说明

生成日期：2026-08-07

本包用于将 `information-platform` 的正式项目文档统一切换到：

- Phase 1：Completed
- Phase 2：Completed
- Phase 3：Completed
- Phase 4：Not Started

本次按用户确认，**CI 不作为 Phase 3 收尾阻塞条件**。CI 后续作为独立工程维护事项处理，不再影响 Phase 3 的阶段状态。

## 需要覆盖的文件

将本包中的文件按相同路径覆盖到仓库：

```text
README.md
docs/README.md
docs/CURRENT_STATUS.md
docs/ROADMAP.md
docs/PROJECT_CONTEXT.md
docs/PHASE3_SCOPE.md
docs/tasks/TASK-032.md
```

新增：

```text
docs/PHASE3_COMPLETION.md
```

## 本次不建议修改

以下属于已经 Accepted / Implemented 的设计事实或历史合同，不应为了“阶段收尾”重新改写：

```text
docs/ARCHITECTURE.md
docs/PHASE3_ARCHITECTURE_DRAFT.md
docs/PHASE3_DATA_MODEL_DRAFT.md
docs/DATABASE_DESIGN_PHASE3_DRAFT.md
docs/DATABASE_DESIGN.md
docs/decisions/*
docs/contracts/job-user-relevance-v1.md
docs/contracts/job-user-relevance-v2.md
docs/contracts/information-analysis-v1.md
docs/contracts/analysis-batch-v1.md
docs/contracts/analysis-schedule-v1.md
```

尤其：

- `JOB_USER_RELEVANCE_V1` 保持 version=1 / maxOutputTokens=1000；
- `JOB_USER_RELEVANCE_V2` 保持 version=2 / maxOutputTokens=5000；
- V2 是兼容演进，不回写或篡改 V1 历史语义。

## 建议提交信息

```text
docs(phase3): 完成 Phase 3 收尾并归档阶段状态
```

可选正文：

```text
- 标记 TASK-032 与 Phase 3 完成
- 同步 README、ROADMAP、CURRENT_STATUS 与文档索引
- 正式记录 JOB_USER_RELEVANCE_V2 兼容演进
- 新增 Phase 3 completion summary
- 明确 Phase 4 尚未启动
- CI 转为后续独立工程维护事项
```
