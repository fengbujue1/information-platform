# TASK-032：真实模型、定时任务与 Phase 3 E2E 验收

状态：IN PROGRESS
所属阶段：Phase 3
优先级：P0
负责人：User + Codex

## 1. 目标

使用小规模真实 JOB 数据和受控真实 AI Provider 验证 Phase 3 从登录到 Prompt、Preview、Batch、Usage、Schedule 的完整链路，并完成 Phase 3 文档收尾。

## 2. 前置依赖

- TASK-031 已完成并推送。
- 必须阅读 `docs/PHASE3_SCOPE.md` 和本任务相关 Contract / ADR。
- 开始前执行 `git status`、`git log --oneline -8`、`git diff --check`。

## 3. 本任务范围

自动化：
- Identity。
- Prompt Version。
- Fake Provider。
- Single Analysis。
- Preview。
- Batch。
- Budget。
- Schedule。
- Web E2E。

真实人工/受控 E2E：
- 配置真实 Provider Key（仅环境变量）。
- 选择少量 JOB Snapshot。
- Preview Estimated Token。
- 手动 Batch。
- 核对 Actual Token。
- 记录 Estimate vs Actual 偏差。
- 验证失败场景。
- 验证 Schedule 可在临时测试时间触发，验收后恢复默认/关闭。
- 验证用户 Usage。
- 验证没有 rawPayload 泄露。
- 验证 API Key 不在日志/前端/Git。
- 更新 README/ROADMAP/CURRENT_STATUS。
- Phase 3 标记完成。

## 4. 不在本任务范围

- 不大规模全库跑真实模型。
- 不为了验收启动推荐或通知。
- 不把真实职位隐私数据写入 Git fixture。
- 不在 CI 使用真实 API Key。
- 不把验收临时 Schedule 留在开启状态。

## 5. 实施原则

- 只完成当前 TASK。
- 不覆盖来源事实。
- 不把 rawPayload 默认发送给模型。
- 不把秘密写入 Git 或前端。
- 不提前引入 Kafka、Redis、Elasticsearch、向量数据库、RAG 或微服务。
- 不提前实现推荐和通知。
- Codex 修改前必须先汇报计划并等待确认。
- Codex 不提交 Git，由用户检查后提交。

## 6. 验收标准

- [x] Java 测试通过
- [x] Vue typecheck/test/build 通过
- [x] 浏览器 E2E 通过
- [ ] Fake Provider CI 通过（工作流已新增，同一命令本地通过；等待提交后 GitHub CI）
- [ ] 小规模真实 Provider 分析成功
- [x] Estimated vs Actual Token 有记录（Fake Provider 自动化证据已记录；真实 Provider 偏差待补）
- [x] Actual Token 与 Provider Usage 一致（Fake Provider）
- [x] 失败调用 Usage 规则验证
- [x] Manual Batch limits 验证
- [x] Schedule 默认关闭验证
- [x] Schedule 触发/幂等/overlap/misfire 规则验证
- [x] 用户 Usage 统计验证
- [x] 无 rawPayload/API Key 泄露（运行时随机 Fake Key；真实 Key 待受控验收）
- [x] README/ROADMAP/CURRENT_STATUS 更新
- [ ] Phase 3 标记完成
- [x] 下一阶段不自动启动

## 7. 实施前必须汇报

- 当前真实代码与文档基线；
- 前置依赖是否满足；
- 计划修改文件；
- 数据流 / 事务 / 安全边界；
- 测试计划；
- 风险；
- 与 Draft 设计不一致的地方；
- 明确不实施的内容。

## 8. 实施记录

### 8.1 自动化链路

新增真实 Web + Information Hub + MySQL + 本机 Fake Provider 的 Playwright 编排：

```text
Synthetic Collector JOB（包含仅用于泄露检测的 rawPayload marker）
→ Login / Session / CSRF
→ Prompt Profile + immutable active Version
→ Single Analysis
→ Preview（不产生 Actual）
→ Item Limit + Token Budget
→ Manual Confirm / asynchronous Worker
→ invalid structured output / retained Provider Usage
→ disabled Schedule / due trigger / Scheduled Batch
→ Owner Usage aggregation
→ Schedule disabled in finally
```

Fake Provider 只监听 `127.0.0.1` 随机端口，API Key 每次运行随机生成。测试明确拒绝包含 rawPayload marker 的模型请求，并检查浏览器 API 响应、页面和后端输出均不包含 rawPayload marker 或 Provider Key。后端通过 `--spring.config.location=classpath:/application.yml` 启动，避免未追踪本地配置覆盖专用测试库。

CI 新增 MySQL 8.4 service 和独立 `phase3-e2e` job，只使用公开的 CI 合成凭据，不读取真实 Provider Key。旧 `test:e2e` 固定只收集 `identity.spec.ts` 与 `jobs.spec.ts`，避免与全栈用例互相污染。

