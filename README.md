# Information Platform

通用的信息采集、归档、分析、推荐和浏览平台。

## 当前状态

当前处于 Phase 1：BOSS 职位数据接入。

## 核心链路

BOSS Collector
→ Information Hub
→ MySQL
→ Query API

## 仓库结构

- collectors：独立采集器
- backend：Spring Boot 信息中枢
- frontend：Vue 信息浏览前端
- deploy：Docker Compose 部署配置
- docs：架构、协议、任务与决策文档

## 当前技术栈

- Java 21
- Spring Boot
- Maven
- MySQL
- Python
- Vue 3
- Docker Compose

## 文档入口

- 项目背景：docs/PROJECT_CONTEXT.md
- 架构：docs/ARCHITECTURE.md
- 路线图：docs/ROADMAP.md
- 当前状态：docs/CURRENT_STATUS.md
- Codex 开发规则：AGENTS.md

## 当前限制

本项目当前不引入微服务、Kafka、MongoDB、Elasticsearch
或 Kubernetes。

## 合规说明

采集器仅用于个人学习、技术研究和用户有权访问的数据处理。
使用者应遵守目标网站规则、适用法律和合理访问频率。