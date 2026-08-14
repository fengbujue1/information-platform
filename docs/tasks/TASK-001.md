# TASK-001：分析 BOSS 采集器输出结构

状态：DONE
所属阶段：Phase 1
优先级：P0

## 目标

依据实际列表和详情 JSON，形成可靠字段映射和数据库调整建议。

## 已分析样本

- 列表：90 条。
- 详情：25 条。
- 详情均可通过 job_id 关联列表。
- 详情主要新增 JD。

## 已确认

- encrypt_job_id 是平台来源职位 ID。
- job_id 是 Collector 内部 Join Key。
- boss_name 映射 companyName。
- encrypt_boss_id 映射 sourceRecruiterId。
- boss_title 映射 recruiterTitle。
- job_labels 与 tags 当前重复。
- details.skill_tags 不作为标准技能。
- 当前详情文件没有抓取时间和状态。
- 当前 scraped_at 没有时区。

## 产出

- `docs/DATABASE_DESIGN.md` 1.3
- `docs/contracts/boss-job-field-mapping.md`
- 更新后的 InformationEnvelope V1 草案
- 脱敏列表、详情和 Envelope 样例

## 验收标准

- [x] 实际字段已审计
- [x] 列表和详情关联方式已确认
- [x] 错误的 boss_name 含义已修正
- [x] 数据库修改建议已形成
- [x] 用户已确认设计
- [x] TASK 状态改为 DONE

## 实施记录

2026-07-23 完成设计确认：

- 审计 90 条列表和 25 条详情样本，确认使用 `job_id` 在 Collector 内部合并。
- 确认 `encrypt_job_id` 作为平台 `sourceItemId`。
- 确认 `boss_name` 映射 `companyName`，`encrypt_boss_id` 映射 `sourceRecruiterId`。
- 完成数据库设计 1.3、BOSS 字段映射、InformationEnvelope V1 草案及脱敏样例。
- 用户确认数据身份、时间、安全、详情状态、标签解析、快照版本和非破坏性更新规则。
