# Codex Phase 5 Interaction Guide

用途：Phase 5 日常使用时，按不同场景选择对应提示词。

本文件面向“实际怎么和 Codex 说”，不是设计文档。

---

## 1
### 1.Phase 5 第一次启动

适用：

- 已把 Phase 5 规划包复制到仓库；
- 还没有具体 TASK；
- 只想让 Codex 对齐文档，不改业务代码。

复制给 Codex：

```text
现在项目正式进入 Phase 5 — Product Refinement & Stabilization。

请先阅读：
1. 根 AGENTS.md
2. docs/PROJECT_CONTEXT.md
3. docs/PHASE5_SCOPE.md
4. docs/PHASE5_TASK_MODEL.md
5. docs/PHASE5_TASK_INDEX.md
6. docs/CODEX_PHASE5_WORKFLOW.md
7. docs/CURRENT_STATUS.md
8. docs/PHASE5_EXISTING_DOC_CHANGES.md

然后检查当前 dev 分支真实代码和 Git 状态。

本次只完成 Phase 5 启动文档对齐：
- 按 PHASE5_EXISTING_DOC_CHANGES.md 更新 README、docs/README、ROADMAP、CURRENT_STATUS 和必要的 AGENTS；
- Phase 5 标记为进行中；
- Phase 4 保持已完成；
- 下一个 TASK 编号为 TASK-045；
- 当前没有 Active TASK；
- 不创建空的 TASK-045；
- 不修改业务代码。

完成后执行 git diff --check，
只 git add 本次 Phase 5 启动文档，
不要 commit / push，
最后汇报修改文件和建议提交信息。
```

### 2.换电脑后，继续Phase5
```text
这是一个新的 Codex 对话。

我要继续开发 `information-platform` 的 Phase 5。

不要依赖之前任何 Codex 对话的上下文或记忆，以当前 Git 仓库、Phase 5 文档和代码状态作为唯一事实基线。

请先执行并检查：

bash
git status
git branch --show-current
git log --oneline -10
git diff
git diff --cached


确认当前分支和本地工作区状态。

然后依次阅读：

1. 根 `AGENTS.md`
2. 当前涉及模块的 `AGENTS.md`
3. `docs/PROJECT_CONTEXT.md`
4. `docs/PHASE5_SCOPE.md`
5. `docs/PHASE5_TASK_MODEL.md`
6. `docs/PHASE5_TASK_INDEX.md`
7. `docs/CODEX_PHASE5_WORKFLOW.md`
8. `docs/CURRENT_STATUS.md`
9. 当前 Active TASK 对应的 `docs/tasks/TASK-XXX.md`
10. 当前 TASK 相关的 ADR / Contract / Architecture / Database Design 文档

如果是后端任务，还需要阅读：

* `docs/LOGGING_CONVENTIONS.md`

如果当前 TASK 涉及数据库：

* 阅读 `docs/DATABASE_DESIGN.md`
* 检查当前最新 Flyway Migration

请以：

* 当前仓库代码；
* Git commit；
* staged / unstaged diff；
* `PHASE5_TASK_INDEX.md`；
* 当前 TASK 的 Problem / Expected Behavior / Adjustment Log / Implementation Record；

作为继续开发的事实依据。

先不要修改代码。

先告诉我：

1. 当前 Phase 5 进行到哪个 TASK；
2. 当前 TASK 的状态；
3. 这个 TASK 已经完成了什么；
4. 还有什么未完成；
5. 当前工作区是否存在未提交修改；
6. 下一步最合理的操作是什么。

不要重新实施已经完成并有代码或 TASK 记录证明的工作。

```
---

## 2. 发现一个问题，但先不要改

适用：

- 你只看到一个现象；
- 还不知道是不是 bug；
- 想先和 Codex 分析。

