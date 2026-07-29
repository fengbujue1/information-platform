# Phase 2 规划包说明

## 新增

- `docs/PHASE2_SCOPE.md`
- `docs/contracts/web-ui-behavior-v1.md`
- `docs/CODEX_PHASE2_WORKFLOW.md`
- `docs/decisions/ADR-010-phase2-web-mvp-boundary.md`
- `docs/tasks/TASK-011.md` 至 `TASK-019.md`

## 更新

- 根目录 `README.md`
- `docs/README.md`
- `docs/ROADMAP.md`
- `docs/CURRENT_STATUS.md`
- `frontend/information-hub-web/AGENTS.md`

## Phase 2 任务顺序

```text
TASK-011 冻结 Phase 2 范围
→ TASK-012 创建 Vue 项目骨架
→ TASK-013 实现 API Client 与类型
→ TASK-014 实现职位列表
→ TASK-015 实现职位详情
→ TASK-016 实现历史快照
→ TASK-017 完善交互与健壮性
→ TASK-018 建立 CI 与受控部署配置
→ TASK-019 Phase 2 端到端验收
```

## 关键范围决策

- Phase 2 只浏览标准化数据。
- rawPayload 查看推迟到具备管理端认证之后。
- 不新增职位写入、编辑、删除功能。
- 不修改 Collector 主链路。
- 不启动 AI、推荐、用户画像和消息通知。
- 不直接公开无认证的读取 API。
