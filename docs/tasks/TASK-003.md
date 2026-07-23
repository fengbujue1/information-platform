# TASK-003：创建 Information Hub 后端项目

状态：TODO  
所属阶段：Phase 1  
优先级：P0

## 目标

创建可编译、测试和启动的 Maven Spring Boot 项目骨架。

## 本任务范围

- Java 21。
- Spring Boot 3.x。
- Maven Wrapper。
- 基础 package：common、ingestion、information、job。
- 基础配置和启动测试。
- 不连接真实生产数据库。

## 不在范围

- 不建业务表。
- 不实现接入 API。
- 不接 AI。
- 不创建前端。

## 验收标准

- [ ] `mvnw.cmd test` 通过
- [ ] 应用可启动
- [ ] 无提前实现的业务功能
