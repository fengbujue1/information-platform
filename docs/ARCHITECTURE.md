# Information Platform Architecture

## 1. 当前总体结构

```text
BOSS Collector
    │
    │ 合并列表 JSON 和详情 JSON
    │ InformationEnvelope V1 / HTTP
    ▼
Information Hub
    ├── ingestion
    ├── information
    ├── job
    ├── identity（Identity MVP 已实施）
    ├── analysis（Prompt 已实施，其余按 Phase 3 TASK 推进）
    ├── recommendation（后续）
    └── notification（后续）
    │
    ▼
MySQL
    ├── information_item
    ├── job_information
    ├── information_snapshot
    ├── user_account
    ├── ai_prompt_profile / ai_prompt_version
    ├── information_analysis / ai_model_invocation
    └── ai_analysis_batch / item / schedule
```

Collector 不直接连接 MySQL。

## 2. Phase 1 边界

Phase 1 跑通：

```text
BOSS 列表采集
→ BOSS 详情采集
→ 按 job_id 合并
→ 使用 encrypt_job_id 构建幂等身份
→ Information Hub
→ 当前版本和历史快照
→ Job Query API
```

Phase 1 不包含 Vue、AI、推荐、消息队列和微服务。

## 3. 身份边界

- `job_id`：Collector 内部列表与详情 Join Key。
- `encrypt_job_id`：BOSS 来源职位 ID。
- `information_item.source_item_id`：保存 encrypt_job_id。
- 平台不依赖 job_id 的派生算法。

## 4. 数据变化

```text
读取当前记录
→ 非破坏性合并
→ 规范化稳定业务字段
→ 计算 contentHash
→ Hash 相同：更新最近采集元数据
→ Hash 不同：更新当前版本并新增 version_no 快照
```

招聘者在线观测时间属于高频采集元数据，不参与 contentHash。仅该字段变化时更新当前 Job 扩展记录，不增加版本号，不创建快照。

快照使用版本号唯一约束，允许记录：

```text
A → B → A
```

## 5. 原始数据

中央 rawPayload 保存：

- 安全清理后的列表对象；
- 安全清理后的详情对象。

中央 rawPayload 不保存：

- security_id
- lid
- Cookie
- Token
- 浏览器凭证

## 6. 状态边界

- `information_item.status`：平台内部生命周期。
- `job_information.job_status`：来源职位业务状态。
- `job_information.detail_status`：详情抓取状态。

三者不得混用。

## 7. 招聘者在线观测

- BOSS `bossOnline` 只表示搜索响应到达时的瞬时状态。
- true 时 Collector 生成带时区的 `boss_online_observed_at`。
- false 或缺失时不生成新的观测时间。
- Mapper 将 `boss_online_observed_at` 映射为 `recruiterActiveText`。
- Hub 依靠非破坏性合并保留最后一次非空观测时间。
- 该时间不是 BOSS 官方最后活跃时间，不用于计算在线时长或活跃度评分。

## 8. 时间

- 新输出必须包含时区。
- 历史无时区数据必须通过显式配置解释。
- 所有数据库时间统一存 UTC。
- publishTime 不得由 collectedAt 伪造。

## 9. 开发与集成测试数据库

开发和集成测试使用远程服务器中的 Docker MySQL：

```text
开发主机 A ── SSH 隧道 ──┐
                         ▼
开发主机 B ── SSH 隧道 ── 远程服务器 127.0.0.1:MySQL 宿主机端口
                         │
                         ▼
                    MySQL 容器
                         │
                         ▼
                    持久化数据卷
```

安全边界：

- MySQL 宿主机端口只绑定远程服务器的回环地址，不直接暴露公网。
- 服务器防火墙或安全组不允许公网入站访问 MySQL 端口。
- 开发主机通过使用密钥认证的 SSH 隧道连接数据库。
- 开发库与集成测试库分离，并使用不同的非 root 最小权限账号。
- 自动测试只允许连接测试库。
- 密码、SSH 私钥和其他连接凭据不进入 Git。
- 数据库结构只通过 Flyway 迁移，禁止在共享环境执行 `flyway clean`。
- 远程共享数据库只用于开发和集成测试，生产环境必须使用独立数据库。
- Collector 仍然只连接 Information Hub，不直接连接 MySQL。

