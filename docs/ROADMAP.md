# Information Platform Roadmap

## Phase 0：项目初始化

状态：已完成。

- 创建 Monorepo。
- 导入 BOSS 采集器。
- 创建项目文档和 Codex 规则。
- 建立 Git 跨电脑同步流程。
- 修正运行结果和敏感本地文件忽略规则。

## Phase 1：采集接入

状态：进行中。

目标：

```text
BOSS Collector
→ InformationEnvelope V1
→ Information Hub
→ MySQL
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
- 可以通过 Job Query API 查询采集职位；
- 相关自动化测试通过。

## Phase 2：Web 浏览

状态：未开始。

- 创建 Vue 3 项目；
- 职位列表；
- 职位详情；
- 搜索和筛选；
- 原始 JSON 查看。

## Phase 3：AI 分析

状态：未开始。

- 规则预过滤；
- AI 任务表和 Worker；
- JobQualityAnalyzer；
- JobRemoteAnalyzer；
- JobSummaryAnalyzer；
- Prompt、模型和 Token 记录。

## Phase 4：个性化推荐

状态：未开始。

- 用户画像；
- 简历和技能偏好；
- 内容质量分；
- 用户匹配分；
- 相似 JD 去重；
- 公司多样性；
- 每日 Top N。

## Phase 5：通知

状态：未开始。

实施顺序：

站内通知 → 邮件 → Webhook → 微信 → 短信。

## Phase 6：第二类信息源

状态：未开始。

优先考虑 RSS 新闻，用来验证通用 Information 模型是否真正成立。

## Phase 7：按瓶颈分布式演进

状态：未开始。

只有在真实瓶颈出现后，才评估 Kafka、搜索引擎、向量库、MinIO、Hudi 和微服务。
