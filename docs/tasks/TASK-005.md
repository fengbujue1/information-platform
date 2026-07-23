# TASK-005：实现信息接入 API

状态：TODO  
所属阶段：Phase 1  
优先级：P0

## 前置依赖

TASK-004 完成。

## 目标

实现单条 InformationEnvelope 接入和幂等归档。

## 本任务范围

- `POST /api/v1/collector/items`
- V1 协议校验
- 简单静态 Collector Token
- 首次插入和幂等更新
- 主表与扩展表事务
- contentHash
- 统一错误响应
- 集成测试

## 不在范围

- 不实现批量接口。
- 不实现 AI。
- 不修改 Python Collector。

## 验收标准

- [ ] 首次提交 201
- [ ] 重复提交 200 且不重复插入
- [ ] 内容变化可识别
- [ ] 事务失败完整回滚
- [ ] Token 和参数错误有测试
