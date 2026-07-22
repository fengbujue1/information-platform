# Information Platform 项目背景

## 项目目标

建设通用的信息采集、归档、分析、推荐和浏览平台。

## 当前基础

已经有可运行的 BOSS Python 采集器。

## 核心流程

Collector
→ Information Hub
→ MySQL
→ AI Analysis
→ Recommendation
→ Web / Notification

## 未来信息类型

- JOB
- NEWS
- GOVERNMENT
- HOUSE_PRICE
- EDUCATION

## 长期原则

- 采集器与后端解耦
- Job 只是 Information 的一种类型
- 原始数据必须保留
- AI 结果不能覆盖原始数据
- 支持未来增加新采集器

## 当前技术方向

- Java 21
- Spring Boot
- Maven
- MySQL
- Vue 3
- Python Collector
- Docker Compose

## 当前暂不引入

- 微服务
- Kafka
- MongoDB
- Elasticsearch
- Kubernetes