# 当前开发状态

更新时间：2026-07-23  
当前分支：dev

## 当前阶段

Phase 1：BOSS 采集接入与归档。

## 已完成

- 创建 Monorepo 和模块级 AGENTS。
- 导入并验证 BOSS Collector。
- 建立数据库、协议、TASK 和 ADR 草案。
- 获取并分析真实 BOSS 列表与详情 JSON。
- 确认 90 条列表和 25 条详情的数据结构。
- 确认详情使用 job_id 与列表关联。
- 确认平台 sourceItemId 应使用 encrypt_job_id。
- 确认 boss_name 是公司名，encrypt_boss_id 是招聘者来源 ID。
- 确认 job_labels 与 tags 在当前样本完全重复。
- 确认 details.skill_tags 当前不适合作为标准技能。

## 当前任务

TASK-001 处于 REVIEW，等待用户确认实际字段映射和数据库设计 1.3。

## 待确认决策

1. job_information 增加 source_recruiter_id。
2. 增加 salary_source。
3. 增加 detail_status 和 detail_collected_at。
4. job_labels 改为 source_tags。
5. skills 改为 source_skill_tags。
6. 快照增加 version_no。
7. security_id 和 lid 不进入中央 rawPayload。
8. 旧无时区时间按 Asia/Shanghai 解释。

## 下一步

用户确认后：

1. 将 TASK-001 标记 DONE。
2. 执行 TASK-002，冻结 InformationEnvelope V1。
3. 开始 TASK-003 和 TASK-004。