```text
现在处于 Phase 5。

我发现一个实际使用问题：

【在这里描述现象、页面、操作步骤和你看到的结果】

先不要修改代码，也先不要创建 TASK。

请阅读 Phase 5 相关文档和对应模块代码后：
1. 说明当前代码实际是怎么工作的；
2. 尽量定位 root cause；
3. 说明影响范围；
4. 判断这是 bug、体验问题、设计行为还是环境问题；
5. 给出最小修改方案；
6. 告诉我是否需要数据库、API Contract、架构或安全边界变化；
7. 如果有多个方案，说明差异。

这一轮只分析，等待我继续确认 Expected Behavior。
```

---

## 3. 问题已经讨论清楚，创建新 TASK，但先不实施

```text
上面这个问题的 Expected Behavior 已经确认。

请按照 docs/PHASE5_TASK_MODEL.md：
1. 检查它是否属于现有 Active TASK；
2. 如果是独立问题，使用 PHASE5_TASK_INDEX.md 的 Next Task Number 创建新 TASK；
3. 使用 docs/PHASE5_TASK_TEMPLATE.md；
4. 写清 Problem、Expected Behavior、Scope、Investigation Notes、Acceptance Criteria；
5. 更新 PHASE5_TASK_INDEX.md；
6. 状态设为 READY。

这一步只创建/更新规划文档，不修改业务代码。
完成后 git diff --check，并 git add 本次文档，不要 commit / push。
```

---

## 4. 问题很简单，已经明确，直接建 TASK 并实施

适用：

- 行为非常清楚；
- 不需要再进行一轮确认；
- 想省 Codex 回合。

```text
现在处于 Phase 5。

发现问题：
【问题】

期望行为：
【期望】

请直接按 Phase 5 Rolling Task Model 处理：
1. 判断是否属于现有 TASK；
2. 如果是新问题，使用 Next Task Number 创建 TASK；
3. 写清 Scope 和验收标准；
4. 检查 git status / 最近提交；
5. 实施最小完整修改；
6. 运行 targeted test 和必要回归；
7. 更新 TASK Implementation Record、Task Index、Current Status；
8. 状态进入 VERIFYING；
9. git diff --check；
10. git add 当前 TASK 相关文件。

不要 commit / push。
不要顺手实现其它独立问题。
如果实施中发现必须发生数据库、不兼容 Contract 或重大架构变化，先停止扩大范围并告诉我。
```

---

## 5. 开始实施一个已经存在的 READY TASK

```text
请实施 docs/tasks/TASK-0XX.md。

开始前重新阅读：
- 根 AGENTS.md
- 对应模块 AGENTS.md
- PHASE5_SCOPE.md
- PHASE5_TASK_MODEL.md
- CODEX_PHASE5_WORKFLOW.md
- 当前 TASK
- 相关 ADR / Contract
- CURRENT_STATUS.md

先检查 git status 和当前 diff。

严格只做 TASK-0XX Scope。
完成后：
- 运行相关测试；
- git diff --check；
- 更新 Implementation Record；
- 更新 PHASE5_TASK_INDEX.md 和 CURRENT_STATUS.md；
- 状态改为 VERIFYING；
- git add 当前 TASK 文件；
- 不 commit / push；
- 给出我应该人工验证的具体步骤。
```

---

## 6. Codex 第一版改完，但你实际使用后仍不满意

这是 Phase 5 最常见的情况。

不要新建 FIX TASK。

```text
继续 TASK-0XX，不要创建新 TASK。

我实际验证后发现还有下面的问题：

【描述新的观察结果】

我希望调整为：

【描述现在确认的期望行为】

请：
1. 先判断这是否仍属于 TASK-0XX 的原始目标；
2. 如果属于，在 TASK-0XX 的 Adjustment Log 新增一条记录；
3. 状态从 VERIFYING 改回 IN_PROGRESS；
4. 只修改这个 Adjustment 需要的代码；
5. 重新运行受影响测试；
6. 更新验证记录；
7. 完成后重新进入 VERIFYING；
8. git add 当前 TASK 相关文件；
9. 不 commit / push。

不要重新规划整个 Phase 5。
```

