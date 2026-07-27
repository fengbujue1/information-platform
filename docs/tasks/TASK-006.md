# TASK-006：实现 BOSS 字段映射器

状态：TODO  
所属阶段：Phase 1  
优先级：P0

## 前置依赖

- TASK-002 完成。
- TASK-006A 完成。
- TASK-006B 完成。

## 目标

把 BOSS 列表 JSON 和详情 JSON 合并成 InformationEnvelope V1。

## 输入

- 列表根对象。
- jobs 数组。
- 详情数组。

## 合并规则

- job_id 只用于本地 Join。
- encrypt_job_id 映射 sourceItemId。
- 列表字段是标准化结构主要来源。
- 详情主要补充 JD。
- 重复字段冲突时记录警告，不静默覆盖。
- 缺少详情时仍提交列表职位。

## 字段规则

- boss_name → companyName。
- encrypt_boss_id → sourceRecruiterId。
- boss_title → recruiterTitle。
- boss_online_observed_at → recruiterActiveText。
- boss_online=false、字段缺失或观测时间缺失时 recruiterActiveText=null。
- job_labels 不单独标准化。
- tags → sourceTags、experienceText、educationText。
- skills → sourceSkillTags。
- details.skill_tags 只保留 rawPayload。
- security_id 和 lid 不发送 Hub。
- 空字符串转换为 null。

## 时间规则

- 未来输出必须带时区。
- 当前旧 scraped_at 通过配置时区解释。
- recruiterActiveText 必须使用 Collector 输出的带时区在线观测时间，不得使用 Mapper 运行时间伪造。
- 详情没有时间时 detailCollectedAt=null。

## 测试

- 90/25 形式的部分详情。
- 地点 `成都··`。
- 空 company ID。
- 空 skills 和 welfare。
- 重复福利。
- 薪资带 13～16 薪。
- 详情字段冲突。
- security_id 清理。
- 招聘者在线观测时间映射。
- boss_online=false 或缺失时 recruiterActiveText=null。
- boss_online 和 boss_online_observed_at 保留在安全 rawPayload，recruiterActiveText 不参与 contentHash。
