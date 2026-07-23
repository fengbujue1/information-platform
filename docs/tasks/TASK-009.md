# TASK-009：实现职位查询 API

状态：TODO  
所属阶段：Phase 1  
优先级：P1

## 前置依赖

TASK-005 完成。

## 目标

提供职位分页和详情查询 API。

## 本任务范围

- `GET /api/v1/jobs`
- `GET /api/v1/jobs/{id}`
- 可选：`GET /api/v1/jobs/{id}/snapshots`
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

- [ ] 分页边界正确
- [ ] size 超过 100 被拒绝或限制
- [ ] 排序字段使用白名单
- [ ] 筛选测试通过
- [ ] 列表不返回完整 rawPayload
- [ ] 快照查询按时间倒序
