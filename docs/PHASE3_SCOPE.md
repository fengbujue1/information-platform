# Phase 3：AI Processing Foundation & User Relevance MVP

状态：Accepted / Completed
接受日期：2026-08-03
完成日期：2026-08-07
适用阶段：Phase 3

## 1. 定位

Phase 3 建设面向通用 Information 的 AI Processing Foundation，并以 `JOB + USER_RELEVANCE` 作为第一个真实业务实现。

```text
information_type = JOB
analysis_purpose = USER_RELEVANCE
analysis_definition_key = JOB_USER_RELEVANCE
```

JOB 只是 Information 的一种类型。通用 AI Core 不绑定招聘专属命名。

## 2. Phase 3 完成能力

- Identity MVP；
- 登录、登出、当前用户；
- Session / CSRF / Owner；
- Prompt Profile；
- immutable Prompt Version；
- Analysis Definition；
- Snapshot 级 Information Analysis；
- OpenAI-compatible Provider；
- Provider Invocation；
- Actual Token Usage；
- Prompt Assembly；
- strict structured output；
- 最近 N 天 Candidate / Preview；
- Token Estimate；
- Manual Batch；
- Scheduled Batch；
- Budget Guard；
- Daily Schedule；
- Phase 3 Web；
- Fake Provider Full-stack E2E；
- 真实 Provider 受控联调。

## 3. Identity 与访问边界

- 浏览器使用服务端 Session；
- 状态修改请求必须 CSRF；
- Owner 来自认证上下文；
- `/api/v1/jobs/**` 与 AI 用户接口使用 Session；
- `/api/v1/collector/**` 保持独立 Bearer Token；
- 不引入公共注册、OAuth、MFA、RBAC、Tenant、Organization。

## 4. Prompt Profile 与 Version

- 一个用户可以有多个 Prompt Profile；
- Prompt 正文通过 immutable Version 保存；
- Active Version 指向当前版本；
- 相同 contentHash 可复用历史 Version；
- 用户不能修改 System Prompt 或 Output Schema。

## 5. Analysis Definition

Phase 3 使用代码注册表，不创建 `analysis_definition` 数据库表。

Definition 固定：

- information type；
- analysis purpose；
- definition key / version；
- Input Projection；
- System Prompt；
- Output Schema；
- Validator；
- `maxOutputTokens`。

### V1

```text
JOB_USER_RELEVANCE / version 1
maxOutputTokens = 1000
```

### V2

真实 Provider 联调后新增兼容版本：

```text
JOB_USER_RELEVANCE / version 2
maxOutputTokens = 5000
```

V1 保持不可变。

V2 继续复用：

- Input Projection V1；
- System Prompt Version 1；
- Output Schema Version 1；
- Validator V1；
- Source Content Boundary V1。

新的 Analysis 使用 Registry 当前最高版本。

## 6. Snapshot 与来源事实

Analysis 必须绑定不可变 `information_snapshot.id`。

AI 分析不得覆盖：

- `information_item`；
- `job_information`；
- `information_snapshot`；
- `raw_payload`。

模型默认只接收标准化字段和必要正文。

来源内容必须按不可信数据隔离。

## 7. Analysis 幂等

逻辑唯一键：

```text
user_id
+ snapshot_id
+ prompt_version_id
+ analysis_definition_key
+ analysis_definition_version
```

- SUCCEEDED：重复请求复用；
- FAILED：显式 retry；
- Provider / Model 不进入逻辑唯一键；
- 新业务语义通过 Prompt Version 或 Definition Version 演进。

## 8. Provider 与 Usage

- OpenAI-compatible Client；
- Provider 默认 disabled；
- API Key 只存在服务端；
- Actual Token 只来自 Provider Usage；
- Provider 未返回 Usage 时保持 UNAVAILABLE；
- Estimated Token 不得回填为 Actual；
- timeout / ambiguous failure 不自动盲重试；
- Provider HTTP 在数据库事务外。

## 9. Preview / Batch

- 最近 N 天窗口；
- FIRST_INGESTED 候选；
- 已分析过滤；
- Token Estimate；
- Candidate Limit；
- Token Budget；
- HMAC Confirm Token；
- Preview 不持久化；
- Manual / Schedule 复用同一 Batch Engine；
- Worker 使用短事务。

## 10. Schedule

- 每用户 timezone；
- 每日运行时间；
- Schedule 默认 disabled；
- 由 Dispatcher 创建 Scheduled Batch；
- overlap / misfire / idempotency 受控；
- Provider 调用仍走 Batch Worker。

## 11. Phase 3 明确不做

- 非 JOB 的真实 AI Definition；
- RAG；
- Embedding；
- Vector DB；
- Elasticsearch；
- Kafka / Redis；
- 自动 Top N 推荐；
- Notification；
- 公共注册；
- 复杂权限；
- 微服务。

## 12. 完成结论

Phase 3 的 Identity、Prompt、Analysis、Provider、Preview、Batch、Schedule、Usage 和 Web 主链已经实现并完成阶段验收。

CI 不作为本次阶段关闭的阻塞条件，后续独立维护。

Phase 4 尚未开始。
