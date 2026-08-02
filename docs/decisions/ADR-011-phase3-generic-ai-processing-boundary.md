# ADR-011：Phase 3 建设通用 AI Processing Foundation，JOB 作为首个实现

状态：Proposed  
日期：2026-08-02

## 背景

Information Platform 的长期目标不是招聘网站，而是通用信息平台。当前只有 JOB，但未来可能有 NEWS、POLICY、HOUSE、EDUCATION 等。

如果 Phase 3 直接建设 `job_prompt`、`job_analysis`、`job_batch`，后续接入其他信息类型会产生结构性重构。

另一方面，当前没有真实 NEWS/HOUSE/POLICY 数据，不能为了未来需求提前做完整跨域框架。

## 决策

采用：

> 通用 AI 核心 + JOB 唯一真实领域实现。

通用概念：

- Prompt Profile
- Prompt Version
- Analysis Definition
- Information Analysis
- Model Invocation
- Analysis Batch
- Analysis Schedule
- Token Usage

JOB 专属：

- Job Input Projection
- Job Candidate Resolver
- Job User Relevance V1

第一版：

```text
informationType = JOB
purpose = USER_RELEVANCE
definition = JOB_USER_RELEVANCE_V1
```

## Analysis Purpose

长期区分：

- `ENRICHMENT`：信息本身结构化增强；
- `USER_RELEVANCE`：根据用户 Prompt 判断相关性；
- `PROMPT_RETRIEVAL`：未来大规模 Prompt 检索；
- `RECOMMENDATION`：未来主动推荐。

Phase 3 只实现 USER_RELEVANCE。

## 正面影响

- AI 核心不会绑定招聘。
- 后续新增信息类型不需要推翻 Prompt/Batch/Usage/Schedule。
- 当前仍然只实现一个真实业务，不会过度建设。
- 推荐和检索边界更清晰。

## 负面影响

- Phase 3 需要定义 Analysis Definition 和 Candidate Resolver 两个扩展点。
- 初期代码比纯 `JobAnalysisService` 多一层抽象。

## 不采用

### 方案 A：全部按 JOB 命名

短期简单，但后续重构成本高。

### 方案 B：现在实现多个信息类型

当前没有真实需求和数据，属于过度设计。

### 方案 C：现在引入向量数据库/RAG

当前目标是受控时间窗口内的 USER_RELEVANCE，不需要大规模语义检索。

## 重新评估

以下情况新增 ADR：

- 开始真实 NEWS/POLICY/HOUSE AI；
- 候选规模大到 MySQL 窗口筛选不够；
- 需要 Prompt-driven Retrieval；
- 需要 Embedding / Vector Search；
- 需要多 Worker / 消息队列。
