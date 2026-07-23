# TASK-010：Phase 1 端到端验收

状态：TODO  
所属阶段：Phase 1  
优先级：P0

## 验收场景

1. 导入脱敏列表文件。
2. 导入部分详情文件。
3. 验证列表和详情通过 job_id 合并。
4. 验证数据库 source_item_id 是 encrypt_job_id。
5. 验证 source_recruiter_id 是 encrypt_boss_id。
6. 验证没有详情的职位仍可接入。
7. 验证详情到达后补充 JD 并创建新版本。
8. 验证重复提交不增加版本号。
9. 验证 A → B → A 保存三个版本。
10. 验证 security_id 和 lid 不进入中央 rawPayload。
11. 验证停止后端时 Outbox 不丢数据。
12. 验证 Query API 能查询当前版本和快照。

## 完成标准

- 当前记录、扩展记录和快照一致。
- 所有时间按 UTC 存储。
- 真实采集流程未被破坏。
- 文档与代码一致。
