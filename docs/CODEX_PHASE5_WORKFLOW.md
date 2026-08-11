# Codex Phase 5 Workflow

状态：Accepted

阶段：Phase 5 — Product Refinement & Stabilization

## 1. 基本原则

Phase 5 使用 Rolling Task Model。

一个明确的 Active TASK 对应一个主要 Codex 实施上下文。

Phase 5 不存在启动时冻结的完整 TASK 列表。

Codex 不得自行猜测并提前实现“下一个 Phase 5 需求”。

## 2. 每次新对话 / 新 TASK 开始前

至少阅读：

1. 根 `AGENTS.md`
2. 当前涉及模块的 `AGENTS.md`
3. `docs/PROJECT_CONTEXT.md`
4. `docs/PHASE5_SCOPE.md`
5. `docs/PHASE5_TASK_MODEL.md`
6. `docs/PHASE5_TASK_INDEX.md`
7. 当前 `docs/tasks/TASK-XXX.md`
8. 当前 TASK 相关 ADR / Contract
9. `docs/CURRENT_STATUS.md`

后端 TASK 额外阅读：

- `docs/LOGGING_CONVENTIONS.md`

数据库 TASK 额外阅读：

- `docs/DATABASE_DESIGN.md`
- 当前最新 Flyway Migration

涉及 Phase 3 / Phase 4 核心行为时：

- 阅读对应 Accepted ADR / Contract；
- 不仅依赖旧聊天上下文。

## 3. Git 基线检查

开始实施前：

```bash
git status
git log --oneline -8
git diff --check
```

如果存在用户未提交修改：

- 不覆盖；
- 不 reset；
- 先识别哪些属于当前 TASK；
- 保留其它工作。

## 4. 新问题分析模式

如果用户只描述了问题、没有明确要求立即修改：

Codex 应：

1. 定位相关代码；
2. 复现或解释当前行为；
3. 给出 root cause 或最可信分析；
4. 明确受影响范围；
5. 给出最小可行解决方案；
6. 判断：
   - 属于当前 TASK 的 Adjustment；
   - 还是新的独立 TASK；
7. 不在用户仍在确认行为时擅自扩大改动。

当 Problem / Expected Behavior / Scope 已经足够清楚，可以进入 READY。

## 5. 简单明确问题

如果用户明确说“直接改”，且需求边界清楚：

- 不需要额外进行一轮形式化“请确认规划”；
- 创建 / 更新 TASK；
- 执行必要检查；
- 实施；
- 测试；
- 进入 VERIFYING。

但如果发现需要：

- 新数据库结构；
- 不兼容 API；
- 新架构组件；
- 大范围重构；
- 安全边界变化；

必须先报告影响，不能把大变化伪装成“小修复”。

## 6. 同一 TASK 的多轮调整

第一版实施后，如果用户反馈：

- “基本可以，但这里还要调整”；
- “这个按钮行为不对”；
- “刷新后还有问题”；
- “继续改一下这个 TASK”；

且仍属于同一个 Problem：

- 不创建新 TASK；
- 更新当前 TASK `Adjustment Log`；
- 状态改回 `IN_PROGRESS`；
- 只实施 Adjustment；
- 重新执行受影响测试；
- 再进入 `VERIFYING`。

## 7. 发现独立问题

实施当前 TASK 时，如果发现另一个独立问题：

默认：

- 不顺手实现；
- 创建下一个 TASK 文档或先在 Task Index 记为 DISCOVERED；
- 写清 Problem；
- 当前 TASK 继续。

如果新问题是 P0：

- 明确报告；
- 可以暂停当前 TASK；
- 将 P0 设为 Active。

## 8. Scope 强制

当前 TASK 不得：

- 顺手做下一个 TASK；
- 为未来需求提前建设复杂设施；
- 无关地重写稳定模块；
- 修改历史 Flyway；
- 绕过 Owner / Session / CSRF；
- 把后端业务规则只做成前端隐藏；
- 向日志输出 Token、Cookie、密码、API Key、Prompt 正文、完整 AI 输入输出或 Collector raw payload；
- 把猜测写成已完成事实；
- 用测试 mock 通过替代真实业务修复。

## 9. Frontend TASK

优先：

