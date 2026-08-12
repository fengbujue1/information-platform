# Phase 5 Official Planning Package

状态：Accepted Planning Baseline

阶段：Phase 5 — Product Refinement & Stabilization

基线：Phase 4 已完成

规划日期：2026-08-11

## 1. 使用方式

本包是 Phase 5 的正式规划基线。

Phase 5 与 Phase 3 / Phase 4 的主要区别是：**不预先冻结完整 TASK 列表，而采用 Rolling / Just-in-Time Task Planning。**

从 Phase 5 启动开始：

- Phase 5 的阶段目标、边界、任务规则按本包执行；
- TASK 编号从 `TASK-045` 开始；
- 不预创建一批未来 TASK；
- 实际发现并确认一个独立问题后，再创建下一个 TASK；
- 同一问题在实施、人工验证和多轮调整中继续维护原 TASK；
- 只有出现可独立描述、可独立验收的问题时才创建新 TASK；
- 一个 Codex 实施上下文只处理一个明确的 Active TASK；
- 后端改动继续遵守 `docs/LOGGING_CONVENTIONS.md`；
- 数据库、Contract、Architecture、ADR 只在具体 TASK 确实需要时更新，不为 Phase 5 预先创建空设计文档。

## 2. Phase 5 正式目标

Phase 5 是 Phase 4 完成后的产品打磨、缺陷修复、行为优化和稳定性加固阶段。

主要处理：

- 实际使用中发现的前端 UI / UX 问题；
- 后端 API 和业务行为不合理或不方便的地方；
- 前后端集成问题；
- 测试、CI、运行可靠性问题；
- 安全、日志、可观测性和生产准备问题；
- 已有功能的小范围业务调整；
- 实际使用后确认需要的重构。

Phase 5 不以新增大业务域为主要目标。

## 3. Rolling Task Model

标准流程：

```text
实际使用 / 测试发现问题
        ↓
与 ChatGPT / Codex 分析
        ↓
确认 Problem + Expected Behavior
        ↓
判断是否属于已有 TASK
   ┌────┴────┐
   │         │
  YES       NO
   │         │
原 TASK     创建下一个 TASK
Adjustment   例如 TASK-045
   │         │
   └────┬────┘
        ↓
Codex 实施
        ↓
自动测试
        ↓
用户实际验证
        ↓
有问题 → 原 TASK Adjustment → 再实施
        ↓
最终验收
        ↓
TASK = DONE
```

## 4. 本包文件

### 根目录

- `PHASE5_PACKAGE_MANIFEST.md`
  - 本文件；
  - Phase 5 正式入口。

### docs

- `docs/PHASE5_SCOPE.md`
  - 阶段目标、范围、边界和完成条件。

- `docs/PHASE5_TASK_MODEL.md`
  - Rolling Task 的编号、状态、拆分和多轮调整规则。

- `docs/PHASE5_TASK_INDEX.md`
  - Phase 5 动态任务索引；
  - 初始 Next Task Number 为 `TASK-045`。

- `docs/PHASE5_TASK_TEMPLATE.md`
  - 新 TASK 的标准模板。

- `docs/CODEX_PHASE5_WORKFLOW.md`
  - Codex 在 Phase 5 中必须遵守的正式工作流。

- `docs/CODEX_PHASE5_INTERACTION_GUIDE.md`
  - 面向实际使用的 Codex 场景化交互指南；
  - 包含可直接复制的提示词。

- `docs/PHASE5_EXISTING_DOC_CHANGES.md`
  - Phase 5 启动和日常实施时对现有文档的同步规则。

- `docs/PHASE5_COMPLETION_TEMPLATE.md`
  - Phase 5 最终收尾时使用的完成总结模板。

## 5. 不创建的 Phase 5 启动文档

Phase 5 启动时**不创建**：

- `PHASE5_ARCHITECTURE_DRAFT.md`；
- `PHASE5_DATA_MODEL_DRAFT.md`；
- `DATABASE_DESIGN_PHASE5_DRAFT.md`；
- 预先编号的 `TASK-045`～`TASK-0XX` 空任务；
- 为未知需求提前准备的 Contract / ADR。

原因：

Phase 5 的具体需求来自实际使用反馈，无法在阶段开始时可靠冻结。

如果某个 TASK 真正改变架构、数据库或外部 API，则该 TASK 负责：

- 更新 `ARCHITECTURE.md`；
- 更新 `DATABASE_DESIGN.md`；
- 新增 Flyway Migration；
- 新建或演进 Contract；
- 必要时新增 ADR。

## 6. TASK 编号

Phase 4 最终任务为 `TASK-044`。

Phase 5 从：

```text
TASK-045
```

开始。

默认顺序：

```text
TASK-045
TASK-046
TASK-047
...
```

仅在已经存在后续编号、又必须插入紧密关联的纠偏任务时，才允许使用：

```text
TASK-045A
```

正常的新问题不要主动使用 A/B 后缀。

## 7. Phase 5 核心原则

1. 冻结问题和预期行为，不提前冻结未经验证的实现。
2. 同一用户问题的多轮调整优先留在同一 TASK。
3. 新问题必须能够独立描述、独立验收，才创建新 TASK。
4. 不因“顺手”扩大当前 TASK。
5. 不为未来可能需求提前引入复杂基础设施。
6. 代码和测试事实高于规划文档。
7. 测试结果必须来自实际执行。
8. 后端业务日志不得泄露秘密、Prompt 正文、完整 payload 或敏感凭据。
9. 旧 Flyway Migration 不修改。
10. Codex 完成实施后可自动 `git add` 当前 TASK 的变更，但不得自动 commit / push，除非用户明确要求。

## 8. Phase 5 Completion

Phase 5 不要求消灭所有未来可能发现的问题。

满足以下条件后可进行收尾：

- 主要用户流程经过实际使用后稳定；
- 没有已知 P0 / P1 产品或可靠性问题；
- Active TASK 全部完成或明确 Deferred；
- 核心后端、前端、Browser / Full-stack 测试保持可用；
- 生产部署前必须解决的问题已经完成；
- 剩余小问题可以安全进入后续 Backlog；
- `PHASE5_COMPLETION.md` 根据实际完成的 TASK 生成。

Phase 5 的最终 TASK 数量在阶段开始时不预设。
