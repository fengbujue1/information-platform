# TASK-030：实现每日 Analysis Schedule

状态：TODO  
所属阶段：Phase 3  
优先级：P0  
负责人：User + Codex

## 1. 目标

实现页面可配置的每日自动 AI 分析调度。默认关闭、默认用户本地时间 02:00，并严格复用 Manual Batch 的候选、幂等、Budget 和 Worker。

## 2. 前置依赖

- TASK-029 已完成。
- 必须阅读 `docs/PHASE3_SCOPE.md` 和本任务相关 Contract / ADR。
- 开始前执行 `git status`、`git log --oneline -8`、`git diff --check`。

## 3. 本任务范围

- Schedule CRUD。
- enabled default false。
- localTime default 02:00。
- IANA timezone。
- windowDays/maxCandidates/maxEstimatedTokens。
- Prompt Profile 绑定。
- nextRunAt 计算。
- 后台 due schedule dispatcher。
- 运行时 Resolve Active Prompt Version。
- Scheduled Batch 创建。
- triggerType=SCHEDULED。
- scheduleId + scheduledFor 幂等。
- 同 Schedule overlap guard。
- misfire=SKIP。
- 无候选 No-op。
- Schedule list/detail。
- 上次/下次运行信息。
- Test Current Config 复用 Preview。
- DST / timezone 基础测试。

## 4. 不在本任务范围

- 不开放任意 Cron。
- 不支持小时/分钟级重复。
- 不补跑停机期间所有遗漏。
- 不引入 Quartz/XXL-JOB。
- 不绕过 Token Budget。
- 不直接从 Scheduler 调 Provider。

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

- [ ] 新 Schedule 默认关闭
- [ ] 默认本地时间 02:00
- [ ] 用户主动开启后才运行
- [ ] timezone 正确
- [ ] Schedule 使用 Active Prompt Version
- [ ] Batch 创建后 Prompt Version 冻结
- [ ] 与 Manual 共用 Batch Worker
- [ ] 同一 scheduledFor 不重复创建
- [ ] overlap 被跳过
- [ ] misfire 默认不补跑
- [ ] 无候选不调用 AI
- [ ] limits/budget 完全生效
- [ ] 测试通过
- [ ] CURRENT_STATUS 指向 TASK-031

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

待填写。

## 9. 测试结果

必须填写实际执行命令和结果，禁止编造。

## 10. 遗留问题

待填写。

## 11. 完成确认

- [ ] 当前 TASK 文档已更新
- [ ] `docs/CURRENT_STATUS.md` 已更新
- [ ] `git diff --check` 通过
- [ ] 用户已检查 `git diff`
- [ ] 用户确认测试结果
- [ ] 用户完成 commit / push