- 保持现有 API Contract；
- 使用已有组件和状态模式；
- 必要时拆 composable / component，但不为拆文件而拆文件；
- 同时检查 loading / empty / error / success；
- 对用户可见行为增加或更新组件测试；
- 重要流程按需要更新 Browser E2E。

纯 UI TASK 不得无理由修改后端。

## 10. Backend TASK

必须检查：

- authentication；
- authorization / owner；
- validation；
- transaction boundary；
- idempotency；
- logging；
- error contract；
- test coverage。

后端业务改动遵守 `LOGGING_CONVENTIONS.md`。

## 11. Database TASK

必须：

1. 查看真实最新 migration；
2. 不修改旧 Migration；
3. 使用新版本 Migration；
4. 增加 migration / integration test；
5. 更新 `DATABASE_DESIGN.md`；
6. 检查升级和已有数据兼容；
7. 在 TASK 中记录数据库事实。

## 12. API / Contract TASK

如果外部接口行为变化：

- 找到现有 Contract；
- 判断兼容性；
- 前后端一起调整；
- 更新 Contract；
- 不兼容变化需要显式设计决策；
- 必要时新增 ADR；
- 增加 API / frontend tests。

## 13. Architecture TASK

局部优化不自动升级成 Architecture Change。

只有实际发生：

- 新模块边界；
- 核心生命周期变化；
- 持久化模型重要变化；
- 跨模块通用机制；
- 原 Accepted ADR 被替代；

才同步 `ARCHITECTURE.md` / ADR。

## 14. 测试策略

顺序：

```text
targeted
    ↓
module / integration
    ↓
必要的 frontend / browser
    ↓
必要的 full-stack
```

不得因为环境问题无意义重复运行同一耗时命令。

测试失败时必须区分：

- 当前 TASK 引入；
- 既有失败；
- 环境失败；
- 外部依赖不可用。

不得为了让 CI 绿而随意删除测试或放宽关键断言。

## 15. 用户人工验证

Phase 5 强调实际使用。

Codex 完成第一版后：

- TASK 进入 `VERIFYING`；
- 清楚告诉用户应该验证什么；
- 不在没有用户实际验证时擅自把明显依赖人工体验的 TASK 宣布 DONE。

用户反馈仍有问题：

- 按 Adjustment 流程继续。

## 16. 上下文过长 / 新开 Codex 对话

新对话不要依赖上一窗口记忆。

重新读取：

- AGENTS；
- Phase 5 Scope；
- Task Model；
- Task Index；
- Current Status；
- 当前 TASK；
- 当前 staged / unstaged diff。

然后根据 TASK 的 Implementation / Adjustment Log 继续。

不要重复已经写入 TASK 的已确认问题。

## 17. TASK 完成前统一检查

必须执行或如实说明无法执行：

- 当前 TASK targeted tests；
- 必要回归；
- `git diff --check`；
- Scope review；
- 后端 Logging review；
- Security / Owner review（相关时）；
- Database review（相关时）；
- Contract review（相关时）；
- 更新 TASK Implementation Record；
- 更新 `PHASE5_TASK_INDEX.md`；
- 更新 `CURRENT_STATUS.md`；
- 如架构 / 数据库事实变化，同步正式文档。

## 18. Git 行为

实施和文档更新完成后：

```bash
git diff --check
git status
git add <仅当前 TASK 相关文件>
git diff --cached --check
```

Codex 必须汇报 staged 文件。

默认不得：

```bash
git commit
git push
```

除非用户明确要求。

Codex 应提供建议 commit message。

## 19. TASK DONE

只有满足以下条件才能标记 DONE：

- Expected Behavior 已实现；
- 用户体验类任务完成必要人工验证；
- 相关测试已实际执行并记录；
- 未知失败没有被隐瞒；
- 文档同步完成；
- Adjustment 无未解决内容，或剩余内容被拆为独立 TASK / DEFERRED。

## 20. Phase 5 收尾

只有用户明确决定结束 Phase 5 时执行。

收尾时：

- 盘点 Task Index；
- 不能凭计划写“完成”，必须依据真实 TASK；
- 创建 `docs/PHASE5_COMPLETION.md`；
- README / docs README / ROADMAP / CURRENT_STATUS 同步 Phase 5 完成；
- Deferred 问题必须保留；
- 不自动规划下一 Phase 的大功能。
