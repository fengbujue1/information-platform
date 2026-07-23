# TASK-002：冻结 InformationEnvelope V1

状态：TODO  
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

- [ ] 协议状态改为 Accepted
- [ ] 与 DATABASE_DESIGN 1.3 一致
- [ ] 与 boss-job-field-mapping 一致
- [ ] 请求和响应样例完整
- [ ] 兼容性规则明确
