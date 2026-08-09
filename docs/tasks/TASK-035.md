# TASK-035：Recommendation Profile Backend 与 API

状态：TODO

所属阶段：Phase 4

优先级：P0

设计状态：Accepted

## 1. 目标

实现账号级 Recommendation Profile、AI Prompt Profile 绑定、contentHash 和 Owner API。

## 2. 前置依赖

TASK-034；Contract `recommendation-profile-v1.md`。

## 3. 实施范围

- GET/PUT singleton；
- Owner / CSRF；
- same-owner Prompt Profile；
- normalize structured preferences；
- contentHash；
- windowDays/topN；
- Profile update no refresh。

## 4. 明确不做

- Profile version；
- 自动 Prompt extraction；
- Web。

## 5. 验收与测试

create/update、cross-owner、limits、hash stability、Profile update 不产生 Run、validation logs。

## 6. 文档同步

- Contract implementation note
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
