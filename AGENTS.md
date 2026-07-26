# Information Platform 开发说明

## 项目目标

本项目是一个通用的信息采集、分析、推荐和浏览平台。

当前第一阶段只处理 BOSS 直聘职位，但架构不能绑定招聘业务。
未来可能接入：

- 智联、猎聘等招聘来源
- 新闻
- 政府政策
- 房价
- 教育信息
- 其他公开信息源

## 当前目录

- `collectors/boss-zhipin-scraper`
  - 已经能够运行的 Python BOSS 采集器
  - 使用真实 Chrome 和 CDP 获取职位数据
  - 当前不要重写其采集核心
  - 优先通过新增适配层接入后端

- `backend/information-hub`
  - Spring Boot 中央信息服务
  - 负责接收、校验、去重、保存、查询和后续 AI 分析

- `frontend/information-hub-web`
  - 后续的 Vue 3 管理和浏览前端

## 第一阶段数据链路

BOSS 采集器
→ 转换为统一采集协议
→ POST 到 Information Hub
→ MySQL 保存
→ REST API 查询


## 核心设计原则

1. 采集器与主系统解耦。
2. 不把 Python 采集代码迁入 Java 项目。
3. 主系统不能依赖 BOSS 专有字段。
4. Job 只是 Information 的一种类型。
5. 原始数据必须保留，不能只保存标准化字段。
6. 所有写入接口必须支持幂等。
7. 当前使用模块化单体，不做微服务。
8. 当前不引入 Kafka、MongoDB、Elasticsearch、Kubernetes。
9. 当前使用 MySQL，并使用 JSON 字段保存原始 payload。
10. 不要修改已经验证可运行的 BOSS 采集核心，除非当前任务明确要求。

## 技术栈

后端：

- Java 21
- Spring Boot 3.x
- Maven
- MySQL
- MyBatis-Plus
- Flyway

采集器：

- 保持现有 Python 技术栈

前端：

- Vue 3
- TypeScript
- Element Plus

## Codex 工作规则

每次开始任务前：

1. 阅读本文件和 `docs/PROJECT_CONTEXT.md`
2. 检查当前代码和 Git 状态
3. 说明准备修改哪些模块
4. 只实现当前任务，不提前实现后续阶段
5. 完成后运行相关测试
6. 汇报修改文件、测试结果和遗留问题

Java 代码注释要求：

1. 新增的 PO、DTO、Domain、配置对象和其他数据实体，必须在每个字段或 record 组件上方添加简明中文注释，说明字段的业务含义、单位或取值范围。
2. 复杂方法必须在定义处添加中文注释，说明职责、关键约束或事务语义。
3. 方法内部涉及幂等、事务、数据合并、版本判断、安全校验等不易直接理解的逻辑调用，必须在调用处添加简明中文注释。
4. 注释用于解释业务意图和设计原因，不逐行复述代码，不保留与实现不一致的过期说明。

禁止：

- 在 Controller 中堆积业务逻辑
- 创建职责过大的 Service
- 未经确认大规模重写现有采集器
- 为未来需求提前引入复杂基础设施
- 把密码、Cookie、Token 或数据库密码提交到 Git
