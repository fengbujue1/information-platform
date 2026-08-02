# Phase 3 Data Model Draft

状态：Proposed

> 本文件负责 Phase 3 的领域模型、实体职责和关系设计，不是最终 Flyway SQL。
>
> 具体 MySQL 字段类型、主键、唯一键、索引、外键、删除规则、状态和物理表结构参见：
>
> `docs/DATABASE_DESIGN_PHASE3_DRAFT.md`
>
> `PHASE3_DATA_MODEL_DRAFT.md` 与 `DATABASE_DESIGN_PHASE3_DRAFT.md` 必须在 TASK-020 中一起接受真实仓库审查；用户确认后才能进入 Accepted。TASK-024 只能实现已经 Accepted 的数据库设计。

## 1. 建议新增表

```text
user_account
ai_prompt_profile
ai_prompt_version
information_analysis
ai_model_invocation
ai_analysis_batch
ai_analysis_batch_item
ai_analysis_schedule
```

## 2. user_account

职责：

- 登录身份；
- AI 数据 Owner；
- 用户时区。

建议字段：

```text
id
username
password_hash
display_name
timezone
status
created_at
updated_at
```

不保存明文密码。

## 3. ai_prompt_profile

职责：

- 用户长期关注点；
- 绑定 Analysis Definition；
- 指向 Active Version。

建议字段：

```text
id
user_id
name
analysis_definition_key
active_version_id
status
created_at
updated_at
```

## 4. ai_prompt_version

职责：

- 不可变 Prompt 历史。

建议字段：

```text
id
prompt_profile_id
version_no
content
content_hash
created_at
```

旧 Version 内容不可修改。

## 5. information_analysis

职责：

- 一次逻辑 Information Analysis；
- 绑定 User、Snapshot、Prompt Version、Definition；
- 保存业务结果。

建议字段：

```text
id
user_id
information_id
snapshot_id
information_type

analysis_definition_key
analysis_definition_version
analysis_purpose

prompt_profile_id
prompt_version_id

status

result_json
relevance_score
summary

estimated_input_tokens
estimated_output_tokens
estimated_total_tokens
estimate_method

started_at
completed_at
created_at
updated_at
```

逻辑身份：

```text
user_id
+ snapshot_id
+ prompt_version_id
+ analysis_definition_key
+ analysis_definition_version
```

是否做数据库 UNIQUE 必须在 TASK-020 结合失败重试历史冻结；TASK-024 不得临场自行决定。

## 6. ai_model_invocation

职责：

- 每一次真实 Provider 请求；
- Actual Token 事实来源。

建议字段：

```text
id
analysis_id
user_id

provider
model_name
provider_request_id

attempt_no
status

input_tokens
output_tokens
total_tokens
cached_input_tokens
reasoning_tokens
usage_status

latency_ms

error_code
error_message

started_at
completed_at
created_at
```

规则：

- Provider 有 Usage 就存；
- Analysis FAILED 不影响 Usage；
- Provider 无 Usage 时 actual 字段 NULL；
- Estimate 不得写入 actual 字段。

## 7. ai_analysis_batch

职责：

- 一次手动确认或 Schedule 触发的批任务；
- 冻结时间窗口、Prompt Version、限制和统计。

建议字段：

```text
id
user_id

trigger_type
schedule_id

information_type
analysis_definition_key
analysis_definition_version

prompt_profile_id
prompt_version_id

window_basis
requested_window_days
window_start
window_end

requested_max_candidates
requested_token_budget

total_in_window
eligible_count
already_analyzed_count
selected_count
deferred_count

estimated_input_tokens
estimated_output_tokens
estimated_total_tokens
estimate_method

status
skip_reason

scheduled_for
started_at
completed_at
created_at
updated_at
```

Actual Token 通过 Invocation 聚合。若 Batch 保存汇总列，只能是派生缓存，不是事实源。

## 8. ai_analysis_batch_item

职责：

- 冻结 Batch 候选；
- 记录选择、跳过、延期和执行结果。

建议字段：

```text
id
batch_id
information_id
snapshot_id
analysis_id

selection_order
status
decision_reason

estimated_input_tokens
estimated_output_tokens
estimated_total_tokens

created_at
updated_at
```

可能原因：

```text
SELECTED
SKIPPED_ALREADY_ANALYZED
DEFERRED_ITEM_LIMIT
DEFERRED_TOKEN_BUDGET
SUCCEEDED
FAILED
```

最终枚举由 Contract 冻结。

## 9. ai_analysis_schedule

职责：

- 每日自动分析配置；
- 只创建 Batch，不直接调用 Provider。

建议字段：

```text
id
user_id
name

information_type
analysis_definition_key
prompt_profile_id

enabled

local_time
timezone

window_days
max_candidates
max_estimated_tokens

last_triggered_at
next_run_at

created_at
updated_at
```

规则：

```text
enabled default false
local_time default 02:00
timezone 使用 IANA 名称
```

## 10. Schedule 幂等

同一 Schedule 的同一计划时刻只能产生一次运行。

候选唯一身份：

```text
(schedule_id, scheduled_for)
```

可以落在 Batch UNIQUE，也可以引入独立 Run 表。TASK-030 最终决定。

## 11. 用户 Token 聚合

不以 `token_statistics` 作为事实表。

事实：

```text
ai_model_invocation
```

聚合维度：

- 今日；
- 本月；
- 累计；
- Batch；
- Analysis；
- Provider；
- Model。

性能不足以后再考虑汇总表。

## 12. 成本

可选保存：

```text
pricing_version
currency
derived_cost
```

不做支付/余额/套餐/结算。

没有可靠价格快照时 Cost 为 NULL。

## 13. FK 与删除

建议：

- Analysis → Snapshot 使用保护性 FK；
- Prompt Version 被引用后不可物理删除；
- User AI 历史默认不级联硬删；
- Schedule 以禁用为主；
- Batch / Analysis / Invocation 保留审计历史。

最终 ON DELETE 与既有数据库规则在 TASK-024 统一。

## 14. 时间

沿用现有平台：

- DB UTC；
- API 带偏移；
- Schedule 保存 IANA timezone；
- local_time 表达墙上时间；
- next_run_at 使用 UTC。

## 15. 不新增

```text
embedding
vector
recommendation
notification
billing
organization
tenant
```
