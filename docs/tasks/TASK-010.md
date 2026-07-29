# TASK-010：Phase 1 端到端验收

状态：DONE
所属阶段：Phase 1
优先级：P0

## 验收场景

- [x] 导入脱敏列表文件。
- [x] 导入部分详情文件。
- [x] 验证列表和详情通过 job_id 合并。
- [x] 验证数据库 source_item_id 是 encrypt_job_id。
- [x] 验证 source_recruiter_id 是 encrypt_boss_id。
- [x] 验证没有详情的职位仍可接入。
- [x] 验证详情到达后补充 JD 并创建新版本。
- [x] 验证重复提交不增加版本号。
- [x] 验证 A → B → A 保存三个版本。
- [x] 验证 security_id 和 lid 不进入中央 rawPayload。
- [x] 验证停止后端时 Outbox 不丢数据。
- [x] 验证 Query API 能查询当前版本和快照。

## 完成标准

- 当前记录、扩展记录和快照一致。
- 所有时间按 UTC 存储。
- 真实采集流程未被破坏。
- 文档与代码一致。

## 验收记录

完成时间：2026-07-29

- 项目负责人已在本地完成 Collector → Information Hub → MySQL → Query API 的真实端到端验收，确认当前功能没有遗漏。
- Collector 的列表/详情左连接、字段映射、安全清理、Hub Client、Outbox 和任意工作目录入口均有自动化回归覆盖。
- Information Hub 的认证、请求校验、幂等写入、非破坏性合并、版本快照和职位查询均有自动化回归覆盖。
- 新采集 `scraped_at` 统一输出 UTC `Z` 时间；历史无时区数据继续按显式配置解释。
- BOSS 字段映射和 Job Query API V1 合同已接受，路线图与当前状态同步关闭 Phase 1。

## 测试记录

- `.\mvnw.cmd test`（JDK 21）：通过，46 项测试，0 失败，0 错误，13 项条件数据库测试因当前命令环境未提供测试库变量而跳过；真实数据库链路由本地端到端验收覆盖。
- `PYTHONUTF8=1` 环境下执行 `python -m unittest discover -s tests -v`：通过，144 项测试，0 失败，0 错误。
- `git diff --check`：通过。
