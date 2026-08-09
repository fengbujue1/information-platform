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

每个后端 TASK 开始前必须阅读：

- 本文件；
- `../../docs/LOGGING_CONVENTIONS.md`；
- 当前 TASK 和其引用的 ADR / Contract；
- `../../docs/CURRENT_STATUS.md`。

---

## 二、技术栈

- Java 21
- Spring Boot 3.x
- Maven
- Maven Wrapper
  - 后端构建、测试、打包必须优先使用仓库自带 Maven Wrapper；
  - Maven 版本以 `.mvn/wrapper/maven-wrapper.properties` 为准；
  - 当前 Wrapper 固定使用 Maven 3.9.16。
- MySQL
- MyBatis-Plus
- Flyway

未经 ADR 和当前 TASK 批准，不引入 MongoDB、Kafka、RabbitMQ、Elasticsearch、Redis、Kubernetes 或微服务框架。

---

## 三、Java / Maven 执行环境规则

### 3.1 Java 版本

后端必须使用 JDK 21。

每个 Codex 任务第一次需要执行后端 Maven 命令之前，只进行一次 Java / Maven 基础环境检查。

Windows：

```powershell
java -version
.\mvnw.cmd -version
```

Linux/macOS：

```bash
java -version
./mvnw -version
```

检查结果必须满足：

- Java 主版本为 21；
- Maven Wrapper 能正常启动；
- Maven Wrapper 使用项目指定的 Maven 版本。

如果当前 Java 不是 JDK 21，应将其判断为本机执行环境问题。

不得为了绕过本机 JDK 配置问题而：

- 修改 `pom.xml` 中的 Java 版本；
- 降低项目 Java 版本要求；
- 删除或放宽 Maven Enforcer 规则；
- 修改业务代码；
- 修改项目架构。

不得把开发者机器上的 JDK 绝对安装路径写入仓库，例如：

```text
D:\jdk\...
C:\Program Files\Java\...
```

JDK 的实际安装位置由开发机的 `JAVA_HOME` 和 `PATH` 管理。

### 3.2 Maven Wrapper

正常的项目构建、测试和打包必须使用仓库 Maven Wrapper。

Windows：

```powershell
.\mvnw.cmd
```

Linux/macOS：

```bash
./mvnw
```

不得使用全局安装的：

```text
mvn
```

替代 Maven Wrapper 执行正常的项目构建、测试或打包。

只有当前任务明确要求排查本机 Maven 环境时，才允许使用：

```powershell
mvn -version
```

Maven 版本以：

```text
.mvn/wrapper/maven-wrapper.properties
```

中的 `distributionUrl` 为准。

当前项目 Maven Wrapper 使用 Maven 3.9.16。

除非当前 TASK 明确要求升级 Maven，否则不得为了适配本机 Maven 环境而修改：

```text
.mvn/wrapper/maven-wrapper.properties
```

### 3.3 环境检查只执行一次

如果当前任务第一次环境检查已经成功，同一个任务中不得无理由重复检查：

- `java -version`
- `JAVA_HOME`
- `PATH`
- `MAVEN_HOME`
- `mvn -version`
- `mvnw -version`
- 本机 JDK 安装目录
- 本机 Maven 安装目录

不得因为同一个已知环境问题反复尝试不同 JDK、Maven、Shell 或执行环境组合。

### 3.4 环境异常必须 Fail Fast

如果发现：

- 当前 Java 不是 JDK 21；
- Maven Wrapper 无法启动；
- Maven Wrapper distribution 不可访问；
- 必要的本地 Maven 依赖缓存不可访问；
- 受限执行环境无法联网获取 Wrapper 或依赖；

应首先判断是否属于执行环境问题。

如果确认属于执行环境问题：

1. 停止重复构建；
2. 不修改业务代码；
3. 不修改 `pom.xml` 绕过问题；
4. 不降低 Java/Maven 要求；
5. 不反复尝试不同 JDK/Maven 组合；
6. 明确报告发现的环境问题和已经执行过的检查。

