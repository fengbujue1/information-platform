# Information Hub Backend 开发规则

## 一、模块定位

本模块是 Information Platform 的中央信息服务。

负责：

- Collector 接入；
- 请求校验；
- 幂等归档；
- 非破坏性合并；
- 信息快照；
- 数据查询；
- 后续 AI、推荐和通知任务。

当前只实现 TASK 明确要求的功能。

## 二、技术栈

- Java 21
- Spring Boot 3.x
- Maven / Maven Wrapper
- MySQL
- MyBatis-Plus
- Flyway

未经 ADR 和当前 TASK 批准，不引入 MongoDB、Kafka、RabbitMQ、Elasticsearch、Redis、Kubernetes 或微服务框架。

## 三、模块边界

推荐模块：

- `common`
- `ingestion`
- `information`
- `job`
- `analysis`
- `recommendation`
- `notification`
- `user`

依赖原则：

- `common` 不依赖业务模块。
- `information` 不依赖具体 Collector。
- `job` 可以依赖 `information`。
- `ingestion` 负责协议适配和应用编排。
- `analysis` 不修改原始信息。
- `recommendation` 使用分析结果，不负责采集。
- 禁止循环依赖。

## 四、分层规则

业务模块可以包含：

- `api`
- `application`
- `domain`
- `infrastructure`
- `query`

Controller 不得：

- 直接调用 Mapper；
- 编写事务；
- 实现复杂业务规则；
- 拼装大量领域逻辑。

## 五、数据库规则

设计文档：

`../../docs/DATABASE_DESIGN.md`

Flyway 目录：

`src/main/resources/db/migration/`

规则：

1. Flyway SQL 是数据库结构的最终事实来源。
2. 不使用 Hibernate 或 MyBatis 自动建表。
3. 已执行迁移不得修改。
4. 数据库文档与迁移 SQL 保持一致。
5. 主表、业务扩展表和必要快照写入同一事务。
6. rawPayload 完整保留。
7. publishTime 未知时为 null。
8. firstSeenTime 和 lastSeenTime 由服务端维护。
9. 幂等键是 `source + informationType + sourceItemId`。
10. 快照记录不可更新。

## 六、接入规则

协议：

`../../docs/contracts/information-envelope-v1.md`

处理流程：

```text
认证
→ 请求大小和参数校验
→ 协议版本校验
→ 幂等查询
→ 非破坏性合并
→ 计算 contentHash
→ 保存当前版本
→ 必要时创建快照
→ 返回接入结果
```

缺失、null、空字符串和默认空数组不得破坏已有有效数据。

## 七、状态规则

- `information_item.status`：平台内部生命周期。
- `job_information.job_status`：来源职位状态。

一次搜索未出现职位，不能直接标记 OFFLINE。

## 八、异常和安全

- 不向客户端暴露数据库堆栈。
- 参数错误使用稳定错误码。
- 日志不得输出 Token、Cookie 或完整敏感 payload。
- 不允许空 catch。
- 不允许吞掉异常后返回成功。
- 请求体上限按协议配置。

## 九、测试要求

Windows：

```powershell
.\mvnw.cmd test
```

Linux/macOS：

```bash
./mvnw test
```

数据库与接入至少覆盖：

- 正常插入；
- 重复幂等提交；
- 非破坏性更新；
- contentHash 未变化；
- contentHash 变化；
- 快照唯一约束；
- 事务回滚；
- rawPayload 读写；
- publishTime 为空；
- 参数和 Token 错误；
- 请求体过大。

测试失败时不得将任务标记为完成。

## 十、完成任务前

必须：

1. 运行相关测试。
2. 检查 `git diff`。
3. 核对是否超出当前 TASK。
4. 更新 TASK 实施记录。
5. 更新 `docs/CURRENT_STATUS.md`。
6. 汇报测试命令和实际结果。
