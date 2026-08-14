# ADR-001：使用 Monorepo

状态：Accepted
日期：2026-07-23
决策人：项目负责人

## 背景

项目同时包含 Python Collector、Java Backend、Vue Frontend、部署配置和文档，并需要在两台电脑间同步 Codex 上下文。

## 决策

使用一个 Monorepo 管理 `collectors/`、`backend/`、`frontend/`、`deploy/` 和 `docs/`。

## 备选方案

- 多仓库：边界清晰，但跨仓库联调和上下文恢复复杂。
- Git submodule：保持独立历史，但个人开发的提交和同步容易出错。

## 理由

当前以个人开发、快速联调和统一文档为主，Monorepo 成本最低。

## 重新评估条件

模块由独立团队维护、权限隔离或完全独立发布时重新评估。
