# TASK-XXX：任务标题

状态：DISCOVERED

所属阶段：Phase 5

类型：Frontend / Backend / Integration / Infrastructure / Documentation

优先级：P0 / P1 / P2 / P3

创建日期：YYYY-MM-DD

## 1. Problem

描述实际观察到的问题。

优先记录：

- 出现在哪个页面 / API / 流程；
- 如何复现；
- 当前实际行为；
- 为什么当前行为不合理。

不要在这一节直接把某个实现方案写成需求。

## 2. Expected Behavior

描述任务完成后应该表现为什么。

必须尽量可验证。

## 3. Scope

### Included

- 当前任务必须解决的内容。

### Not Included

- 与本问题无关的顺手重构；
- 未确认的未来需求；
- 不必要的基础设施；
- 其它独立问题。

## 4. Preconditions / Existing Constraints

记录本 TASK 不能破坏的已有边界。

例如：

- Session / CSRF；
- Owner isolation；
- Phase 3 Analysis snapshot immutability；
- Phase 4 Recommendation trigger boundary；
- Logging Convention；
- Flyway migration 规则。

没有特别约束时写：

```text
遵守根 AGENTS.md、模块 AGENTS.md 和现有 Accepted ADR / Contract。
```

## 5. Investigation Notes

在分析和 Codex 多轮交互过程中更新。

可记录：

- root cause；
- 受影响文件 / 模块；
- 现有测试；
- 数据状态；
- 被排除的错误假设；
- 与用户确认的行为细节。

不要把尚未验证的猜测写成事实。

## 6. Implementation Decision

问题和方案确认后再填写。

记录：

- 最终选择的实现；
- 为什么这样做；
- 必要时记录未选择的方案和原因；
- API / Database / Contract / Architecture 是否受影响。

如果只是局部简单修改，可简洁记录。

## 7. Acceptance Criteria

- [ ] 原问题无法按原步骤复现。
- [ ] Expected Behavior 已实现。
- [ ] 现有相关流程没有回归。
- [ ] 相关 targeted test 通过。
- [ ] 必要的 integration / frontend / browser test 通过。
- [ ] `git diff --check` 通过。
- [ ] 必要文档已同步。

任务特定验收：

- [ ] TODO

## 8. Adjustment Log

第一版实施后，如果实际验证仍需要调整，在这里继续追加，不新建 FIX TASK。

### Adjustment 1

状态：Pending / Completed

Observed:

```text
TODO
```

Expected adjustment:

```text
TODO
```

Implementation / Result:

```text
TODO
```

如果没有 Adjustment，可以在最终验收时写：

```text
No follow-up adjustment required.
```

## 9. Test / Verification Record

必须记录实际执行的命令和真实结果。

示例：

```text
backend targeted:
...

frontend:
...

browser:
...

full-stack:
...

manual verification:
...
```

不要写未实际执行的测试。

环境导致无法执行时，记录：

- 失败命令；
- 环境原因；
- 是否与当前代码修改有关；
- 剩余需要人工执行的验证。

## 10. Documentation Sync

按实际影响选择：

- [ ] `docs/CURRENT_STATUS.md`
- [ ] `docs/PHASE5_TASK_INDEX.md`
- [ ] `docs/ARCHITECTURE.md`
- [ ] `docs/DATABASE_DESIGN.md`
- [ ] Contract
- [ ] ADR
- [ ] AGENTS
- [ ] README / ROADMAP
- [ ] 无额外事实文档变化

## 11. Completion Checklist

- [ ] Scope review
- [ ] Security / Owner review（如相关）
- [ ] Logging review（后端改动）
- [ ] Database / migration review（如相关）
- [ ] Contract compatibility review（如相关）
- [ ] Tests
- [ ] `git diff --check`
- [ ] Implementation Record
- [ ] Task Index
- [ ] Current Status
- [ ] 当前 TASK 相关文件已 `git add`
- [ ] 未自动 commit / push

## 12. Implementation Record

完成时记录：

- 实际修改内容；
- 关键行为变化；
- 测试结果；
- 未完成 / Deferred 内容；
- 人工验证结论。

## 13. Commit

建议提交信息：

```text
<type>: complete TASK-XXX <short description>
```

Codex 只提供建议提交信息并 `git add`，除非用户明确要求，不执行 `git commit` 或 `git push`。
