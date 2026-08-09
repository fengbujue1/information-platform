# Codex Phase 4 Workflow

状态：Accepted

实施状态：Completed；TASK-044 已完成 Phase 4 收尾

## 1. 基本原则

一个明确 TASK 对应一个 Codex 实施上下文。

不要一次实现整个 Phase 4。

## 2. 每个 TASK 开始前

至少阅读：

1. 根 `AGENTS.md`
2. 对应模块 `AGENTS.md`
3. `docs/PHASE4_SCOPE.md`
4. `docs/PHASE4_ARCHITECTURE_DRAFT.md`
5. 当前 TASK
6. 当前 TASK 相关 ADR / Contract
7. 后端 TASK：`docs/LOGGING_CONVENTIONS.md`
8. `docs/CURRENT_STATUS.md`

数据库 TASK 额外：

- `docs/DATABASE_DESIGN.md`
- `docs/DATABASE_DESIGN_PHASE4_DRAFT.md`

## 3. 实施前检查

```text
git status
git log --oneline -8
git diff --check
```

数据库 migration 必须先查看真实最新 migration version。

## 4. Scope 强制

不得：

- 提前做下一个 TASK；
- 引入 Redis/Kafka/Vector DB；
- 修改旧 Flyway；
- 把 Recommendation 逻辑塞 Controller；
- Recommendation 调 Provider；
- 新增 Recommendation Cron；
- 自动读取 BOSS 聊天记录；
- 将 Profile 与 AI Prompt Profile 合并；
- 将 CONTACTED 误当 hard exclusion。

## 5. Hard Exclusion

所有 Candidate/Feed 实现必须统一：

```text
NOT_INTERESTED
OR
CONTACTED_NOT_SUITABLE
```

`CONTACTED` 不排除。

不得只在前端隐藏而后端 Candidate 仍继续推荐。

## 6. Trigger

固定：

```text
Analysis Batch COMPLETED/PARTIAL_FAILED
→ eligibility
→ Recommendation Run
```

和：

```text
Manual Refresh
→ Recommendation Run
```

不得新增：

- profile-changed trigger；
- collector trigger；
- feed GET trigger；
- single-analysis trigger；
- independent schedule。

## 7. 日志

严格遵守 `LOGGING_CONVENTIONS.md`。

## 8. 测试

先 targeted，再 integration，再当前 TASK 必要回归。

不得因环境问题反复无意义重跑同一完整命令。

## 9. TASK 完成前

- 测试；
- `git diff --check`；
- Scope review；
- Logging review；
- 更新 TASK 实施记录；
- 更新 CURRENT_STATUS；
- 数据库事实变更同步 DATABASE_DESIGN；
- 架构事实变更同步 ARCHITECTURE；
- 汇报命令和结果。

## 10. Phase 4 收尾

只有 TASK-044 可以：

- 宣布 Phase 4 完成；
- 将实施状态同步 README / ROADMAP / CURRENT_STATUS；
- 输出 Phase 4 Completion Summary；
- 进入后续 Product Polish 阶段。