## 10. Phase 1 后端持久化

- 业务表读写使用 MyBatis-Plus 3.5.17 的 Spring Boot 3 Starter。
- `information` 模块保存 `information_item`、`information_snapshot` 的 PO 和 Mapper。
- `job` 模块保存 `job_information` 的 PO 和 Mapper。
- `ingestion` 基础设施适配器负责组合三个 Mapper，但事务边界仍由应用服务控制。
- 普通单表插入和更新使用 MyBatis-Plus `BaseMapper`。
- 幂等更新前的当前记录查询使用固定 `FOR UPDATE` 锁查询，锁定后再读取 Job 扩展。
- 主表、Job 扩展和快照继续使用同一个 Spring 事务。
- Flyway SQL 仍是数据库结构的最终事实来源，MyBatis-Plus 不负责自动建表。

## 11. Phase 3 Accepted 架构与实施边界

TASK-020 已冻结 Phase 3 设计，TASK-024 已实施数据库与 MyBatis-Plus 持久化基础：

```text
Information Hub Web
→ Same-origin Session + CSRF
→ identity / information / job / analysis
→ MySQL
→ OpenAI-compatible Provider（默认关闭）
```

边界：

- 后端模块名使用 `identity`，不是泛化的 `user` 大模块。
- `/api/v1/jobs/**` 已纳入 Session Auth。
- `/api/v1/collector/**` 继续使用独立 Bearer Token。
- Analysis 绑定 `information_snapshot.id`；当前 Snapshot 仍通过 `(information_id, current_version_no)` 读取。
- FIRST_INGESTED 使用 `information_item.first_seen_time`。
- Provider Client 使用 Spring `RestClient` 与 Jackson；CI 使用 Fake Provider。
- Preview 使用短期 HMAC token，不建表、不调用 AI。
- Worker/Scheduler 使用 Spring + MySQL 短事务；外部 HTTP 调用不持有数据库事务。
- Actual Token 的唯一事实来源是 `ai_model_invocation`。

当前已实施：

- V2 Flyway Migration；
- 8 张 Phase 3 核心表及 FIRST_INGESTED 查询索引；
- `identity` 的 `UserAccountPo` / Mapper；
- `analysis` 的 7 组 PO/Mapper；
- 数据库结构、升级、空库 Migration 和 Mapper 集成测试。
- Identity 登录、Bootstrap、Session、CSRF 与 Owner 上下文；
- Job Query API 与 Information Hub Web 的 Session 认证适配；
- 保持独立 Bearer Token 认证的 Collector 安全边界。
- 账号级 Prompt Profile、不可变 Prompt Version、稳定 contentHash、事务内 Active Version 切换与 Owner 隔离 API。
- 通用 Analysis Definition Registry、唯一的 `JOB_USER_RELEVANCE_V1`、Snapshot 输入投影、版本化平台 System Prompt / Output Schema 与严格输出校验。
- 通用 AiProviderClient、OpenAI-compatible Adapter、Provider Usage/错误语义、默认禁用配置与 CI Fake Provider。
- 稳定三消息 Prompt Assembly、版本化执行上下文、1 MiB 响应上限、单一 JSON 解析与 Definition Schema 后处理。
- 绑定不可变 Snapshot 与 Prompt Version 的单条 Analysis 执行、逻辑身份幂等、Invocation attempt、Actual Usage 持久化、短事务和 Owner 隔离 API。
- Manual Preview Confirm、原子 Batch/Items 冻结、Candidate/Token Budget Guard、串行 MySQL Worker、重启 UNKNOWN 保护和 Batch Actual Usage 聚合。
- Owner 隔离的每日 Schedule 配置 API、IANA/DST 计划点、due-row Dispatcher、Scheduled Batch 幂等、misfire/overlap NOOP 与现有 Worker 复用。

当前尚未实施：

- 真实模型与 Phase 3 E2E。

详细设计以 Accepted `PHASE3_ARCHITECTURE_DRAFT.md` 和 `DATABASE_DESIGN_PHASE3_DRAFT.md` 为准。文件名中的 `_DRAFT` 为保持既有链接而保留，不表示设计仍待讨论。TASK-031 已完成 Phase 3 Web 与用户维度 Actual Usage 查询；当前下一实施任务是 TASK-032。