---
## 17. 用户已经人工验收通过，关闭 TASK

```text
TASK-0XX 我已经人工验证通过，可以收尾。

请：
1. 再检查当前 TASK 的 Acceptance Criteria；
2. 更新最终 Test / Verification Record；
3. 更新 Implementation Record；
4. 如有 Adjustment，确认都已完成或明确拆出；
5. TASK 状态改为 DONE；
6. 更新 PHASE5_TASK_INDEX.md；
7. 更新 CURRENT_STATUS.md；
8. 必要时同步 Architecture / Database / Contract / ADR；
9. git diff --check；
10. git add 当前 TASK 全部相关文件；
11. 不 commit / push；
12. 给我一条最终提交用语。
```

---

## 7. 调整时发现其实是另一个独立问题

```text
继续处理 TASK-0XX。

刚才又发现一个问题：

【新问题】

请先判断：
- 它是 TASK-0XX 的遗漏/Adjustment；
- 还是可以独立描述、独立验收的新问题。

如果是 Adjustment：
继续 TASK-0XX。

如果是独立问题：
- 不在 TASK-0XX 中顺手实现；
- 使用 Next Task Number 创建一个 DISCOVERED TASK；
- 更新 PHASE5_TASK_INDEX.md；
- 当前仍优先完成 TASK-0XX。

如果新问题属于 P0，再告诉我是否需要中断当前 TASK。
```

---

## 8. Codex 实施时提出“顺便重构一大片”

```text
先不要扩大当前 TASK。

请把你认为需要重构的部分分成：
1. 解决当前 TASK 必须做的最小重构；
2. 可以独立延后的重构；
3. 纯粹为了未来可能需求的提前设计。

当前只允许第 1 类进入 TASK-0XX。

第 2 类如果确实有价值，可以记录为新的 DISCOVERED TASK。
第 3 类不要实施。

请重新给出最小修改边界。
```

---

## 9. 页面 UI / UX 问题

```text
这是 Phase 5 的前端 UI/UX TASK。

问题：
【页面 + 操作 + 现象】

期望：
【期望效果】

请优先检查现有 Vue 组件、composable、API client 和测试。

约束：
- 不因为纯 UI 问题修改后端 Contract；
- 不为了拆文件而拆文件；
- 如果当前 View 已明显承担过多职责，可以做与本 TASK 直接相关的局部拆分；
- loading / empty / error / success 状态一起检查；
- 用户可见行为增加/更新组件测试；
- 重要流程按需要更新 Browser E2E。

完成后告诉我具体人工验证步骤。
```

---

## 10. 后端行为优化

```text
这是 Phase 5 后端行为优化 TASK。

当前行为：
【描述】

期望行为：
【描述】

实施时必须检查：
- authentication；
- owner / authorization；
- validation；
- transaction；
- idempotency；
- error contract；
- logging；
- 相关集成测试。

遵守 LOGGING_CONVENTIONS.md。

不要为了这个问题引入与当前规模不匹配的新基础设施。
```

---

## 11. 安全问题 / Production Hardening

```text
这是 Phase 5 的安全/生产加固问题。

问题：
【描述】

请先做 threat / failure boundary 分析，再实施。

要求：
- 优先 fail-closed；
- 不信任客户端 owner/userId；
- 不在日志输出秘密；
- 不删除现有 Session/CSRF/Collector Bearer 边界；
- 为安全行为增加自动化测试；
- 如果会改变公共 API 的可访问性，明确列出影响；
- 如果是跨模块长期规则，判断是否需要 ADR / AGENTS 更新。

不要只通过前端限制来实现安全规则。
```

---

## 12. 需要改数据库

不要直接说“给我加张表”后让 Codex自由操作。

