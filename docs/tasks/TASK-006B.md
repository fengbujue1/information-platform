# TASK-006B：接入 BOSS 招聘者在线观测时间

状态：DONE
所属阶段：Phase 1  
优先级：P0

## 前置依赖

- TASK-002 完成。
- TASK-005 完成。
- TASK-006A 完成。

## 目标

将 BOSS 搜索响应中的 `bossOnline` 转换为可供 Information Platform 使用的“招聘者最近一次被 Collector 观察到在线的时间”。

本任务不是获取 BOSS 提供的真实最后活跃时间。BOSS 当前只返回瞬时布尔值，因此平台记录的是 Collector 在某次搜索响应中观察到 `bossOnline=true` 的时间。

## 背景

TASK-006A 的真实搜索响应样本包含 30 条职位，其中 `bossOnline=true` 13 条、`bossOnline=false` 17 条。样本没有提供可靠的官方最后活跃时间或活跃时间文本。

当前结构化 `boss_jobs_*.json` 尚未保留 `bossOnline`，TASK-006 Mapper 也不能依赖默认关闭的诊断原始响应文件，因此需要先建立稳定、安全的结构化输入。

## 业务语义

数据链路：

```text
BOSS jobList[].bossOnline
→ Collector boss_online
→ Collector boss_online_observed_at
→ InformationEnvelope.extension.recruiterActiveText
→ job_information.recruiter_active_text
```

映射规则：

| BOSS 值 | Collector 输出 | `recruiterActiveText` |
|---|---|---|
| `bossOnline=true` | `boss_online=true`，`boss_online_observed_at=<本次响应观测时间>` | `boss_online_observed_at` |
| `bossOnline=false` | `boss_online=false`，`boss_online_observed_at=null` | `null` |
| 字段缺失、null 或类型非法 | 两个字段均为 null | `null` |

`boss_online_observed_at` 必须是 ISO-8601 带明确偏移或 `Z` 的时间，例如：

```text
2026-07-27T05:11:12.345Z
```

同一次搜索接口响应中的职位共享同一个观测时间。时间必须在搜索响应到达并完成解析时生成，不得在数小时后读取文件或提交 Hub 时重新伪造。

## 最后在线时间的保留规则

平台采用非破坏性更新：

```text
第一次观察到在线 T1
→ recruiterActiveText = T1

后续观察到不在线
→ 本次上报 recruiterActiveText = null
→ Hub 保留已有 T1

再次观察到在线 T2
→ recruiterActiveText = T2
```

因此 `recruiterActiveText` 表示“最近一次被本 Collector 观察到在线的时间”，不表示：

- BOSS 官方最后活跃时间；
- 招聘者登录时间；
- 招聘者回复时间；
- 招聘者持续在线时长；
- 招聘者活跃度评分。

## contentHash 与快照

`recruiterActiveText` 是高频变化的观测元数据，不属于职位稳定业务内容。

必须将其从 `contentHash` 输入中排除：

- 在线观测时间变化仍更新 `job_information.recruiter_active_text`；
- 仅该字段变化时不得递增职位版本号；
- 仅该字段变化时不得创建 `information_snapshot`；
- rawPayload 和最近采集元数据仍按现有接入流程更新；
- 其他稳定业务字段变化时仍正常创建快照。

该决策见：

```text
docs/decisions/ADR-009-boss-recruiter-online-observation.md
```

## 影响范围

Collector：

- `collectors/boss-zhipin-scraper/scripts/boss_cdp_raw.py`
- `collectors/boss-zhipin-scraper/.gitignore`
- `collectors/boss-zhipin-scraper/tests/`

Information Hub：

- `backend/information-hub/src/main/java/com/informationplatform/hub/ingestion/domain/CanonicalContentHasher.java`
- 对应单元测试和必要的接入集成测试

文档：

