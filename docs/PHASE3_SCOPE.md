# Phase 3：AI Processing Foundation & User Relevance MVP

状态：Accepted
接受日期：2026-08-03
适用阶段：Phase 3

## 1. 定位

Phase 3 建设面向通用 Information 的 AI Processing Foundation，并只为 `JOB` 实现第一个真实业务分析：

```text
information_type = JOB
analysis_purpose = USER_RELEVANCE
analysis_definition_key = JOB_USER_RELEVANCE
analysis_definition_version = 1
```

JOB 只是 Information 的一种类型。通用核心不得出现 `JobPrompt`、`JobAnalysisBatch` 等把 AI 基础设施绑定招聘业务的模型。

## 2. Phase 3 必须完成

- Identity MVP：登录、登出、当前用户、Session、CSRF、Owner 隔离；
- 账号级 Prompt Profile 与不可变 Prompt Version；
- 平台控制的 Analysis Definition、System Prompt 和 Output Schema；
- Snapshot 级 `information_analysis`；
- OpenAI-compatible Provider 与 CI Fake Provider；
- Provider Invocation 与 Actual Token Usage；
- 最近 N 天候选 Preview；
- Manual / Scheduled 共用的异步 Batch Engine；
- Candidate Limit 与 Estimated Token Budget；
- 每日 Schedule；
- Phase 3 Web；
- 小规模受控真实模型 E2E。

## 3. Identity 与访问边界

Phase 3 引入 Spring Security，但不引入公共注册、OAuth、MFA、RBAC、Tenant 或 Organization。

浏览器使用服务端 Session：

- Session Cookie 必须 `HttpOnly`、`SameSite=Lax`，生产 HTTPS 下启用 `Secure`；
- 登录成功后启用 Session fixation protection；
- 状态修改请求必须经过 CSRF；
- 提供 `GET /api/v1/auth/csrf` 供同源 Web 初始化 CSRF token；
- 第一版使用单实例内存 Session，不提前加入 Spring Session JDBC 表。

现有 `/api/v1/jobs/**` 在 Phase 3 纳入 Session Auth。现有 Collector 接口继续使用独立 Bearer Token，不进入用户 Session 和 CSRF 流程。

初始账号采用一次性受控 Bootstrap：

- 仅从服务端环境变量读取用户名和初始密码；
- 仅当 `user_account` 为空时创建；
- 仓库无默认账号、无默认密码；
- 密码和摘要不得写入日志；
- 首次创建后移除 Bootstrap 密码配置。

用户名在应用层 `trim + lower-case` 后保存和查询，并使用大小写不敏感唯一语义。用户默认时区为 `Asia/Shanghai`。

## 4. Prompt Profile 与 Version

- 一个用户可以有多个 Profile；
- `(user_id, name)` 唯一；
- Profile 绑定 `analysis_definition_key`，不保存可变 Prompt 正文；
- Prompt 每次实际变更产生不可变 Version；
- 同一 Profile 的相同 `content_hash` 复用已有 Version，不制造重复版本；
- Profile 用 `active_version_id` 指向当前版本；
- Service 必须在事务内验证 Active Version 属于该 Profile；
- User Prompt 最大 8,000 字符；
- 用户不能修改 System Prompt 或 Output Schema。

## 5. Analysis Definition

`analysis_definition` 第一版是代码注册表，不建数据库表。

Definition 固定：

- information type；
- analysis purpose；
- definition key / version；
- Input Projection；
- System Prompt 资源和版本；
- Output Schema 和 Validator；
- `maxOutputTokens`。

Phase 3 唯一真实 Definition 为 `JOB_USER_RELEVANCE_V1`，单次 `maxOutputTokens = 1000`。

## 6. Snapshot 与来源事实

Analysis 必须绑定不可变 `information_snapshot.id`，不得只绑定当前 `information_item`。

当前真实数据库事实：

- `information_item.id`：`BIGINT UNSIGNED`；
- `information_snapshot.id`：`BIGINT UNSIGNED AUTO_INCREMENT` 主键；
- Snapshot 唯一键：`(information_id, version_no)`；
- 当前 Snapshot 通过 `information_item.current_version_no` 与 `(information_id, version_no)` 关联；
- 不新增 `current_snapshot_id`。

Phase 3 可新增仅供内部使用的按 `snapshot_id` 读取投影，不扩大公开 Job Query API，也不返回 `raw_payload`。

AI 结果只写入 Phase 3 AI 表，不覆盖：

