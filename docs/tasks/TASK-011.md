# TASK-011：冻结 Phase 2 Web MVP 范围

状态：DONE
所属阶段：Phase 2
优先级：P0
负责人：User + Codex

## 1. 目标

确认并冻结 Phase 2 的产品边界、页面范围、访问边界和 Web UI 行为，使后续前端任务可以按稳定合同实施。

## 2. 背景

Phase 1 已完成，但 Phase 2 原先只有“职位列表、详情、原始数据和历史版本查看”的概括。

当前 Job Query API V1 不返回 rawPayload，也没有读取端用户认证。必须先消除范围冲突，避免创建前端后再临时增加权限和管理接口。

## 3. 前置依赖

- TASK-010 已完成。
- Job Query API V1 状态为 Accepted。
- Phase 1 端到端验收已完成。

## 4. 影响范围

- `docs/PHASE2_SCOPE.md`
- `docs/contracts/web-ui-behavior-v1.md`
- `docs/decisions/ADR-010-phase2-web-mvp-boundary.md`
- `docs/ROADMAP.md`
- `docs/CURRENT_STATUS.md`
- `docs/README.md`
- 根目录 `README.md`
- `frontend/information-hub-web/AGENTS.md`（复核，无需修改）

## 5. 本任务范围

- 审查 Phase 2 范围。
- 确认标准化职位浏览 MVP。
- 确认 rawPayload 延后。
- 确认当前不增加用户系统。
- 确认当前不公开无认证读取 API。
- 确认页面路由。
- 确认列表 URL 状态规则。
- 确认快照只做查看，不做复杂 Diff。
- 接受 ADR-010。
- 接受 Web UI Behavior V1。
- 更新路线图和当前状态。

## 6. 不在本任务范围

- 不创建 `package.json`。
- 不安装 Node 依赖。
- 不创建 Vue 页面。
- 不修改 Java 和 Python。
- 不修改数据库。
- 不增加认证。
- 不增加 rawPayload API。
- 不提交 Git。

## 7. 业务与技术规则

- Phase 2 复用 Job Query API V1。
- 前端只浏览标准化数据。
- 开发使用 Vite Proxy。
- 部署优先使用 Nginx 同源代理。
- 未增加读取认证前只允许受控访问。
- 路由固定为 `/jobs`、`/jobs/:id` 和 `/jobs/:id/snapshots`。
- 列表状态写入 URL Query。
- 详情和快照使用经过校验的内部 `from` 参数恢复列表；Router State 不能作为唯一恢复来源。
- 重置筛选时保留 `size`，其他筛选、排序和页码恢复默认值。
- Phase 2 不提前进入 AI、推荐或通知。

## 8. 验收标准

- [x] 用户确认 `docs/PHASE2_SCOPE.md`
- [x] `PHASE2_SCOPE.md` 状态改为 Accepted
- [x] Web UI Behavior V1 状态改为 Accepted
- [x] ADR-010 状态改为 Accepted
- [x] ROADMAP 的 Phase 2 任务顺序正确
- [x] CURRENT_STATUS 当前任务改为 TASK-012
- [x] 根 README 显示 Phase 2 范围已冻结，下一任务为 TASK-012
- [x] 相关文档没有范围冲突
- [x] 未创建前端代码

## 9. 实施前计划

### 发现的文档冲突

- 本任务“不提交 Git”与原完成确认“已提交并 push”冲突；以不提交 Git、等待用户检查为准。
- Web UI Behavior V1 同时允许 `from` 和 Router State，不能保证刷新后的返回行为；冻结为受校验的内部 `from` 参数，Router State 仅可辅助。
- 重置行为原为“推荐保留 size”；冻结为必须保留当前 `size`。
- 根 README 的验收描述落后于实际 Phase 1 状态；改为展示 Phase 2 已冻结及下一任务。

### 计划修改文件

- `docs/PHASE2_SCOPE.md`
- `docs/contracts/web-ui-behavior-v1.md`
- `docs/decisions/ADR-010-phase2-web-mvp-boundary.md`
- `docs/tasks/TASK-011.md`
- `docs/ROADMAP.md`
- `docs/CURRENT_STATUS.md`
- `docs/README.md`
- `README.md`

### 状态变更

- Phase 2 Scope：Proposed → Accepted。
- Web UI Behavior V1：Draft → Accepted。
- ADR-010：Proposed → Accepted。
- TASK-011：TODO → DONE。
- ROADMAP Phase 2：规划中 → 进行中，范围已冻结。
- CURRENT_STATUS 当前任务：TASK-011 → TASK-012。

### 验证方法

- `git diff --check`
- Markdown 相对链接存在性检查
- Phase 2 状态和任务顺序检查
- Web URL 参数与 Job Query API V1 对照
- 前端目录无 Vue/Node 代码检查
- Git 未追踪文件和秘密配置检查

### 用户确认的决定

- 只浏览标准化字段，rawPayload 延后。
- 当前不建设用户系统，不公开无认证读取 API。
- 开发使用 Vite Proxy，部署优先使用 Nginx 同源代理。
- 固定三个职位页面路由，列表状态写入 URL Query。
- 使用受校验的内部 `from` 参数恢复列表，重置时保留 `size`。
- 快照只做版本查看，不做复杂 Diff。
- TASK-011 不创建 Vue 代码。

## 10. 实施记录

完成时间：2026-07-29

- 接受 Phase 2 Scope、Web UI Behavior V1 和 ADR-010。
- 冻结标准化职位只读浏览 MVP、访问安全边界、页面路由和 URL 状态规则。
- 明确 rawPayload、用户系统、写操作、AI、推荐、通知和公网无认证部署不进入 Phase 2。
- 将 Phase 2 状态更新为进行中，并把当前任务切换为 TASK-012。
- 复核 `frontend/information-hub-web/AGENTS.md` 与冻结范围一致，未创建或修改 Vue 代码。

## 11. 测试结果

- `git diff --check`：通过。
- Markdown 相对链接检查：8 个本任务文档全部通过，无断链。
- Web UI 与 Job Query API V1 参数检查：12 个列表 Query 参数完全一致。
- Phase 2 任务顺序检查：TASK-011 至 TASK-019 文件齐全且路线图顺序正确。
- 前端目录检查：仍只有 `frontend/information-hub-web/AGENTS.md`，未创建 Vue/Node 文件。
- Git 检查：无新增未追踪文件，真实配置和秘密文件未纳入版本控制。

## 12. 遗留问题

- TASK-012 开始前检查开发环境，并冻结受维护的 Node LTS 版本。
- 读取认证、公网部署和 rawPayload 管理需要独立范围变更与安全任务。
- 端到端测试工具在 TASK-019 中按最小需要确定。

## 13. 完成确认

- [x] CURRENT_STATUS 已更新
- [x] 用户已确认范围和实施计划
- [x] 未创建前端代码
- [x] 未提交 Git，等待用户检查