- `docs/contracts/boss-job-field-mapping.md`
- `docs/contracts/information-envelope-v1.md`
- `docs/DATABASE_DESIGN.md`
- `docs/ARCHITECTURE.md`
- `docs/ROADMAP.md`
- `docs/CURRENT_STATUS.md`
- `docs/tasks/TASK-006.md`
- `collectors/boss-zhipin-scraper/INTEGRATION.md`

## 实施要求

### Collector

1. 只在现有 BOSS 搜索接口字段选择中增加：
   - `boss_online`
   - `boss_online_observed_at`
2. 允许本任务对采集核心做上述最小修改。
3. 不改变 Chrome、CDP、登录检测、请求地址、请求参数、请求频率、分页、去重和详情抓取逻辑。
4. 不让正式映射器读取 TASK-006A 的诊断原始响应文件。
5. 不把 `bossOnline=false` 当作空值丢弃；必须在 Collector 本地结构中保留明确的 `false`。
6. `boss_online_observed_at` 只在 `bossOnline` 严格等于布尔 `true` 时生成。
7. 观测时间必须带时区，且同一搜索响应只生成一次。
8. 建议在响应解析完成后生成一次 `new Date().toISOString()`，不得在逐职位映射时分别取时。
9. 新字段不得破坏现有 JSON、CSV、分析和详情抓取输出。
10. `result/chrome-profile/`、原始响应和运行结果必须保持在 Git 忽略范围，禁止提交 Cookie、会话和浏览器凭据。

### TASK-006 Mapper

TASK-006 实施时：

1. `boss_online_observed_at` 映射为 `recruiterActiveText`。
2. `boss_online=false`、字段缺失或观测时间缺失时映射为 null。
3. 不得使用 Mapper 实际运行时间替代来源观测时间。
4. 历史列表没有这两个字段时保持向后兼容。
5. `rawPayload.list` 保留 `boss_online` 和 `boss_online_observed_at`。
6. `boss_name` 仍映射 `companyName`，不得借本任务改为招聘者姓名。

### Information Hub

1. 从 Canonical Hash 输入中删除 `recruiter_active_text`。
2. 不删除数据库字段，不创建新的数据库表或 Flyway 迁移。
3. 保持非破坏性合并语义：
   - null 不覆盖已有观测时间；
   - 新的非空观测时间覆盖旧值。
4. Hash 相同时仍需更新当前 Job 扩展记录。
5. 不在 Controller 中增加业务逻辑。
6. 保持 Information Hub 现有明确事务边界，不拆分或绕过接入事务。

## 测试

Collector 至少覆盖：

- `bossOnline=true` 输出布尔 true 和带时区观测时间。
- `bossOnline=false` 输出布尔 false，观测时间为 null。
- `bossOnline` 缺失或 null 时两个字段均为 null。
- 非布尔值不被当作在线，也不生成观测时间。
- 同一搜索响应中的在线职位使用相同观测时间。
- JSON、CSV 和现有 API 字段提取测试保持兼容。
- TASK-006A 原始响应诊断功能保持可用。

Information Hub 至少覆盖：

- 仅 `recruiterActiveText` 不同时，Canonical Hash 相同。
- 首次在线时间可以保存。
- 后续 null 不清空已有在线时间。
- 新的非空在线时间更新当前记录。
- 仅在线时间变化不增加 `versionNo`，不创建快照。
- 在线时间之外的稳定业务字段变化仍创建快照。

TASK-006 Mapper 后续至少覆盖：

- true 对应的观测时间原样映射。
- false、缺失和非法观测时间映射为 null。
- 输出时间必须包含偏移或 `Z`。
- rawPayload 保留来源在线布尔值和观测时间。

## 验收标准

