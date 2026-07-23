# TASK-010：Phase 1 端到端验收

状态：TODO  
所属阶段：Phase 1  
优先级：P0

## 前置依赖

TASK-001 至 TASK-009 完成。

## 目标

验证从 BOSS 采集到 Job Query API 的完整闭环。

## 验收场景

1. 启动 MySQL 和 Information Hub。
2. 使用脱敏样例提交。
3. 运行一次真实 Collector。
4. 验证 MySQL 幂等记录。
5. 重复采集验证不重复插入。
6. 停止后端，验证 Outbox。
7. 恢复后端，验证补传。
8. 调用列表和详情查询 API。
9. 运行全部测试。
10. 更新 README、CURRENT_STATUS 和 ROADMAP。

## 完成标准

- [ ] 主链路可重复运行
- [ ] 失败场景不丢数据
- [ ] 文档与代码一致
- [ ] Phase 1 可标记完成