- `information_item`；
- `job_information`；
- `information_snapshot`；
- `raw_payload`。

默认模型输入仅使用标准化字段和必要正文。来源内容必须作为不可信数据隔离，不能覆盖平台 System Prompt。

## 7. Analysis 幂等与重试

逻辑唯一键固定为：

```text
user_id
+ snapshot_id
+ prompt_version_id
+ analysis_definition_key
+ analysis_definition_version
```

- 已 `SUCCEEDED`：普通重复调用复用结果；
- 已 `FAILED`：显式重试复用同一 Analysis，并创建新的 Invocation；
- Provider/Model 不进入逻辑唯一键；
- 需要产生新业务结果时升级 Prompt Version 或 Definition Version；
- 不提前创建 `analysis_generation`。

## 8. Provider 与 Actual Token

第一版实现通用 `OpenAiCompatibleChatClient`，使用 Spring `RestClient` 和现有 Jackson `ObjectMapper`，不引入 AI SDK。

Provider 配置只存在服务端：

- `enabled` 默认 `false`；
- `baseUrl`；
- `apiKey`；
- `model`；
- timeout；
- `maxOutputTokens`。

第一家受控真实 E2E Provider 推荐使用阿里云百炼 Qwen 的 OpenAI-compatible Chat Completions；CI 只使用 Fake Provider，不访问公网。

Actual Token 的唯一事实来源是 Provider Usage，并保存到每一次 `ai_model_invocation`。Provider 未返回 Usage 时 token 字段为 `NULL`，`usage_status = UNAVAILABLE`。Estimated Token 绝不能回填为 Actual。

Invocation 绑定 `batch_item_id`，确保某个 Batch 的历史 Usage 不会因同一 Analysis 后续重试而变化。

## 9. Candidate 与 Preview

JOB 的 `FIRST_INGESTED` 物理字段固定为：

```text
information_item.first_seen_time
```

该值由 Information Hub 在首次插入时使用服务端时间设置，后续更新不改变。

候选查询：

```text
information_type = JOB
first_seen_time ∈ [window_start, window_end)
ORDER BY first_seen_time DESC, id DESC
```

Preview 只做读取与 Estimate，不调用 Provider，不创建 Invocation，也不新增 Preview 表。

Manual Preview 返回 10 分钟有效的服务端 HMAC 签名 token，至少冻结：

- user；
- profile；
- prompt version；
- definition key/version；
- window start/end；
- requested limits；
- 有序候选及 Estimate 指纹；
- 随机 `manualRequestId`。

Confirm 必须重新解析相同绝对窗口并比较指纹。token 过期、Owner/Version/候选/Estimate 不一致时拒绝创建 Batch。HMAC 使用 JDK 能力，不引入 JWT SDK。

## 10. Token Estimate 与平台限制

第一版 Estimate 算法固定为：

```text
baseTokens = ceil(UTF-8 字节数 / 3)
estimatedInputTokens = ceil((baseTokens + 64) × 1.20)
estimatedOutputTokens = definition.maxOutputTokens
estimatedTotalTokens = estimatedInputTokens + estimatedOutputTokens
estimateMethod = UTF8_BYTES_DIV3_MARGIN20_V1
```

默认请求值：

- `windowDays = 3`；
- `maxCandidates = 20`；
- `maxEstimatedTokens = 75_000`。

平台硬上限：

- `windowDays <= 14`；
- `maxCandidates <= 50`；
- `maxEstimatedTokens <= 200_000`；
- 单次 `maxOutputTokens <= 1000`。

## 11. Batch Engine

Manual 和 Schedule 必须共用同一个 Batch 创建与执行引擎。

Batch 创建时冻结：

- user；
- trigger type；
- definition key/version；
- prompt profile/version；
- absolute window；
- limits；
-统计；
- 有序执行候选；
- 每项 Estimate。

`maxCandidates` 之外的候选只保存 Batch 聚合计数；进入 Candidate Limit 后又被 Token Budget 延后的候选保存为 Batch Item，原因标记为 `TOKEN_BUDGET`。

Manual Batch 使用 `(user_id, manual_request_id)` 唯一约束保证重复 Confirm 不重复建批。Scheduled Batch 使用 `(schedule_id, scheduled_for)` 唯一约束保证同一计划点只产生一批。

## 12. Worker 最小可靠方案