- [x] Collector 输出 `boss_online` 和 `boss_online_observed_at`。
- [x] true、false、缺失和非法类型语义明确且有测试。
- [x] 观测时间为 ISO-8601 带时区时间。
- [x] 同一次响应只生成一个观测时间。
- [x] 未读取或依赖 TASK-006A 诊断原始响应文件。
- [x] `recruiterActiveText` 从 contentHash 中排除。
- [x] 仅在线时间变化不创建职位快照。
- [x] 非破坏性更新可以保留最后一次在线观测时间。
- [x] 无数据库迁移、无新基础设施。
- [x] 未改变 BOSS 请求和详情抓取行为。
- [x] Chrome Profile、原始响应和运行结果未进入 Git。
- [x] 相关合同、设计文档、TASK 和 CURRENT_STATUS 保持一致。
- [x] 所有新增测试通过，完整回归未新增失败。

## 不在本任务范围

- 不获取或推测 BOSS 官方最后活跃时间。
- 不记录招聘者上线、下线事件流水。
- 不计算在线时长或活跃度评分。
- 不实现前端展示和筛选。
- 不新增 `recruiter_last_online_at` 数据库列。
- 不把 `bossOnline` 映射为职位状态。
- 不接入招聘者姓名、头像、认证或联系状态。
- 不捕获详情页面原始响应。
- 不实施 Information Hub Client 或 Outbox。

## 风险

1. `bossOnline` 是采集瞬间状态，受 BOSS 返回语义、登录态和网络环境影响，只能作为弱活跃信号。
2. `recruiterActiveText` 是字符串字段，本任务保存 ISO-8601 时间文本；未来如需时间范围查询，应升级为专用时间列。
3. Collector 主机时钟不准确会影响观测时间，因此时间必须在响应处理时生成并带明确时区。
4. false 上报 null 依赖 Hub 的非破坏性合并保留旧值；测试必须覆盖该链路。
5. 将该字段从 Hash 排除后，历史快照不记录每次在线观测变化，这是有意设计。

## 实施记录

完成时间：2026-07-27

- 在 BOSS 搜索响应字段选择中以严格布尔类型读取 `bossOnline`，输出 `boss_online`；字段缺失、null 或非布尔值统一归一为 null。
- 每次搜索响应完成解析时生成一次 UTC ISO-8601 观测时间；同一响应中仅 `boss_online=true` 的职位共享该时间，false 或未知值对应的 `boss_online_observed_at` 为 null。
- 未修改请求地址、请求参数、分页、登录检测、频率、详情抓取和 TASK-006A 原始响应诊断行为；JSON 输出保留新字段，固定列 CSV 忽略扩展字段并保持兼容。
- 将 `result/chrome-profile/` 加入 Collector Git 忽略规则，浏览器会话数据不进入版本控制。
- 从 Canonical Hash 输入中排除 `recruiterActiveText`，但继续在标准化快照 payload 和当前 Job 扩展记录中保留该字段。
- 复用现有非破坏性合并与 `TransactionTemplate` 事务边界：null 保留历史观测时间，新的非空时间更新当前记录；仅该字段变化不增加版本号、不创建快照。
- 未创建数据库迁移、数据库表、Controller 逻辑或新基础设施；TASK-006 Mapper 仍留给 TASK-006 实施。

验证结果：

- `python -m py_compile scripts/boss_cdp_raw.py`：通过。
- `python -m unittest tests.test_recruiter_online_observation tests.test_raw_response_capture`：15 项通过。
- Collector 完整回归：91 项中 86 项通过；5 项 Windows Chrome 路径/端口基线失败与 TASK-006A 前一致，本任务未新增失败。
- `mvnw.cmd -Dtest=CanonicalContentHasherTest,NonDestructiveInformationMergerTest test`（JDK 21）：5 项通过。
- `mvnw.cmd test`（JDK 21）：24 项、0 失败；普通运行中 11 项远程数据库条件测试按既有配置跳过。
- 通过既有 SSH 隧道连接共享测试库执行 `InformationIngestionIntegrationTest`：5 项全部通过、0 跳过。
- `git diff --check`：通过；Chrome Profile、原始响应和运行结果未进入 Git。

