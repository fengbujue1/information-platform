# Phase 5 Existing Document Change Plan

状态：Accepted

## 1. 目标

Phase 5 启动时同步现有事实文档。

只声明：

- Phase 4 已完成；
- Phase 5 已开始；
- Phase 5 使用 Rolling Task Model；
- Next Task Number = TASK-045；
- 当前启动时没有 Active TASK。

不得在启动文档中提前宣称未知 Phase 5 优化已经实现。

## 2. README.md

在当前状态增加：

```text
Phase 1、Phase 2、Phase 3、Phase 4 已完成。
Phase 5 — Product Refinement & Stabilization 进行中。
```

增加 Phase 5 简介：

- UI / UX refinement；
- backend behavior refinement；
- integration / stability；
- production hardening；
- Rolling Task Planning；
- TASK 从 045 开始，按实际问题创建。

文档入口增加：

- `PHASE5_PACKAGE_MANIFEST.md`
- `docs/PHASE5_SCOPE.md`
- `docs/PHASE5_TASK_MODEL.md`
- `docs/PHASE5_TASK_INDEX.md`
- `docs/CODEX_PHASE5_WORKFLOW.md`
- `docs/CODEX_PHASE5_INTERACTION_GUIDE.md`

## 3. docs/README.md

`当前阶段` 改为：

```text
Phase 3、Phase 4 已归档。
Phase 5 进行中。
```

增加 Phase 5：

- Scope；
- Task Model；
- Task Index；
- Task Template；
- Codex Workflow；
- Codex Interaction Guide；
- Existing Document Change Plan。

TASK 区域不要预列未来 TASK。

可写：

```text
Phase 5 TASK 从 TASK-045 开始，按实际发现的问题滚动创建。
当前任务以 PHASE5_TASK_INDEX.md 为准。
```

## 4. ROADMAP.md

在 Phase 4 后增加：

```markdown
## Phase 5：Product Refinement & Stabilization

状态：进行中。

Phase 5 不预先冻结完整 TASK 列表，采用 Rolling / Just-in-Time Task Planning。

主要范围：

- 已有页面 UI / UX 优化；
- 后端行为优化和缺陷修复；
- 前后端集成问题；
- 测试 / CI；
- 安全和可靠性加固；
- 生产准备问题；
- 实际使用后确认必要的小范围重构。

TASK 从 TASK-045 开始，实际发现并确认独立问题后再创建。

默认不在 Phase 5 中新增新的大型 Information Type 或大规模架构能力。
```

Future 章节保持 Future，不因为 Phase 5 启动而提前实现。

## 5. CURRENT_STATUS.md

启动时：

```text
更新时间：2026-08-11
当前分支：dev
```

当前阶段增加：

```text
Phase 5：进行中。
```

当前任务：

```text
Phase 5 已启动。
当前无 Active TASK。
Next Task Number：TASK-045。
具体任务以 docs/PHASE5_TASK_INDEX.md 为准。
```

后续每个 TASK：

- 开始时更新 Active Task；
- VERIFYING 时可保持当前 TASK 并标状态；
- DONE 后更新 Recently Completed 或当前任务；
- 不需要复制 TASK 全部实施细节。

## 6. 根 AGENTS.md

建议新增 Phase 5 工作约束：

```markdown
## Phase 5 实施约束

- Phase 5 使用 Rolling / Just-in-Time Task Planning，不预先创建完整 TASK 列表。
- Phase 5 TASK 从 TASK-045 开始，以 docs/PHASE5_TASK_INDEX.md 为动态索引。
- 同一问题实施后的多轮调整继续记录在原 TASK 的 Adjustment Log 中，不创建 FIX/FIX2 TASK。
- 新问题只有在可独立描述、独立验收时才创建新 TASK。
- 当前 TASK 不顺手实施其它独立问题。
- 数据库、Contract、Architecture 和 ADR 只在具体 TASK 确实改变对应事实时更新。
- Codex 完成当前 TASK 后运行相关测试与 git diff --check，并 git add 当前 TASK 相关文件；默认不 commit / push。
```

根 AGENTS 中已有长期项目原则继续有效。

## 7. docs/AGENTS.md

当前通用 Documentation 规则已经适用。

Phase 5 启动默认无需修改。

如果希望强化 TASK 事实规则，可补充：

```text
Phase 5 Task Index 只记录状态与索引；
具体 Problem、Adjustment、Implementation、Verification 写入对应 TASK。
```

不是必须修改项。

## 8. ARCHITECTURE.md

Phase 5 启动时不修改架构事实。

只有具体 TASK 实际改变架构时才更新。

## 9. DATABASE_DESIGN.md

Phase 5 启动时不修改数据库事实。

只有具体 TASK 新增 Flyway / 改变数据模型时才更新。

不创建空的 `DATABASE_DESIGN_PHASE5_DRAFT.md`。

## 10. Contract / ADR

Phase 5 启动时不新增空 Contract / ADR。

具体 TASK：

- API 行为变化 → Contract；
- 重要长期决策 → ADR；
- 只是局部实现 → 不需要为“文档齐全”强行创建 ADR。

## 11. TASK 文件

真实 TASK 继续使用现有目录：

```text
docs/tasks/TASK-045.md
docs/tasks/TASK-046.md
...
```

不要新建第二套 `docs/phase5/tasks/`，避免与仓库既有 TASK 体系分裂。

## 12. Phase 5 收尾

收尾时再同步：

- README；
- docs/README；
- ROADMAP；
- CURRENT_STATUS；
- `PHASE5_COMPLETION.md`；
- 最终 Task Index；
- 实际受影响的 Architecture / Database / ADR / Contract。

不得根据最初计划写完成项，必须依据真实代码、测试和 TASK Implementation Record。