- 模块化单体内后台 Worker，单并发；
- 周期性轮询数据库；
- MySQL `FOR UPDATE SKIP LOCKED` 领取工作；
- 领取事务只把 Batch Item、Analysis、Invocation 标记为 `RUNNING`；
- 外部 Provider 调用不持有数据库事务；
- 完成或失败在短事务内落库；
- 不引入 Kafka、Redis 或独立 Worker 服务。

超时或进程中断留下的 `RUNNING` Invocation 视为结果不确定，转为 `UNKNOWN`/失败并进入人工重试判断；不对可能已计费的请求自动无脑重试。

## 13. Schedule 最小可靠方案

- Schedule 默认 `enabled = false`；
- 默认用户本地 `02:00`；
- 使用 IANA timezone；
- Dispatcher 每 30～60 秒扫描 `enabled = 1 AND next_run_at <= now`；
- 事务内行锁、重新校验并以 `next_run_at` 作为 `scheduled_for`；
- 创建 Batch 或因重叠创建 NOOP Batch 后，原子推进下一次未来执行时间；
- 同一 Schedule 存在 `PENDING/RUNNING` Batch 时不得重叠执行；
- 允许 5 分钟 misfire grace；超过后不补跑历史，直接推进下一次；
- Schedule 只创建 Batch，不直接调用 Provider。

Schedule 只保存 `prompt_profile_id`。触发时解析 Profile 的 Active Prompt Version 和当前 Definition Version，并冻结到 Batch。

## 14. Web 范围

Phase 3 Web 包含：

- Login / Logout；
- 认证路由保护；
- Prompt Profile / Version；
- 单条分析；
- Preview / Confirm；
- Batch 状态与 Usage；
- Schedule 配置。

前端使用同源 `/api`，不保存 Provider API Key、Collector Token 或其他服务器秘密。现有 Job 页面纳入 Session Auth；E2E fixture 必须先登录并携带 Cookie/CSRF。

## 15. 数据库边界

Phase 3 必需 8 张表：

```text
user_account
ai_prompt_profile
ai_prompt_version
information_analysis
ai_analysis_schedule
ai_analysis_batch
ai_analysis_batch_item
ai_model_invocation
```

第一版不新增：

- Spring Session JDBC 表；
- Preview 持久化表；
- Schedule Run 表；
- Analysis Definition 表；
- System Prompt 表；
- Token 汇总表；
- Cost/Pricing 表。

物理字段、索引、FK 和删除规则以 Accepted `docs/DATABASE_DESIGN_PHASE3_DRAFT.md` 为 TASK-024 的唯一设计输入。

## 16. 明确不做

- 非 JOB 类型的真实 AI 实现；
- 推荐 Top N 和通知；
- RAG、Agent、Embedding、Vector DB、Elasticsearch；
- Kafka、Redis、微服务；
- Prompt-driven Retrieval；
- 公共注册、复杂权限和多租户；
- 用户可编辑 System Prompt / Schema；
- 费用账单；
- rawPayload 直传模型。

## 17. TASK 顺序

数据库结构是 Identity、Prompt 和 Analysis 持久化的前置条件，执行顺序冻结为：

1. TASK-020：仓库审查与设计冻结；
2. TASK-024：数据模型与 Flyway；
3. TASK-021：Identity MVP；
4. TASK-022：Prompt Profile 与 Prompt Version；
5. TASK-023：Analysis Definition 与 Contracts 代码落地；
6. TASK-025：AI Provider 与 Fake Provider；
7. TASK-026：Prompt Assembly 与结构化输出；
8. TASK-027：单条 Analysis；
9. TASK-028：Candidate Resolver、Preview 与 Estimate；
10. TASK-029：异步 Batch 与 Budget Guard；
11. TASK-030：每日 Schedule；
12. TASK-031：Phase 3 Web；
13. TASK-032：受控真实模型与 E2E。

任务编号保持不变，仅修正前置依赖与实际执行顺序。

## 18. 完成标准

Phase 3 完成时必须能够：

- 用户安全登录；
- 创建、版本化并切换 Prompt；
- 对确定 Snapshot 执行 `JOB_USER_RELEVANCE_V1`；
- Preview 不产生真实调用；
- Manual / Schedule 复用同一 Batch Engine；
- 限制候选和 Estimated Token；
- 持久化每次 Provider Invocation 和 Actual Usage；
- Worker 重启后可恢复且不对不确定调用盲目重试；
- Web 展示分析、批次、Usage 和 Schedule；
- 真实 E2E 只在显式启用时以小规模数据运行。
