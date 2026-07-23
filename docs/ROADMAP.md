# Information Platform Roadmap

## Phase 0：项目初始化

状态：已完成。

- 创建 Monorepo。
- 导入 BOSS 采集器。
- 创建项目文档和 Codex 规则。
- 建立 Git 跨电脑同步流程。

## Phase 1：采集接入与归档

状态：进行中。

目标：

```text
BOSS Collector
→ InformationEnvelope V1
→ Information Hub
→ MySQL 当前版本与历史快照
→ Job Query API
```

任务：

1. [分析 BOSS 输出结构](tasks/TASK-001.md)
2. [确认 InformationEnvelope V1](tasks/TASK-002.md)
3. [创建 Spring Boot 后端](tasks/TASK-003.md)
4. [创建 Phase 1 数据库结构](tasks/TASK-004.md)
5. [实现信息接入 API](tasks/TASK-005.md)
6. [实现 BOSS 字段映射器](tasks/TASK-006.md)
7. [实现 Information Hub Client](tasks/TASK-007.md)
8. [实现本地 Outbox](tasks/TASK-008.md)
9. [实现职位查询 API](tasks/TASK-009.md)
10. [完成端到端验收](tasks/TASK-010.md)

完成标准：

- 原采集流程仍可运行；
- 本地 JSON/CSV 输出未被破坏；
- 后端不可用时数据不会丢失；
- 相同职位重复提交不会重复入库；
- 内容变化时保存历史快照；
- 临时缺失字段不会破坏已有数据；
- 可以通过 Job Query API 查询职位；
- 自动化测试通过。

## Phase 2：Web 浏览

状态：未开始。

- Vue 职位列表；
- 职位详情；
- 搜索和筛选；
- 原始 JSON 和版本历史查看。

## Phase 3：AI 分析

状态：未开始。

- 规则预过滤；
- AI 任务和 Worker；
- 职位质量、远程和摘要分析；
- Prompt、模型和 Token 记录。

## Phase 4：个性化推荐

状态：未开始。

- 用户画像；
- 内容质量分；
- 用户匹配分；
- 相似 JD 去重；
- 公司多样性；
- 每日 Top N。
