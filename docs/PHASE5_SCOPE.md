# Phase 5 Scope — Product Refinement & Stabilization

状态：Accepted

阶段：Phase 5

基线：Phase 4 已完成

## 1. 阶段定位

Phase 5 是 Phase 4 Personalized Recommendation MVP 完成后的滚动优化阶段。

本阶段不再像 Phase 3 / Phase 4 一样提前拆出完整功能路线和固定 TASK 列表。

Phase 5 的需求主要来自：

- 实际页面使用；
- 前后端联调；
- 真实数据运行；
- 自动化测试；
- 人工验收；
- 安全与可靠性审查；
- 生产准备过程。

因此 Phase 5 采用 Rolling / Just-in-Time Task Planning。

## 2. 目标

Phase 5 的目标是：

- 改善已有 Web 页面和用户操作体验；
- 修复真实使用中发现的缺陷；
- 优化不合理的后端 API 或业务行为；
- 提高已有流程的一致性、可靠性和可维护性；
- 补齐测试、CI、安全、运行和生产准备缺口；
- 在不破坏既有阶段边界的前提下完成必要的小范围重构；
- 使项目达到进入生产部署准备阶段的质量水平。

## 3. In Scope

### 3.1 Frontend

包括但不限于：

- 页面布局与视觉层级；
- 表单交互；
- loading / empty / error / success 状态；
- 按钮、提示、确认和撤销行为；
- 分页、筛选、排序和 URL 状态；
- Recommendation 页面；
- Analysis 页面；
- Job 页面；
- 登录与 Session 相关体验；
- 组件拆分和局部可维护性重构；
- 响应式和不同窗口尺寸问题；
- 前端性能和不必要请求；
- 前后端错误信息展示。

### 3.2 Backend

包括但不限于：

- API 行为优化；
- 校验逻辑；
- 错误码和异常处理；
- Owner / Security 边界加固；
- Worker 可靠性；
- Analysis / Recommendation 生命周期优化；
- 查询与事务优化；
- 日志与可观测性；
- CI / E2E；
- 生产配置 fail-safe / fail-fast；
- 实际使用后确认必要的小范围代码重构。

### 3.3 Integration

包括：

- frontend ↔ backend；
- collector ↔ information-hub；
- AI Provider 集成；
- Session / CSRF；
- MySQL / Flyway；
- Worker / Scheduler；
- Browser E2E；
- Full-stack E2E。

### 3.4 Documentation

每个 TASK 可按实际影响更新：

- `CURRENT_STATUS.md`；
- `ARCHITECTURE.md`；
- `DATABASE_DESIGN.md`；
- Contract；
- ADR；
- AGENTS；
- README / ROADMAP（仅在阶段级事实发生变化时）。

## 4. Out of Scope by Default

以下内容默认不应作为普通 Phase 5 小任务直接吸收：

- 新增完整 Information Type，例如 NEWS / HOUSE / POLICY；
- 大型新业务模块；
- 推荐系统整体替换；
- Microservice 化；
- 无明确当前需求的 Kafka / Redis / Elasticsearch / Vector DB；
- RAG / Agent / Learning-to-Rank 等大能力；
- 大规模数据库重构；
- 与当前问题无关的全仓库重写；
- 公共注册、复杂 RBAC、多租户等独立产品能力；
- 为“以后可能有用”提前建设的复杂基础设施。

如果实际问题必须引入上述能力，应先判断是否应该升级成独立后续 Phase。

## 5. 需求确认原则

TASK 在实现前至少需要确认：

1. **Problem**
   - 当前真实发生了什么；
   - 如何复现；
   - 为什么当前行为有问题。

2. **Expected Behavior**
   - 修改后用户或系统应该表现为什么。

3. **Scope**
   - 当前 TASK 必须改什么；
   - 明确不改什么。

实现方案可以在调查后演进，不要求第一次讨论就冻结。

## 6. TASK 创建原则

以下情况创建新 TASK：

- 问题可以独立描述；
- 可以独立验收；
- 与当前 Active TASK 的核心目标不同；
- 解决它需要不同的业务决策或明显不同的改动范围。

以下情况通常继续当前 TASK：

- 第一版实现后 UI 细节不满意；
- 同一操作的 loading / error / undo 需要调整；
- 当前 TASK 的测试发现同一根因下的遗漏；
- 实施方案需要小范围重做；
- 人工验收发现当前问题仍未完全解决。

## 7. 优先级

- `P0`：数据安全、严重安全漏洞、核心链路不可用、数据损坏风险。
- `P1`：重要功能错误、生产阻塞、明显可靠性问题。
- `P2`：一般产品问题、体验问题、常规优化。
- `P3`：低风险体验、代码整洁、非紧急维护。

优先级不决定 TASK 编号。

## 8. 实施原则

- 先 targeted test，再执行必要回归；
- 不为了“测试全面”无意义反复运行耗时命令；
- 外部 AI HTTP 调用继续避免持有数据库事务；
- 不修改历史 Flyway；
- 不通过前端隐藏替代后端业务规则；
- 不信任客户端传入的 userId / owner；
- 不把秘密写入 Git、前端或日志；
- 不为了一个局部问题引入大规模基础设施；
- 重构必须服务于当前 TASK，而不是无限扩大。

## 9. 阶段退出条件

Phase 5 可结束的判断标准：

- 主流程在真实使用中稳定；
- P0 / P1 已清零或存在明确、可接受的 Deferred 决策；
- Active TASK 全部 DONE / DEFERRED；
- 核心测试通过；
- 数据库和架构事实文档与代码一致；
- 已有 Phase 1～4 的核心边界没有被无记录破坏；
- 生产部署前置问题已经达到可接受状态；
- 根据实际 TASK 输出 Phase 5 Completion Summary。

## 10. Phase 5 之后

Phase 5 结束后，根据项目真实状态再决定：

- Production Deployment；
- Retrieval / Semantic Search；
- Notification；
- 新 Information Type；
- Recommendation V2；
- 或新的产品功能阶段。

不在 Phase 5 启动时提前冻结下一阶段。
