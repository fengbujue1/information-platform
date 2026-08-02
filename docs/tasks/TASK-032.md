# TASK-032：真实模型、定时任务与 Phase 3 E2E 验收

状态：TODO  
所属阶段：Phase 3  
优先级：P0  
负责人：User + Codex

## 1. 目标

使用小规模真实 JOB 数据和受控真实 AI Provider 验证 Phase 3 从登录到 Prompt、Preview、Batch、Usage、Schedule 的完整链路，并完成 Phase 3 文档收尾。

## 2. 前置依赖

- TASK-031 已完成。
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

- [ ] Java 测试通过
- [ ] Vue typecheck/test/build 通过
- [ ] 浏览器 E2E 通过
- [ ] Fake Provider CI 通过
- [ ] 小规模真实 Provider 分析成功
- [ ] Estimated vs Actual Token 有记录
- [ ] Actual Token 与 Provider Usage 一致
- [ ] 失败调用 Usage 规则验证
- [ ] Manual Batch limits 验证
- [ ] Schedule 默认关闭验证
- [ ] Schedule 触发/幂等/overlap/misfire 规则验证
- [ ] 用户 Usage 统计验证
- [ ] 无 rawPayload/API Key 泄露
- [ ] README/ROADMAP/CURRENT_STATUS 更新
- [ ] Phase 3 标记完成
- [ ] 下一阶段不自动启动

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
