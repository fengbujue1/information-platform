# TASK-011：冻结 Phase 2 Web MVP 范围

状态：TODO  
所属阶段：Phase 2  
优先级：P0  
负责人：User + Codex

## 1. 目标

确认并冻结 Phase 2 的产品边界、页面范围、访问边界和 Web UI 行为，使后续前端任务可以按稳定合同实施。

## 2. 背景

Phase 1 已完成，但 Phase 2 目前只有“职位列表、详情、原始数据和历史版本查看”的概括。

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
- `frontend/information-hub-web/AGENTS.md`

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
- Phase 2 不提前进入 AI、推荐或通知。

## 8. 验收标准

- [ ] 用户确认 `docs/PHASE2_SCOPE.md`
- [ ] `PHASE2_SCOPE.md` 状态改为 Accepted
- [ ] Web UI Behavior V1 状态改为 Accepted
- [ ] ADR-010 状态改为 Accepted
- [ ] ROADMAP 的 Phase 2 任务顺序正确
- [ ] CURRENT_STATUS 当前任务改为 TASK-012
- [ ] 根 README 不再显示 Phase 1 仍在规划
- [ ] 相关文档没有范围冲突
- [ ] 未创建前端代码

## 9. 实施前计划

由 Codex 在修改前填写：

- 发现的文档冲突
- 计划修改文件
- 状态变更
- 验证方法
- 需要用户确认的决定

## 10. 实施记录

待填写。

## 11. 测试结果

本任务以文档校验为主：

```text
git diff --check
Markdown 格式检查
链接路径检查
```

实际结果待填写。

## 12. 遗留问题

待填写。

## 13. 完成确认

- [ ] CURRENT_STATUS 已更新
- [ ] 用户已检查 git diff
- [ ] 用户已确认范围
- [ ] 已提交并 push
