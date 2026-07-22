# Information Platform Roadmap

## Phase 0：项目初始化

状态：已完成

- 创建 Monorepo
- 导入 BOSS 采集器
- 创建项目文档
- 建立 Git 同步流程

完成标准：

- 两台电脑均可拉取并继续开发
- 迁移后的采集器仍可正常运行

## Phase 1：采集接入

状态：进行中

- 创建 Spring Boot 后端
- 创建数据库表
- 实现接入 API
- 实现幂等保存
- 改造 BOSS 采集器适配层
- 实现职位查询 API

完成标准：

BOSS Collector
→ Information Hub
→ MySQL
→ 查询 API

## Phase 2：Web 浏览

状态：未开始

- Vue 职位列表
- 职位详情
- 搜索筛选

## Phase 3：AI 分析

状态：未开始

- AI 任务
- AI Worker
- 职位质量分析
- 职位摘要

## Phase 4：推荐和推送

状态：未开始

- 用户偏好
- 推荐评分
- Top N
- 站内通知