### 8.2 验收期间修复

TASK-031 Web 的 Batch 状态类型仍使用不存在的 `SUCCEEDED`，与后端 Contract 的 `COMPLETED/PARTIAL_FAILED` 不一致，导致完成批次继续轮询。本任务将类型和终态判断修正为后端冻结状态，并增加组件回归测试。

### 8.3 配置与文件

- `.github/workflows/ci.yml`：新增 Phase 3 全栈 Fake Provider CI job；
- `backend/information-hub/CONFIGURATION.md`：补齐 Provider 与 Phase 3 E2E 配置说明；
- `backend/information-hub/config/application.yml.example`：补齐默认关闭且无密钥的 Provider 示例；
- `backend/information-hub/src/main/resources/application.yml`：为全部显式运行配置补充中文含义、格式示例和来源注释；
- 后端 6 个 `@ConfigurationProperties` 配置对象：补充字段来源、单位、格式与安全语义注释；
- `frontend/information-hub-web/e2e/phase3.spec.ts`：新增完整 Phase 3 浏览器流程；
- `frontend/information-hub-web/e2e/run-phase3-e2e.mjs`：新增后端、Vite、Fake Provider 与 Playwright 编排；
- `frontend/information-hub-web/e2e/run-e2e.mjs`：隔离旧模拟 E2E 测试集；
- `frontend/information-hub-web/package.json`：新增 `test:e2e:phase3`；
- `frontend/information-hub-web/src/types/batch.ts`：对齐冻结 Batch 状态；
- `frontend/information-hub-web/src/views/BatchDetailView.vue`：修正完成/部分失败终态；
- `frontend/information-hub-web/src/views/BatchDetailView.spec.ts`：新增终态与 Actual Usage 回归；
- README、ROADMAP、CURRENT_STATUS 与本任务记录：同步当前完成度。

没有新增 Migration、业务表、公开 API 或后端业务逻辑；既有事务、Owner 隔离和幂等边界未改变。

## 9. 测试结果

环境：Java `21.0.11`、Node `24.18.0`、npm `11.16.0`、MySQL `8.4`，测试库经 `127.0.0.1:13306` SSH 隧道访问。

- `.\mvnw.cmd -DskipTests package`：`BUILD SUCCESS`；
- `.\mvnw.cmd test`：175 项，0 失败，0 错误，1 跳过；跳过项为未配置独立空库变量的 Migration 测试；
- `npm.cmd run typecheck`：通过；
- `npm.cmd test -- --run`：24 个文件、79 项，0 失败；
- `npm.cmd test -- --run src/views/BatchDetailView.spec.ts`：1 项，0 失败；
- `npm.cmd run test:e2e`：6 项，0 失败；
- `npm.cmd run build`：typecheck 与 Vite production build 通过；
- `npm.cmd run test:e2e:phase3`：1 项，0 失败。

最后一次全栈证据：

```json
{
  "singleEstimated": 2312,
  "singleActual": 150,
  "batchEstimated": 2312,
  "batchActual": 150,
  "failedActual": 18,
  "scheduledActual": 150,
  "baselineUserActualTotal": 1404,
  "runActualTotal": 468,
  "userActualTotal": 1872
}
```

Schedule 实际触发后进入 `COMPLETED`，Actual 为 150；临时 Schedule 已恢复为 disabled。幂等、overlap、misfire 规则由完整 Maven 回归中的 Schedule/Batch 测试覆盖。

尚未执行：

- 真实 Provider 受控 E2E：当前 Process/User/Machine 均未设置 `INFORMATION_HUB_AI_ENABLED/BASE_URL/API_KEY/MODEL/PREVIEW_HMAC_SECRET`；
- GitHub Actions `phase3-e2e` job：尚未 commit/push，远程工作流未触发；
- 独立空库 Migration 测试：未提供该测试要求的专用空库环境变量。

## 10. 遗留问题

- 必须由用户以环境变量提供真实 Provider 配置后，完成少量 Snapshot 的成功、失败、Estimate/Actual、Usage、Schedule 与泄露检查；验收结束后关闭 Schedule 并移除 Key。
- GitHub CI 通过前不能勾选 Fake Provider CI。
- 真实 Provider 验收和 GitHub CI 均通过前，不得把 TASK-032 或 Phase 3 标记完成。
- MySQL 8.4 继续出现 Flyway 最新已测试版本为 8.1 的提示；Migration 与回归均成功。

## 11. 完成确认

- [x] 当前 TASK 文档已更新
- [x] `docs/CURRENT_STATUS.md` 已更新
- [x] `git diff --check` 通过
- [ ] 用户已检查 `git diff`
- [ ] 用户确认测试结果
- [ ] 用户完成 commit / push