禁止出现类似的重复试错流程：

```text
构建失败
→ 换 Maven
→ 再构建
→ 换 JDK
→ 再构建
→ 检查 PATH
→ 再构建
→ 再换执行环境
```

相同根因导致的失败命令原则上不得重复执行。

### 3.5 Sandbox 与本机执行环境

如果 Maven Wrapper：

- 在正常授权的本机执行环境中能够成功运行；
- 但在受限 sandbox 中因为 Maven Wrapper distribution、用户 `.m2` 缓存或网络不可访问而失败；

则：

- 不要在 sandbox 中反复重试；
- 不要反复尝试联网下载 Maven；
- 不要修改 Wrapper 配置绕过 sandbox；
- 应在允许访问本机现有 Maven Wrapper 和 Maven 缓存的执行环境中完成必要验证；
- 在任务总结中说明 sandbox 环境限制。

如果同一套代码已经在允许的执行环境中完成 TASK 要求的测试或构建验证，不得仅为了验证 sandbox 而重复执行相同完整测试。

### 3.6 本机环境不得进入仓库

下列内容属于开发机配置，不属于项目配置：

- `JAVA_HOME`
- `MAVEN_HOME`
- JDK 绝对安装目录
- Maven 绝对安装目录
- 用户 `.m2` 的绝对路径

不得将上述本机路径写入：

- Java 代码；
- `pom.xml`；
- `application*.yml`；
- Maven Wrapper 配置；
- Git 跟踪的脚本；
- TASK 实现代码。

项目只规定：

- JDK 21；
- Maven Wrapper；
- Wrapper 固定的 Maven 版本。

---

## 四、模块边界

推荐模块：

- `common`
- `ingestion`
- `information`
- `job`
- `analysis`
- `recommendation`
- `notification`
- `identity`

依赖原则：

- `common` 不依赖业务模块。
- `information` 不依赖具体 Collector。
- `job` 可以依赖 `information`。
- `ingestion` 负责协议适配和应用编排。
- `analysis` 不修改原始信息。
- `identity` 提供 Session 当前用户和 Owner 边界，不承载 Prompt/Analysis 业务。
- `recommendation` 使用分析结果，不负责采集。
- 禁止循环依赖。

---

## 五、分层规则

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

---

## 六、Java 代码注释规则

1. 新增的 PO、DTO、Domain、配置对象和其他数据实体，每个字段或 record 组件上方必须有简明中文注释。
2. 字段注释应说明业务含义；存在单位、时间语义、状态取值或数据来源时一并说明。
3. 复杂方法在定义处使用中文注释说明职责、输入输出、关键约束或事务语义。
4. 方法内部的复杂逻辑调用应在调用处添加中文注释，重点解释幂等、事务、非破坏性合并、版本判断、数据安全等业务意图。
5. 简单 getter、setter 和语义明显的单行代码无需重复注释；禁止使用只复述代码或已经过期的注释。

---

## 七、数据库规则

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
6. `rawPayload` 完整保留。
7. `publishTime` 未知时为 `null`。
8. `firstSeenTime` 和 `lastSeenTime` 由服务端维护。
9. 幂等键是 `source + informationType + sourceItemId`。
10. 快照记录不可更新。
11. 业务表的持久化使用 MyBatis-Plus；行锁等特殊查询通过 Mapper 自定义固定 SQL，不在 Controller 中直接操作 Mapper。

Phase 3 数据库规则：

12. TASK-024 只实施 Accepted `../../docs/DATABASE_DESIGN_PHASE3_DRAFT.md`。
13. Analysis 必须绑定 `information_snapshot.id`，所有历史 FK 使用 `ON DELETE RESTRICT`。
14. Actual Token 只能写入 Model Invocation 且只来自 Provider Usage，Estimate 不得回填。
15. Preview 不持久化；第一版不增加 Spring Session JDBC、Usage 汇总或 Cost 表。
16. Worker 领取和完成使用短事务，Provider HTTP 调用必须在事务之外。

