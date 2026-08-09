# TASK-043：Phase 4 Web — Profile、Feed、Refresh、Feedback 与 BOSS Contact Status

状态：TODO

所属阶段：Phase 4

优先级：P0

设计状态：Accepted

## 1. 目标

完成 Phase 4 最小可用 Web，让用户配置画像、查看推荐、刷新、反馈并维护“已联系/已联系且不合适”。

## 2. 前置依赖

TASK-035、036、040、042。

## 3. 实施范围

- Recommendation page；
- Profile form；
- Feed card/list；
- score/reasons；
- open source/BOSS link；
- Manual Refresh；
- Run polling；
- Interested；
- Not Interested；
- Contacted；
- Contacted Not Suitable；
- Viewed；
- CONTACTED badge；
- hard exclusion card immediate removal；
- undo/reset action；
- profile stale hint；
- error states。

## 4. 明确不做

- 自动读取 BOSS 聊天；
- 全站 redesign；
- complex animations；
- Notification；
- Product Polish。

## 5. 验收与测试

profile save、refresh/polling、feed、feedback、contact disposition、immediate hide、CONTACTED remains、undo, stale hint, error handling, browser E2E。

## 6. 文档同步

- frontend docs as needed
- CURRENT_STATUS

## 7. 完成前统一检查

- 当前 TASK 测试通过；
- `git diff --check`；
- 未超 Scope；
- 后端改动按 `docs/LOGGING_CONVENTIONS.md` 检查；
- 更新当前 TASK 实施记录；
- 更新 `docs/CURRENT_STATUS.md`；
- 如数据库/架构事实改变，同步事实文档；
- 汇报实际命令与结果。
