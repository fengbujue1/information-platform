# TASK-002：冻结 InformationEnvelope V1

状态：DONE
所属阶段：Phase 1
优先级：P0

## 前置依赖

- TASK-001 用户确认完成。

## 目标

冻结采集器与 Information Hub 的 V1 协议。

## 必须确认

- sourceItemId 使用 encrypt_job_id。
- sourceRecruiterId 使用 encrypt_boss_id。
- sourceTags 和 sourceSkillTags 命名。
- detailStatus 语义。
- collectionContext 结构。
- rawPayload 安全清理规则。
- 非破坏性更新。
- 快照 versionNo 响应。
- UTC 时间规则。

## 不在范围

- 不实现后端代码。
- 不实现批量接口。
- 不设计 AI 分析结果。

## 验收标准

- [x] 协议状态改为 Accepted
- [x] 与 DATABASE_DESIGN 1.3 一致
- [x] 与 boss-job-field-mapping 一致
- [x] 请求和响应样例完整
- [x] 兼容性规则明确

## 确认记录

2026-07-23 冻结 InformationEnvelope V1：

1. `information_snapshot` 外键采用 `ON DELETE RESTRICT`。
2. `security_id` 和 `lid` 在提交 Information Hub 前删除。
3. 历史无时区时间按 `Asia/Shanghai` 解释。
4. 新采集时间必须包含明确时区。
5. `detailStatus` 支持 `UNKNOWN`、`FETCHED`、`FAILED`、`UNAVAILABLE`。
6. `tags` 使用规则识别经验和学历，无法识别时只保留 `sourceTags`。
7. `sourceItemId` 使用 `encrypt_job_id`。
8. `job_id` 只用于 Collector 内部合并列表和详情。
9. `boss_name` 映射 `companyName`。
10. `encrypt_boss_id` 映射 `sourceRecruiterId`。
11. 快照使用递增 `versionNo`，支持 `A → B → A`。
12. 使用非破坏性更新，null 或空值不覆盖已有有效值。