```text
当前 TASK 确认需要数据库变化。

请先：
1. 查看真实最新 Flyway migration；
2. 阅读 DATABASE_DESIGN.md；
3. 检查受影响实体、Mapper、integration test；
4. 说明兼容性和已有数据迁移方式。

约束：
- 禁止修改旧 Migration；
- 使用新的 Flyway version；
- migration 必须可从当前数据库版本向前升级；
- 更新 DATABASE_DESIGN.md；
- 增加 migration / integration test；
- TASK Implementation Record 记录最终数据库事实。

如果数据库变化超出当前 TASK 的最小需要，先报告，不要扩表。
```

---

## 13. 需要改 API / Contract

```text
当前 TASK 会改变前后端 API 行为。

请先定位现有 Contract，并判断：
- backward compatible；
- 还是 incompatible。

如果只是兼容增加：
按现有版本策略更新 Contract 和前后端测试。

如果不兼容：
先不要直接改代码，
先说明迁移方案、影响页面、调用方和是否需要新的 Contract version / ADR。

确认后再实施。
```

---

## 14. 需要重要架构调整

```text
你判断当前问题需要改变重要架构边界。

先不要直接大改。

请说明：
1. 当前架构为什么无法安全解决；
2. 最小架构变化是什么；
3. 哪些模块和数据会受影响；
4. 是否与现有 Accepted ADR 冲突；
5. 是否应该继续作为 Phase 5 TASK，还是升级成独立后续 Phase。

如果仍属于 Phase 5：
先更新必要 ADR / ARCHITECTURE 决策，再实施代码。
```

---

## 15. 测试报错，不知道是不是这次修改引起

```text
当前 TASK 测试出现失败：

【粘贴失败信息】

不要先为了通过测试修改断言。

请判断属于：
1. 当前 TASK 引入的 regression；
2. 既有代码失败；
3. 本地环境 / MySQL / SSH / Browser / Provider 问题；
4. flaky test。

请给出证据。

如果是当前 TASK 引入，修复后重新执行 targeted test。
如果是环境问题，记录无法验证的部分，不要反复无意义重跑。
如果是独立既有问题，按 Phase 5 规则判断是否创建新 TASK。
```

---

## 16. Codex 说“全部完成”，但你想先让它自查

```text
先不要把 TASK-0XX 标为 DONE。

请对当前 staged/unstaged diff 做一次 TASK-0XX completion review：

检查：
- 是否完全满足 Expected Behavior；
- 是否超 Scope；
- 是否存在遗漏的异常分支；
- authentication / owner；
- transaction / idempotency；
- logging secret boundary；
- frontend loading/error 状态；
- 测试是否真正覆盖本次行为；
- 文档是否与真实代码一致；
- git diff --check。

只修复与 TASK-0XX 直接相关的问题。
完成后保持 VERIFYING，并给出人工验收步骤。
```

---



## 18. 让 Codex 提交代码

只有你完成验收后再说：

```text
TASK-0XX 已经完成并验收。

请检查当前 staged diff 只包含 TASK-0XX 相关修改。
确认 git diff --cached --check 通过后，
使用下面提交信息执行 git commit：

【提交信息】

不要 push。
提交完成后给我 commit hash 和 git status。
```

如果你自己提交，就不用发这一条。

---

## 19. Codex 上下文已经很长，准备新开一个对话

```text
这是一个新的 Codex 对话，继续 information-platform Phase 5 的 TASK-0XX。

不要依赖旧对话记忆。

请重新读取：
- 根 AGENTS.md
- 对应模块 AGENTS.md
- PROJECT_CONTEXT.md
- PHASE5_SCOPE.md
- PHASE5_TASK_MODEL.md
- PHASE5_TASK_INDEX.md
- CODEX_PHASE5_WORKFLOW.md
- docs/tasks/TASK-0XX.md
- CURRENT_STATUS.md
- 当前相关 ADR / Contract

然后执行：
git status
git log --oneline -8
git diff
git diff --cached

以当前仓库代码、TASK Implementation/Adjustment Log 和 Git diff 为事实基线。

先告诉我 TASK-0XX 当前完成到了哪里、还剩什么，
不要重复已经完成并有记录的工作。
```