---

## 八、接入规则

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

缺失、`null`、空字符串和默认空数组不得破坏已有有效数据。

---

## 九、状态规则

- `information_item.status`：平台内部生命周期。
- `job_information.job_status`：来源职位状态。

一次搜索未出现职位，不能直接标记 `OFFLINE`。

---

## 十、异常和安全

- 不向客户端暴露数据库堆栈。
- 参数错误使用稳定错误码。
- 日志不得输出 Token、Cookie 或完整敏感 payload。
- 不允许空 catch。
- 不允许吞掉异常后返回成功。
- 请求体上限按协议配置。

### Backend Operational Logging

详细规范：

`../../docs/LOGGING_CONVENTIONS.md`

所有新增或修改的后端业务代码必须遵守该规范。

---

## 十一、测试要求

每次修改必须运行与任务相关的测试。

默认使用 Maven Wrapper。

Windows：

```powershell
.\mvnw.cmd test
```

Linux/macOS：

```bash
./mvnw test
```

测试策略：

1. 优先运行与当前修改范围直接相关的定向测试；
2. 定向测试通过后，根据当前 TASK 要求决定是否运行完整回归测试；
3. 完整测试已经成功后，不得无理由重复执行相同完整测试；
4. 因 JDK、Maven、Wrapper Cache、Sandbox 网络权限等环境原因导致失败时，应先识别环境问题，不得通过反复执行相同 Maven 命令进行试错；
5. 测试失败时不得将任务标记为完成。

数据库与接入至少覆盖：

- 正常插入；
- 重复幂等提交；
- 非破坏性更新；
- `contentHash` 未变化；
- `contentHash` 变化；
- 快照唯一约束；
- 事务回滚；
- `rawPayload` 读写；
- `publishTime` 为空；
- 参数和 Token 错误；
- 请求体过大。

---

## 十二、完成任务前

必须：

1. 运行当前 TASK 要求的相关测试。
2. 检查 `git diff`。
3. 核对是否超出当前 TASK。
4. 更新 TASK 实施记录。
5. 更新 `docs/CURRENT_STATUS.md`。
6. 汇报测试命令和实际结果。
7. 如果存在因本机或 sandbox 环境导致无法完成的验证，明确说明原因，不得通过重复试错掩盖环境问题。

---

## 十三、Phase 3 安全与调用

- `/api/v1/jobs/**` 和 AI 用户接口在 Identity MVP 后使用同源 Session。
- `/api/v1/collector/**` 保持现有 Bearer Token，不混用用户 Session/CSRF。
- 状态修改 API 必须有 CSRF；Owner 必须来自认证上下文，不能信任请求中的 `userId`。
- Provider Client 优先复用 Spring `RestClient` 和现有 Jackson，不安装 AI SDK。
- API Key、Bootstrap 密码、Authorization、Cookie、完整 Provider 原始响应不得落库或写日志。
- 结果不确定的 timeout/崩溃调用不得自动盲重试。

---

## 十四、Phase 4 Recommendation 边界

- Recommendation 只消费已有成功 Analysis，不调用 AI Provider。
- Recommendation Worker 只消费已经存在的 PENDING Run；不得增加独立 Recommendation Cron。
- Feed 只读取预计算的最新成功 Run / Item，不在 GET 请求中实时重算。
- Recommendation 业务刷新只允许 Analysis Batch 完成和 Manual Refresh 两种入口。
- Hard exclusion 固定为 `NOT_INTERESTED` 或 `CONTACTED_NOT_SUITABLE`。
- `CONTACTED` 表示已联系但仍可能继续沟通，不自动排除。
- Phase 4 不自动读取或同步 BOSS 聊天记录。
- Profile 修改不自动触发 AI Analysis 或 Recommendation Refresh。
