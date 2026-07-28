# 当前开发状态

更新时间：2026-07-28
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
- 完成 TASK-001 BOSS 输出结构审计。
- 完成 TASK-002 并冻结 InformationEnvelope V1。
- 接受数据库设计 1.3 和 ADR-007。
- 完成 TASK-003，创建可编译、测试和启动的 Information Hub Spring Boot 项目骨架。
- 完成 TASK-004A，建立远程 Docker MySQL 开发与集成测试环境，并通过安全、权限、持久化和备份恢复验证。
- 接受 ADR-008，确定使用远程 Docker MySQL 作为共享开发与集成测试数据库，并通过 SSH 隧道安全连接。
- 完成 TASK-004，通过 Flyway V1 创建 Phase 1 数据库结构，并在独立测试库完成迁移、约束、JSON、幂等、快照版本和事务回滚验证。
- 完成 TASK-005，实现 InformationEnvelope V1 接入 API、Bearer Token 认证、请求校验、非破坏性合并、Canonical Hash、MyBatis-Plus 幂等归档和版本快照，补齐实体字段与关键逻辑的中文注释，并通过本地与独立测试库验证。
- 完成 TASK-006A，为 BOSS 职位搜索接口增加默认关闭的原始响应诊断开关，可在字段转换前打印并按请求原样保存响应体；专项测试 11 项全部通过，完整回归未新增失败。
- 接受 ADR-009，确定将 BOSS 招聘者在线状态记录为 Collector 在线观测时间，并将 `recruiterActiveText` 排除出 contentHash。
- 完成 TASK-006B：Collector 输出严格布尔 `boss_online` 和同响应共享的 UTC `boss_online_observed_at`；Information Hub 将 `recruiterActiveText` 排除出 contentHash，并验证在线观测时间的非破坏性更新不会增加版本或快照。
- 完成 TASK-006：新增独立 BOSS Mapper 适配层，完成列表/详情左连接、InformationEnvelope V1 字段转换、历史时区解释、招聘者在线观测映射和 rawPayload 递归安全清理；专项测试 14 项通过，完整回归未新增失败。
- 完成 TASK-007：新增默认关闭的 Information Hub HTTP Client，从环境变量读取 URL、Bearer Token 和连接/读取超时；明确处理 2xx、4xx、413、5xx、超时和连接失败，并在本地保存完成后以不影响原采集流程的方式提交单条职位；专项测试 17 项通过，完整回归未新增失败。
- 完成 TASK-007A：Information Hub 增加外部 `config/application.yml` 示例，Collector 增加默认 `config/collector.ini`、可选 `--config` 和环境变量覆盖；真实配置文件由 Git 忽略，专项测试 23 项通过，完整回归未新增失败。
- 完成 TASK-007B：修复直接执行 `boss_cdp_raw.py` 时无法加载同级 `integrations` 包的问题；脚本现在基于自身路径初始化 Collector 根目录，支持从任意工作目录使用绝对脚本路径启动，新增无真实网络的入口子进程回归测试，专项 39 项全部通过。
- 完成 TASK-008：Collector 新增本地文件 Outbox，对 Hub 可重试失败原子保存当前及剩余安全 Envelope；提供不依赖 Chrome/BOSS 的独立补传命令、指数退避、损坏隔离和永久失败归档；修复新采集内存对象缺少 `scraped_at` 导致映射提前失败的问题，相关回归 49 项通过，完整回归未新增失败。

## 当前任务

TASK-009：实现职位查询 API。

## 已冻结设计

- 平台 `sourceItemId` 使用 `encrypt_job_id`，`job_id` 只用于 Collector 内部合并。
- `boss_name` 映射 `companyName`，`encrypt_boss_id` 映射 `sourceRecruiterId`。
- `security_id` 和 `lid` 在提交 Information Hub 前删除。
- 历史无时区时间按 `Asia/Shanghai` 解释；新采集时间必须带明确时区。
- `detailStatus` 支持 `UNKNOWN`、`FETCHED`、`FAILED`、`UNAVAILABLE`。
- `tags` 规则识别失败时仅保留 `sourceTags`。
- 快照使用递增 `versionNo` 并支持 `A → B → A`。
- 更新采用非破坏性合并，null 或空值不覆盖已有有效值。
- `information_snapshot` 外键采用 `ON DELETE RESTRICT`。
- 开发与集成测试使用远程 Docker MySQL，生产数据库保持独立。
- MySQL 不向公网直接开放 `3306`，开发主机通过 SSH 隧道连接。
- 开发库与测试库分离，并使用不同的非 root 最小权限账号。
- Collector 接入 API 使用单 Bearer Token，Information Hub 可从外部 YAML 或环境变量读取，未配置时拒绝接入。
- 接入请求体默认最大 2 MiB，rawPayload 包含敏感字段时拒绝整个请求。
- `bossOnline=true` 时记录带时区的 Collector 在线观测时间；false 或缺失时本次映射为空。
- `recruiterActiveText` 表示最近一次被 Collector 观察到在线的时间，不代表 BOSS 官方最后活跃时间。
- `recruiterActiveText` 不参与 contentHash，仅该字段变化时不创建职位快照。
- Hub Client 默认关闭，可从 Collector INI 配置完整写入 URL、Bearer Token、连接超时和读取超时；环境变量具有更高优先级。
- Information Hub 和 Collector 的真实配置文件不纳入 Git，只提交不含秘密的 `.example` 模板。
- Hub 单条 4xx 或 413 不阻止后续职位；5xx、非预期状态、超时或连接失败时停止本批次剩余请求。
- Hub 的 5xx、非预期状态、超时、连接失败、请求失败和未知客户端异常进入本地 Outbox；4xx、413、配置错误和映射错误不入队。
- Outbox 位于 Collector 的 `result/outbox/`，只保存安全清理后的 Envelope 和重试元数据；使用独立 `--flush-outbox` 命令补传，不在采集启动时自动执行。
- Outbox 补传失败采用 60 秒起步、最大 1 小时的指数退避；成功删除，损坏文件隔离，永久失败响应归档。

## 下一步

执行 TASK-009，实现职位分页、详情和快照查询 API。
