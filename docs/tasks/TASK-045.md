# TASK-045：Phase 3 AI 页面中文化与展示文案统一

状态：DONE

完成日期：2026-08-13

所属阶段：Phase 5

类型：Frontend

优先级：P2

创建日期：2026-08-12

## 1. Problem

Phase 3 AI 页面存在大量直接面向用户的英文或中英文混排，包括顶部导航、页面标题、字段名、操作、状态标签、空态、错误提示和说明文字。

典型现象：

- 提示词页面显示 `Prompt Profiles`、`Profile`、`Version`、`Active`；
- 分析批次页面显示 `Analysis Batches`、`Trigger`、`Selected`，并原样展示 `MANUAL`、`COMPLETED` 等协议枚举；
- 定时分析页面显示 `Analysis Schedules`、`Window Days`、`Max Candidates`；
- Token 用量页面显示 `Token Usage`、`Actual Total Token`、`Invocation`；
- 手动分析、分析结果和批次详情存在相同的展示问题。

这些英文主要来自 Vue 模板中的硬编码文案，以及后端稳定枚举值被前端直接渲染。当前行为不影响核心功能，但降低中文用户的理解效率，并造成同一产品界面的语言和术语不一致。

## 2. Expected Behavior

- 整个 Phase 3 AI 页面域的用户可见标题、字段名、操作、状态和说明文字使用准确、统一的中文。
- 覆盖顶部 AI 导航、提示词方案、手动分析、分析结果、分析批次列表与详情、定时分析和 Token 用量统计。
- `Prompt`、`Profile` 等 AI 产品术语翻译为“提示词”“提示词方案”等准确中文。
- 保留 `Token`、`IANA`、`Java`、`MySQL`、`Hash`、`ID`、`AI` 等必要技术术语，以及 `Asia/Shanghai` 等真实 IANA 时区值、用户数据和技术栈内容。
- `ACTIVE`、`DISABLED`、`MANUAL`、`SCHEDULED`、`COMPLETED` 等后端协议枚举在展示层转换为准确中文；业务判断、TypeScript 类型和 API 传输值保持原值。
- loading、empty、error、success 等页面状态中的文案保持中文一致性。

## 3. Scope

### Included

- 统一 `frontend/information-hub-web` 中整个 Phase 3 AI 页面域的用户可见中文文案。
- 更新全局导航中属于 Phase 3 AI 的入口名称。
- 为提示词方案、分析、批次、定时分析和用量相关协议枚举增加集中、类型安全的中文展示映射。
- 对未知枚举值保留安全、可辨识的回退展示，避免空白或渲染异常。
- 更新受影响的组件测试、应用壳测试和 Phase 3 Browser E2E 文案定位与断言。
- 检查 normal、loading、empty、error、success 状态的中文一致性。

### Not Included

- 不修改后端枚举值、API 请求或响应结构。
- 不修改数据库、Flyway Migration、Contract、Architecture 或 ADR。
- 不引入完整 i18n、语言切换或新的国际化基础设施。
- 不翻译用户输入、用户名、职位技术栈、IANA 时区值、Hash 内容或其它业务数据。
- 不扩展到 Phase 2 职位浏览或 Phase 4 职位推荐页面的独立文案重构；仅修改共享顶部导航中的 Phase 3 AI 入口。
- 不顺带修复与中文化无关的功能、状态颜色或布局问题。

## 4. Preconditions / Existing Constraints

- 遵守根 `AGENTS.md`、`frontend/information-hub-web/AGENTS.md` 和现有 Accepted ADR / Contract。
- 保持同源 Session、CSRF、Owner 隔离和认证路由守卫不变。
- Phase 3 Analysis、Snapshot、Prompt Version、Provider Usage 和 Batch / Schedule 的历史业务语义保持不变。
- Actual Token 仍只来自 Provider Usage；Estimated Token 不得补入 Actual Token。
- 前端展示映射不得改变后端稳定枚举值或 API Contract。
- 不使用 `v-html`，不向浏览器或日志引入任何秘密。

## 5. Investigation Notes

- Phase 5 Task Index 当前无 Active TASK；本问题可独立描述和验收，应创建为 `TASK-045`。
- 根因不是浏览器语言、Element Plus 语言包、后端配置或操作系统环境。
- 固定英文文案直接存在于 `App.vue`、`PromptProfilesView.vue`、`ManualAnalysisView.vue`、`AnalysisResultView.vue`、`BatchListView.vue`、`BatchDetailView.vue`、`SchedulesView.vue`、`UsageView.vue` 及相关组件。
- `MANUAL`、`SCHEDULED`、`ACTIVE`、`DISABLED` 和 Analysis / Batch / Item 状态由后端按 Accepted Contract 返回，当前前端部分位置直接输出原值。
- Phase 3 Web 由 `TASK-031` 以功能链路、安全和状态语义为重点完成；原验收标准未冻结完整中文展示规范。
- 现有组件测试和 `e2e/phase3.spec.ts` 部分使用英文文案定位元素，实施时必须同步更新。
- Batch 列表成功状态颜色判断疑似使用了与 Batch Contract 不一致的 `SUCCEEDED`，但该问题可独立描述，本 TASK 不顺带修复。

## 6. Implementation Decision

- 采用纯前端展示层中文化方案，不改变后端协议。
- 固定界面文案在现有组件内替换为统一中文。
- 稳定协议枚举通过集中、类型安全的格式化函数转换为中文；内部逻辑继续使用英文枚举值。
- 不引入完整 i18n；当前只有中文界面需求，引入语言资源和切换机制会不必要地扩大范围。
- 数据库、API Contract、Architecture、ADR 和安全边界均不受影响。

