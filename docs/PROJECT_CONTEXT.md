# Information Platform 项目背景

## 项目目标

建设一个通用的信息采集、归档、分析、推荐和浏览平台。

系统当前从 BOSS 职位信息开始，但最终模型不能绑定招聘业务。未来可能接入新闻、政府政策、房价、教育、行业报告和其他公开信息源。

## 当前基础

仓库中已有可运行的 Python BOSS 采集器：

`collectors/boss-zhipin-scraper`

该采集器使用真实 Chrome 和 Chrome DevTools Protocol 复用登录状态，通过 BOSS 搜索接口获取结构化职位列表，并抓取职位详情。

## 长期核心流程

Collector  
→ Information Hub  
→ 归档与标准化  
→ 规则过滤  
→ AI 分析  
→ 个性化推荐  
→ Web / Notification

## 长期设计原则

1. 采集器与 Information Hub 解耦。
2. 采集器不直接连接主数据库。
3. Job 只是 Information 的一种类型。
4. 原始数据必须保留。
5. 标准化字段不能取代原始数据。
6. AI 分析结果不能覆盖原始信息。
7. 信息质量分和用户匹配分必须分开。
8. 新数据源应通过统一协议接入，不应修改平台核心流程。
9. 当前先构建模块化单体，出现真实瓶颈后再分布式拆分。

## 当前技术方向

- Collector：Python
- Backend：Java 21、Spring Boot、Maven
- Persistence：MySQL 8、Flyway、MyBatis-Plus
- Frontend：Vue 3、TypeScript、Vite、Element Plus
- Deployment：Docker Compose
- AI：后续使用 OpenAI 兼容接口，框架在 AI Phase 再决定

## 当前暂不引入

- 微服务
- Kafka / RabbitMQ
- MongoDB
- Elasticsearch
- 向量数据库
- Kubernetes
- 短信和微信推送
- 复杂用户权限系统
