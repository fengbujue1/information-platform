# TASK-031：实现 Phase 3 Web

状态：TODO  
所属阶段：Phase 3  
优先级：P0  
负责人：User + Codex

## 1. 目标

在现有 Vue 3 Web 中实现登录、Prompt、手动分析、Batch、Schedule、Analysis Result 和 Token Usage 页面，形成可实际使用的 Phase 3 产品界面。

## 2. 前置依赖

- TASK-030 已完成。
- 必须阅读 `docs/PHASE3_SCOPE.md` 和本任务相关 Contract / ADR。
- 开始前执行 `git status`、`git log --oneline -8`、`git diff --check`。

## 3. 本任务范围

建议实现：
- `/login`
- `/ai/prompts`
- `/ai/analyze`
- `/ai/batches`
- `/ai/batches/:id`
- `/ai/schedules`
- `/ai/usage`
- 职位详情中的当前用户 Analysis 入口。

页面功能：
- Login / Logout / Me。
- Prompt Profile CRUD。
- Prompt Version 历史。
- Manual Preview。
- 显示 total/eligible/already/pending。
- Estimated Token。
- Confirm Batch。
- Batch 进度和 Item 结果。
- Analysis Result。
- 今日/月度/累计 Actual Token。
- Estimated vs Actual。
- Schedule 开关，默认关闭。
- Schedule 时间、timezone、windowDays、limits。
- Test Schedule Config Preview。
- 上次/下次执行。
- 错误、Loading、Empty、401/403。
- CSRF integration。
- TypeScript 强类型。

## 4. 不在本任务范围

- 不在浏览器保存 AI API Key。
- 不允许前端直接调用 Provider。
- 不实现充值/套餐。
- 不实现推荐 Top N。
- 不实现通知。
- 不引入新 UI 框架。

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

- [ ] 登录态工作正常
- [ ] Prompt Version UI 不覆盖历史
- [ ] Preview 不产生 Actual Token
- [ ] 用户可确认并创建 Batch
- [ ] Batch 可查看进度
- [ ] Analysis Result 可读
- [ ] 今日/月度/累计 Token 可见
- [ ] Schedule 默认关闭
- [ ] 默认时间显示 02:00
- [ ] Schedule 测试 Preview 可用
- [ ] 401/403 正确处理
- [ ] 无秘密进入浏览器
- [ ] typecheck/test/build 通过
- [ ] CURRENT_STATUS 指向 TASK-032

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
