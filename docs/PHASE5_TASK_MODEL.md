# Phase 5 Rolling Task Model

状态：Accepted

## 1. 目的

Phase 5 的任务无法在阶段开始时完整预测。

本文件定义：

- 什么时候创建 TASK；
- TASK 如何编号；
- 多轮 Codex 调整如何记录；
- 什么时候继续原 TASK；
- 什么时候拆新 TASK；
- TASK 如何结束。

## 2. 编号

Phase 4 最终编号：

```text
TASK-044
```

Phase 5 下一个编号：

```text
TASK-045
```

默认连续递增：

```text
TASK-045
TASK-046
TASK-047
...
```

### A/B 后缀

仅在以下特殊情况允许：

- 后续编号已经占用；
- 当前任务实施中发现一个必须独立实施、但逻辑上必须插入当前序列的纠偏任务；
- 重新编号已有 TASK 会造成历史混乱。

否则不要使用 `TASK-045A`、`TASK-045B`。

## 3. TASK 状态

Phase 5 使用：

### DISCOVERED

已确认存在问题，但：

- 根因还在调查；
- Expected Behavior 还没有确认；
- 暂时不应直接实施。

### READY

已经确认：

- Problem；
- Expected Behavior；
- Scope；
- Acceptance Criteria。

可以开始编码。

### IN_PROGRESS

Codex 正在实施或正在处理当前 TASK 的后续 Adjustment。

### VERIFYING

代码已经完成自动化检查，等待：

- 用户实际页面验证；
- 真实环境验证；
- 或最终验收。

### DONE

用户或约定的验收流程确认完成。

### DEFERRED

问题真实存在，但明确暂缓。

必须记录原因，不得把未解决问题伪装成 DONE。

## 4. 标准生命周期

```text
DISCOVERED
    ↓
READY
    ↓
IN_PROGRESS
    ↓
VERIFYING
   ↙     ↘
调整      验收
 ↓         ↓
IN_PROGRESS DONE
```

可根据问题清晰度直接：

```text
READY → IN_PROGRESS
```

不要求每个简单问题都先经过 DISCOVERED。

## 5. 同一 TASK 的多轮调整

Phase 5 明确允许：

```text
Implementation V1
    ↓
Manual Verification
    ↓
Adjustment 1
    ↓
Implementation V2
    ↓
Adjustment 2
    ↓
Final Verification
```

以上都可以属于一个 TASK。

在 TASK 中使用：

```text
## Adjustment Log
```

记录重要的后续调整。

不要产生：

```text
TASK-045-FIX
TASK-045-FIX2
TASK-045-FINAL
```

## 6. 判断是否新 TASK

使用这个判断：

> 新发现的问题是否能用一句独立的话描述，并拥有独立的验收标准？

如果答案是 NO：

- 继续当前 TASK。

如果答案是 YES：

- 创建新的 TASK。

### 示例：继续原 TASK

TASK-045：

> 优化推荐列表“不感兴趣”的交互。

实施后发现：

- 卡片消失动画不自然；
- undo 后排序没恢复；
- loading 闪烁；
- 分页数量未同步。

只要这些仍然属于同一个操作体验，继续 TASK-045。

### 示例：新 TASK

处理 TASK-045 时发现：

> Recommendation Worker 的 stale recovery 可能导致多实例重复执行。

这是独立的后端可靠性问题。

创建 TASK-046。

## 7. 当前 TASK 中发现独立问题

默认行为：

1. 当前 TASK 不越界实施；
2. 在 Phase 5 Task Index 的 Backlog / Discovered 中记录；
3. 分配下一个 TASK 编号；
4. 状态设为 `DISCOVERED`；
5. 当前 TASK 继续完成。

如果新问题是 P0：

- 可以暂停当前 TASK；
- 明确记录原因；
- 将 P0 TASK 设为 Active；
- 处理完成后再恢复原 TASK。

## 8. TASK 不应提前锁死实现

TASK 必须优先描述：

- Problem；
- Expected Behavior；
- Scope；
- Acceptance Criteria。

调查前不要强制：

- 必须新增哪个类；
- 必须用 Redis；
- 必须用 SSE；
- 必须拆某种架构；
- 必须增加某张表。

只有在调查后确认的实现决策，才写入：

```text
Implementation Decision
```

## 9. 数据库 TASK

如果 TASK 需要数据库变化：

- 先检查实际最新 Flyway version；
- 不修改 V1～当前已经发布的 Migration；
- 创建新的 migration；
- 更新 `DATABASE_DESIGN.md`；
- 如为重要结构决策，增加 ADR；
- 增加 migration / integration test；
- TASK 验收记录必须写明数据库事实。

## 10. Contract TASK

如果已有外部 API 行为变化：

- 检查现有 Contract；
- 判断是否 backward compatible；
- 兼容变化更新现有 Contract 的实施事实，或按项目版本规则演进；
- 不兼容变化必须先明确决策；
- 必要时新增 ADR；
- 前后端同时更新测试。

## 11. Architecture TASK

只有实际代码边界发生重要改变时更新 `ARCHITECTURE.md`。

不要为了普通 UI 调整、字段文案或局部 Service 修改创建架构文档。

## 12. TASK Index

`PHASE5_TASK_INDEX.md` 是 Phase 5 动态索引。

至少维护：

- Next Task Number；
- Active Task；
- Discovered / Backlog；
- Verifying；
- Done；
- Deferred。

真实任务正文仍放：

```text
docs/tasks/TASK-XXX.md
```

保持与既有仓库一致。

## 13. TASK 完成

TASK 进入 DONE 前：

- 当前问题无法再按原复现方式复现；
- Expected Behavior 已验证；
- 相关测试通过；
- 必要文档已同步；
- `git diff --check` 通过；
- TASK Implementation Record 完成；
- Task Index 更新；
- `CURRENT_STATUS.md` 更新当前 Active 状态；
- Codex 对当前 TASK 文件执行 `git add`；
- 不自动 commit / push。

## 14. Phase 5 最终收尾

Phase 5 不设置一个预先固定编号的“最终 TASK”。

当用户明确决定结束 Phase 5 时：

- 盘点 `PHASE5_TASK_INDEX.md`；
- Active 不得遗留未说明状态；
- 输出实际完成和 Deferred 列表；
- 根据 `PHASE5_COMPLETION_TEMPLATE.md` 创建 `PHASE5_COMPLETION.md`；
- 同步 README / docs README / ROADMAP / CURRENT_STATUS；
- 将 Phase 5 标为完成。