## 7. Acceptance Criteria

- [x] 原问题无法按原步骤复现。
- [x] Expected Behavior 已实现。
- [x] 现有相关流程没有回归。
- [x] 相关 targeted test 通过。
- [x] 必要的 frontend / browser test 通过。
- [x] `git diff --check` 通过。
- [x] 必要文档已同步。
- [x] 顶部 Phase 3 AI 导航全部使用准确中文。
- [x] 提示词方案、手动分析、分析结果、批次列表与详情、定时分析、Token 用量页面的标题、字段、操作和说明文案完成中文化。
- [x] loading、empty、error、success 状态文案完成中文化。
- [x] 后端协议枚举只在展示层映射中文，API 值和业务判断保持不变。
- [x] 必要技术术语和真实业务数据未被错误翻译。
- [x] 未引入完整 i18n，未修改后端、数据库、Contract、Architecture、ADR 或安全边界。
- [x] 受影响的组件测试和 Phase 3 Browser E2E 文案定位已同步。

## 8. Adjustment Log

实施过程中未调整冻结 Scope。首次全量前端测试发现新增批次列表测试的中文断言编码异常，修正测试文件编码后重新执行并通过；业务实现方案未变化。

2026-08-13 人工验收发现职位推荐页面的推荐画像区块仍展示 Prompt Profile、Active Version、Analysis 等英文。该问题属于 TASK-045 中文化验收反馈，未创建新 TASK；已将该区块及相关推荐错误提示统一为中文，并同步组件测试与本地 Fixture Browser E2E 定位。推荐逻辑、协议、API 和数据结构未变化。

上述 Adjustment 均已完成并通过自动化及人工验证，没有遗留项，也不需要拆分后续 TASK。

## 9. Test / Verification Record

已执行：

```text
npm.cmd test -- src/utils/aiDisplay.spec.ts src/views/PromptProfilesView.spec.ts src/views/ManualAnalysisView.spec.ts src/views/BatchDetailView.spec.ts src/views/SchedulesView.spec.ts src/views/UsageView.spec.ts src/tests/appShell.spec.ts
结果：7 个测试文件、10 个测试全部通过。

npm.cmd test -- src/views/BatchListView.spec.ts
结果：1 个测试文件、1 个测试通过。

npm.cmd test
结果：30 个测试文件、90 个测试全部通过。

npm.cmd run typecheck
结果：通过。

npm.cmd run build
结果：通过；vue-tsc 与 Vite 生产构建成功。

npm.cmd test -- src/views/RecommendationsView.spec.ts
结果：1 个测试文件、3 个测试全部通过。

npm.cmd run test:e2e
结果：8 个本地 Fixture Browser E2E 全部通过，包括推荐画像、Feed、刷新、反馈和联系状态流程。
```

未执行 `npm.cmd run test:e2e:phase3`：当前环境未配置该脚本要求的 7 个 `INFORMATION_HUB_E2E_*` 变量，且缺少后端构建 JAR。Browser E2E 的中文文案定位与断言已同步，真实浏览器链路留待人工验证或具备 MySQL、后端 JAR 和 E2E 密钥的环境执行。

2026-08-13 用户已完成人工浏览器验证，确认原问题及推荐画像区块的验收遗漏均已解决，TASK-045 验收通过。

## 10. Documentation Sync

- [x] `docs/CURRENT_STATUS.md`
- [x] `docs/PHASE5_TASK_INDEX.md`
- [ ] `docs/ARCHITECTURE.md`
- [ ] `docs/DATABASE_DESIGN.md`
- [ ] Contract
- [ ] ADR
- [ ] AGENTS
- [ ] README / ROADMAP
- [x] 无额外事实文档变化

## 11. Completion Checklist

- [x] Scope review
- [x] Security / Owner review（前端展示改动，边界未变化）
- [x] Logging review（无后端改动）
- [x] Database / migration review（无数据库改动）
- [x] Contract compatibility review（协议枚举与 API 值未变化）
- [x] Tests
- [x] `git diff --check`
- [x] Implementation Record
- [x] Task Index
- [x] Current Status
- [x] 当前 TASK 相关文件已 `git add`
- [x] 未自动 commit / push

## 12. Implementation Record

- 在全局导航和 Phase 3 AI 页面域内统一用户可见中文文案，覆盖提示词方案、手动分析、分析结果、分析批次列表与详情、定时分析和 Token 用量统计。
- 新增集中、类型安全的展示格式化工具，将提示词方案状态、分析状态、批次触发方式、批次与条目状态、调用状态和跳过原因映射为中文；未知值回退为 `未知（原值）`，空值回退为 `—`。
- 保留 Token、IANA、Java、MySQL、Hash、ID、AI 与真实 IANA 时区值等必要技术内容；协议枚举、API 请求响应、业务判断保持原值。
- 同步 Phase 3 API 错误提示、组件测试、应用壳测试和 Browser E2E 的用户可见文案定位。
- 根据 2026-08-13 人工验收反馈，补齐职位推荐页面中复用 Phase 3 提示词方案与分析术语的中文展示，以及相关推荐错误提示。
- 未修改后端、数据库、Migration、Contract、Architecture、ADR、安全边界或 Phase 4 独立页面文案，未引入完整 i18n。
- 两轮 Adjustment 已全部完成，无遗留或需拆分事项；2026-08-13 经人工浏览器验证通过，状态转为 `DONE`。

## 13. Commit

建议提交信息：

```text
feat: 完成 TASK-045 Phase 3 AI 页面中文化与文案统一
```
