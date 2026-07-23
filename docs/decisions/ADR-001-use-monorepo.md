# 使用 Monorepo 管理 Information Platform

状态：Proposed  
日期：2026-07-23  
决策人：项目负责人

## 1. 背景

该决策用于约束 Information Platform 当前阶段的架构实现。

## 2. 决策

使用一个 Monorepo 管理 collectors、backend、frontend、deploy 和 docs。

## 3. 决策理由

当前优先目标是以最低复杂度跑通采集、接入、归档和查询闭环。

## 4. 正面影响

- 降低开发与部署复杂度。
- 方便 Codex 在一个仓库中理解完整上下文。

## 5. 负面影响和代价

- 后续规模扩大后可能需要重新评估。

## 6. 重新评估条件

当数据规模、团队规模或独立扩容需求明显增加时重新评估。
