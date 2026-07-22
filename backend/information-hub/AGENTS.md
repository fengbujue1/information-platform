# Information Hub Backend 开发规则

## 一、模块定位

本模块是 Information Platform 的中央信息服务。

负责：

- Collector 接入；
- 请求校验；
- 幂等归档；
- 信息标准化；
- 数据查询；
- AI 任务；
- 推荐；
- 通知任务。

当前 Phase 只实现任务文档明确要求的功能。

## 二、技术栈

- Java 21
- Spring Boot 3.x
- Maven
- Maven Wrapper
- MySQL
- MyBatis-Plus
- Flyway

除非 ADR 和当前 TASK 明确批准，不引入：

- MongoDB
- Kafka
- RabbitMQ
- Elasticsearch
- Redis
- Kubernetes
- 微服务框架

## 三、包和依赖边界

推荐模块：
common
ingestion
information
job
analysis
recommendation
notification
user

依赖原则：

common 不依赖业务模块；
information 不依赖具体采集器；
job 可以依赖 information；
ingestion 负责协议适配和应用编排；
analysis 不修改原始信息；
recommendation 使用分析结果，不负责采集。

禁止出现循环依赖。

##四、分层规则

每个业务模块可以包含：

api
application
domain
infrastructure
query

职责：

api：Controller、请求和响应转换；
application：用例编排、事务边界；
domain：业务规则和领域模型；
infrastructure：数据库、外部服务；
query：查询模型和读接口。

Controller 不得：

直接调用 Mapper；
编写数据库事务；
实现复杂业务规则；
拼装大量领域逻辑。
##五、数据库规则

数据库设计说明：

../../docs/DATABASE_DESIGN.md

Flyway 目录：

src/main/resources/db/migration/

规则：

Flyway SQL 是数据库结构的最终事实来源。
不使用 Hibernate 或 MyBatis 自动建表。
已在共享环境执行的迁移文件不得修改。
结构变化必须新增迁移文件。
数据库文档与迁移 SQL 必须保持一致。
主表和业务扩展表的写入必须在同一事务中。
rawPayload 必须完整保留。
publishTime 未知时为 null。
firstSeenTime 和 lastSeenTime 由服务端维护。
幂等键为：
source + informationType + sourceItemId。
##六、信息接入规则

统一协议：

../../docs/contracts/information-envelope-v1.md

处理流程：

接收请求
→ 参数校验
→ 协议版本校验
→ 幂等查询
→ 新增或更新主信息
→ 保存业务扩展
→ 返回接入结果

重复数据不得直接产生重复主记录。

内容未变化时，不应重复创建 AI 任务。

##七、异常和响应
不向客户端暴露数据库堆栈。
参数错误使用明确的错误码。
来源数据问题和系统异常要区分。
日志不得输出 Token、Cookie 或完整敏感 payload。
不允许空的 catch 块。
不允许吞掉异常后返回成功。
##八、测试要求

每次修改必须运行与任务相关的测试。

默认命令：

Windows：

.\mvnw.cmd test

Linux/macOS：

./mvnw test

数据库功能至少覆盖：

正常插入；
重复幂等提交；
主表和扩展表事务回滚；
rawPayload 读写；
publishTime 为空；
无效外键；
参数校验失败。

测试失败时不得写“任务完成”。

##九、代码质量
使用明确命名，避免 Utils、Manager 承担过多职责。
不创建巨大 Service。
不通过静态变量保存运行状态。
时间类型优先使用 Java Time API。
枚举值集中定义，不在代码中散落魔法字符串。
DTO、Entity、Domain Model 不应无差别混用。
新增依赖前说明必要性。
##十、完成任务前

必须：

运行相关测试；
检查 git diff；
核对是否超出当前 TASK；
更新 TASK 实施记录；
更新 docs/CURRENT_STATUS.md；
汇报迁移脚本、测试命令和结果。