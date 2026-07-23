# TASK-005：实现信息接入 API

状态：TODO  
所属阶段：Phase 1  
优先级：P0

## 前置依赖

TASK-004 完成。

## 目标

实现单条 InformationEnvelope 接入、非破坏性合并、幂等归档和历史快照。

## 本任务范围

- `POST /api/v1/collector/items`
- V1 协议校验
- 静态 Collector Token
- 请求体上限 5 MiB
- 首次插入
- 重复提交非破坏性合并
- 合并后计算 contentHash
- 首次版本和变化版本快照
- 主表、扩展表和快照事务
- 统一成功和错误响应
- 集成测试

## 更新规则

- 缺失、null 和空字符串不覆盖已有有效值。
- 空数组默认不覆盖已有非空数组。
- V1 不支持主动清空字段。
- rawPayload 和采集元数据更新为最近一次接收值。
- Hash 不变时不创建快照。
- Hash 变化时创建快照。

## 错误码

至少实现：

- `INVALID_REQUEST`
- `COLLECTOR_UNAUTHORIZED`
- `PAYLOAD_TOO_LARGE`
- `UNSUPPORTED_SCHEMA_VERSION`
- `UNSUPPORTED_INFORMATION_TYPE`
- `INTERNAL_ERROR`

## 不在范围

- 不实现批量接口。
- 不实现 AI。
- 不修改 Python Collector。

## 验收标准

- [ ] 首次提交返回 201
- [ ] 重复提交返回 200 且不重复插入主记录
- [ ] 临时 content=null 不清空旧 JD
- [ ] 内容未变化不新增快照
- [ ] 内容变化新增快照
- [ ] 事务失败完整回滚
- [ ] Token、字段校验和请求体过大有测试
