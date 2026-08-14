# TASK-009：实现职位查询 API

状态：DONE
所属阶段：Phase 1
优先级：P1

## 前置依赖

TASK-005 完成。

## 目标

提供职位分页和详情查询 API。

## 本任务范围

- `GET /api/v1/jobs`
- `GET /api/v1/jobs/{id}`
- `GET /api/v1/jobs/{id}/snapshots`
- 分页、筛选和排序白名单
- DTO 与 Entity 分离

## 分页规则

- `page` 从 1 开始。
- `size` 默认 20。
- `size` 最大 100。
- 默认排序：`firstSeenTime DESC`。

## 允许排序字段

- `firstSeenTime`
- `lastSeenTime`
- `publishTime`
- `salaryMinMonthlyYuan`

禁止将客户端排序字段直接拼接到 SQL。

## 筛选规则

支持：

- 关键词
- 公司
- 城市
- 薪资范围
- 来源
- jobStatus
- remoteType

关键词至少搜索：

- 职位标题
- 公司名称
- 职位正文

具体是否使用 LIKE 或全文检索由 Phase 1 实现决定，当前不引入 Elasticsearch。

## rawPayload 规则

- 列表接口默认不返回 `rawPayload`。
- 详情接口可以通过显式参数返回，或者提供独立管理接口。
- 不向未授权调用方暴露潜在敏感来源字段。

## 验收标准

- [x] 分页边界正确
- [x] size 超过 100 被拒绝或限制
- [x] 排序字段使用白名单
- [x] 筛选测试通过
- [x] 列表不返回完整 rawPayload
- [x] 快照查询按时间倒序

## 实施记录

完成时间：2026-07-28

- 新增职位分页、当前详情和历史快照三个只读接口，Controller 只负责参数绑定和调用应用服务。
- 分页默认 `page=1`、`size=20`，单页最大 100；分页统计和列表读取处于同一个只读事务。
- 支持关键词、公司、城市、薪资区间、来源、职位状态和办公方式筛选；薪资使用区间相交语义。
- 使用 MyBatis XML 关联 `information_item` 和 `job_information`；排序列通过 `<choose>` 固定白名单选择，不使用 `${}` 或客户端 SQL 片段。
- 四个允许排序字段均追加同方向 `id` 次排序，避免相同字段值造成不稳定分页。
- 列表不查询职位正文；列表、详情和快照查询均不读取或返回 `rawPayload`。
- 详情和快照不存在时返回稳定的 `JOB_NOT_FOUND`；分页、排序、薪资和路径参数错误返回稳定 400 错误码。
- 数据库 `DATETIME` 按既有 UTC 约定转换为带 `Z` 的 API 时间。
- 未增加数据库迁移、查询认证、原始数据管理接口或新的基础设施。

## 验证结果

- `.\mvnw.cmd -DskipTests compile`：通过。
- `.\mvnw.cmd '-Dtest=JobQueryServiceTest,JobQueryControllerTest' test`：15 项全部通过。
- `.\mvnw.cmd -Dtest=JobQueryMapperSqlTest test`：5 项全部通过，覆盖排序白名单、参数绑定及 rawPayload 排除。
- `.\mvnw.cmd test`：46 项，0 失败、0 错误、13 项跳过；其中新增真实 MySQL 查询集成测试 2 项仅在 `INFORMATION_HUB_TEST_DB_*` 配置后运行，当前环境未配置，因此明确跳过。
