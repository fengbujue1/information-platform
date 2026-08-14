# TASK-006：实现 BOSS 字段映射器

状态：DONE
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

## 验收标准

- [x] 列表职位与详情按 `job_id` 左连接，90/25 场景仍输出 90 个 Envelope。
- [x] `sourceItemId` 使用 `encrypt_job_id`，`boss_name` 只映射 `companyName`。
- [x] 缺少详情仍输出，详情状态和 JD 遵守非破坏性上报语义。
- [x] tags、skills、welfare、地点和薪资按冻结规则转换。
- [x] 招聘者在线观测时间只在严格 true 且来源时间带时区时映射。
- [x] rawPayload 保留安全业务字段并递归删除禁止的敏感字段。
- [x] 历史无时区采集时间通过显式配置按 `Asia/Shanghai` 解释。
- [x] 冲突、重复详情和孤立详情均记录不含业务值的警告。
- [x] 未修改 BOSS 采集核心，未实现 Hub Client、Outbox 或数据库写入。
- [x] 新增测试通过，完整 Collector 回归未新增失败。

## 实施记录

完成时间：2026-07-27

- 新增独立 `integrations` Python 包，包含 Mapper 配置、BOSS 列表/详情左连接和 InformationEnvelope V1 映射。
- 同一映射批次生成并共享一个 runId；列表根 `scraped_at` 带时区时直接解析，无时区时按可配置的 `Asia/Shanghai` 解释，最终 `collectedAt` 输出 UTC `Z` 时间。
- 列表字段保持标准化权威，详情只补充 JD、详情状态和来源提供的详情采集时间；重复字段不覆盖列表值并记录不含字段内容的警告。
- 实现 tags 经验/学历识别、分隔数组去空去重、地点空段处理以及 K 月薪和 13～16 薪解析；无法识别时保留来源文本并将标准字段设为 null。
- `boss_online_observed_at` 仅在 `boss_online` 严格等于布尔 true 且时间带时区时原样映射到 `recruiterActiveText`。
- rawPayload 深拷贝列表和详情，递归清理 `security_id`、`lid` 及 Information Hub 禁止的凭据字段，不修改 Mapper 输入对象。
- `pyproject.toml` 已将 `integrations` 纳入 wheel 包；未增加第三方依赖、新基础设施、采集核心改动或数据库行为。

验证结果：

- `python -m py_compile integrations\*.py`：通过。
- `python -m unittest tests.test_boss_job_merger tests.test_information_mapper`：14 项全部通过。
- 使用仓库脱敏列表/详情样本执行映射和 TOML 包配置验证：通过。
- 使用 Git 忽略目录中的本机实际输出执行只读映射验证：30 条列表、6 条详情全部成功转换，详情数和在线观测数一致，敏感字段未进入 Envelope。
- Collector 完整回归：105 项中 100 项通过；5 项为 TASK-006 前已存在的 Windows Chrome 路径/端口基线失败，本任务未新增失败。