---

## 20. 切换到下一个 TASK

```text
TASK-0XX 已经 DONE。

现在处理 Phase 5 下一个问题：
【描述】

请先根据 PHASE5_TASK_INDEX.md 确认 Next Task Number，
按 Rolling Task Model 判断和创建新 TASK。

不要把上一个 TASK 的临时实现假设自动带到新 TASK，
但要遵守已经形成的正式 ADR / Contract / Architecture 事实。
```

---

## 21. 一个问题越聊越大，怀疑不应该放 Phase 5

```text
先停止实现。

当前需求已经从最初问题扩展为：
【描述】

请根据 PHASE5_SCOPE.md 判断：
1. 仍然是局部 refinement；
2. 应拆成多个 Phase 5 TASK；
3. 已经属于新的大型产品能力 / 新 Information Type / 大架构阶段。

请优先保持 Phase 5 边界，不要为了“已经聊到这里”就把大功能塞进当前 TASK。
```

---

## 22. 临时发现 P0

```text
发现一个疑似 P0 问题：

【描述】

请立即停止当前非 P0 TASK 的扩展实施。

先分析是否存在：
- 数据损坏；
- 越权 / 敏感数据泄露；
- 核心服务不可用；
- 不可逆迁移风险。

如果确认 P0：
- 创建新的 Phase 5 P0 TASK；
- 在 Task Index 标为 Active；
- 原 TASK 记录为暂停但不标 DONE；
- 只实施 P0 最小安全修复；
- 增加回归测试；
- 完成后再恢复原 TASK。
```

---

## 23. 只是一个很小的文案/样式问题

如果仍然属于正在处理的 TASK：

```text
这个修改仍属于 TASK-0XX 的页面体验范围，
请作为 Adjustment 处理，不创建新 TASK。

修改：
【内容】

只改必要文件，运行最小相关测试并更新 Adjustment Log。
```

如果是完全独立的新问题，仍按正常新 TASK 规则，不因为“小”就混入其它 TASK。

---

## 24. Phase 5 准备结束

```text
我准备结束 Phase 5。

先不要新增功能。

请根据仓库真实状态做 Phase 5 completion review：

1. 阅读 PHASE5_TASK_INDEX.md；
2. 检查所有 Phase 5 TASK 的真实状态；
3. Active / VERIFYING 不允许被静默写成 DONE；
4. Deferred 必须保留原因；
5. 汇总实际完成的 Frontend / Backend / Integration / Infrastructure TASK；
6. 运行约定的最终回归测试；
7. 使用 PHASE5_COMPLETION_TEMPLATE.md 创建 PHASE5_COMPLETION.md；
8. 同步 README、docs/README、ROADMAP、CURRENT_STATUS；
9. 检查 ARCHITECTURE / DATABASE_DESIGN / Contract / ADR 与代码事实；
10. git diff --check；
11. git add Phase 5 收尾文件；
12. 不 commit / push；
13. 给出 Phase 5 收尾提交信息。

不要提前宣布下一 Phase 的未实现能力已经完成。
```

---

# 使用建议

日常最常用的其实只有四种：

```text
发现问题
→ 场景 2：先分析

问题确认
→ 场景 3 / 4：创建 TASK 或直接实施

第一版不满意
→ 场景 6：原 TASK Adjustment

验收成功
→ 场景 17：TASK DONE
```

其余场景只在数据库、Contract、架构、测试异常或上下文切换时使用。

Phase 5 的核心不是“每一次聊天都创建一个 TASK”，而是：

> 一个独立用户问题 = 一个可持续多轮调整、最终可验收的 TASK。
