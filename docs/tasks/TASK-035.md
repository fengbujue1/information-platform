# TASK-035：Recommendation Profile Backend 与 API

状态：DONE

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

## 8. 实施记录（2026-08-09）

- 实现 `GET/PUT /api/v1/recommendation/profiles/{informationType}`，Phase 4 当前只接受 `JOB`；
- Owner 只从 Session `CurrentUserProvider` 获取，Recommendation API 已纳入认证与 CSRF 安全边界；
- Generic Core 与 JOB Extension 在同一事务创建或完整替换，不增加 Profile Version；
- Prompt Profile 必须同 Owner、使用 `JOB_USER_RELEVANCE`、状态为 ACTIVE 且存在 Active Version；
- 偏好数组执行 trim、去空、去重、稳定排序，并实施每数组 50 项、每项 100 Unicode 字符的限制；
- `preferredRemoteTypes` 规范为大写并限制为 ONSITE/HYBRID/REMOTE；
- `contentHash` 覆盖 Core + JOB Extension 的完整 canonical representation；
- Profile PUT 不调用 AI、不创建 Analysis/Recommendation Run、不发布刷新事件；
- 未修改 V1～V4 Migration，未实施 TASK-036 或后续 Recommendation 任务。

验证覆盖：create/update、Owner 隔离、跨 Owner Prompt、Prompt 可用性、window/topN/数组限制、hash stability、可空薪资完整替换、Session/CSRF、validation WARN 与 Profile update 不产生 Run。

实际测试结果：

- TASK-035 + Identity Security + Phase 4 Mapper 定向回归：17 tests，0 failures，0 errors，0 skipped；
- 完整后端回归 `./mvnw.cmd test`：199 tests，0 failures，0 errors，2 skipped。
