# TASK-003：创建 Information Hub 后端项目

状态：DONE
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

- [x] `mvnw.cmd test` 通过
- [x] 应用可启动
- [x] 无提前实现的业务功能

## 实施记录

完成时间：2026-07-23

- 使用 Java 21 和 Spring Boot 3.5.16 创建 Maven 单体项目骨架。
- 生成 Maven Wrapper 3.3.4，并固定 Maven 版本为 3.9.16。
- 建立 `common`、`ingestion`、`information`、`job` package 边界。
- 增加基础 `application.yml` 和 Spring Boot 上下文启动测试。
- 未创建数据库表、接入 API、业务服务或 Collector 集成代码。

## 验证结果

- `.\mvnw.cmd --version`：Maven 3.9.16，Java 21.0.11。
- `.\mvnw.cmd test`：通过，1 个测试，0 失败，0 错误。
- `.\mvnw.cmd package`：通过，生成可执行 Spring Boot JAR。
- 可执行 JAR 启动验证：通过，应用在随机端口启动后正常停止。
