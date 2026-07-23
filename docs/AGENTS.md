# Documentation 开发规则

## 一、文档职责

`docs/` 保存：

- 项目背景；
- 架构；
- 路线图；
- 当前状态；
- 数据库设计；
- 接口协议；
- ADR；
- TASK。

## 二、事实优先级

1. 实际代码和测试结果。
2. Flyway SQL 和正式协议。
3. Accepted ADR。
4. 架构与数据库文档。
5. CURRENT_STATUS。

文档与代码不一致时必须指出，不得静默编造。

## 三、维护规则

- PROJECT_CONTEXT 描述背景与长期目标。
- ROADMAP 描述阶段。
- CURRENT_STATUS 描述真实进度。
- TASK 描述单个可验收任务。
- ADR 记录重要决策。
- DATABASE_DESIGN 与 Flyway 保持一致。
- 协议必须包含 schemaVersion、验证规则和兼容性。
- 未完成内容不得写成已完成。
- 测试结果必须来自实际执行。

## 四、Markdown 格式

- 标题井号后必须有空格。
- 列表每项独立一行。
- 代码块前后保留空行。
- 不把整份文件压缩成一行。
- 不在 Markdown 中使用可能被解释成 HTML 标签的占位符。
