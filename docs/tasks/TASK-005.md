# TASK-005：实现信息接入 API

状态：DONE
所属阶段：Phase 1  
优先级：P0

## 前置依赖

TASK-004 完成。

## 目标

实现 InformationEnvelope V1 的幂等接入、非破坏性合并和版本快照。

## 处理流程

```text
认证
→ 请求校验
→ 幂等查询
→ 非破坏性合并
→ Canonical JSON
→ contentHash
→ 当前版本写入
→ 必要时增加 versionNo 和快照
→ 响应
```

## 必须支持

- sourceItemId 中的特殊字符原样保存。
- sourceRecruiterId。
- detailStatus。
- sourceTags 和 sourceSkillTags。
- 安全 rawPayload。
- A → B → A 三个版本。
- content=null 不清空已有 JD。
- Hash 未变化不增加版本号。

## 测试

- 首次提交。
- 完全重复提交。
- 详情补充后内容变化。
- 详情暂时缺失。
- 薪资变化。
- 来源标签顺序变化但内容等价。
- 主表、扩展表和快照事务回滚。

## 实施记录

完成时间：2026-07-26

实施内容：

- 实现 `POST /api/v1/collector/items`，首次创建返回 `201 / ITEM_CREATED`，幂等更新返回 `200 / ITEM_UPDATED`。
- 使用环境变量 `INFORMATION_HUB_COLLECTOR_TOKEN` 配置单 Collector Bearer Token；未配置时接口拒绝接入，不允许匿名降级。
- 请求体默认限制为 2 MiB，同时覆盖已知 Content-Length 和流式读取场景。
- 使用 Bean Validation 和协议边界校验验证 InformationEnvelope V1、JOB、BOSS、带偏移时间、Job 状态及字段长度。
- 对 rawPayload 递归检查敏感字段，发现 Cookie、Token、`security_id`、`lid`、凭据等字段时拒绝请求。
- 实现字符串、数组和空值规范化、非破坏性合并、Canonical JSON 和 SHA-256 contentHash。
- Hash 数组计算时去重排序，持久化时保持来源首次出现顺序；采集元数据、rawPayload、salarySource 和详情状态不参与 Hash。
- 使用 MyBatis-Plus 3.5.17 的 PO、Mapper 和 `BaseMapper` 写入当前主表、Job 扩展表和不可变快照。
- `information` 与 `job` 分别拥有自己的 PO/Mapper，`ingestion` 基础设施适配器组合三表持久化。
- 幂等查询通过 Mapper 的固定 `FOR UPDATE` 查询锁定当前主记录；事务模板继续负责三表原子写入。
- TASK-005 新增的 PO、DTO、Domain、配置对象等实体已逐字段补充中文注释。
- 认证、限流、协议归一化、非破坏性合并、Canonical Hash、事务编排和三表持久化等复杂逻辑已在方法定义或关键调用处补充中文注释。
- 上述注释规则已同步到根目录、后端模块 `AGENTS.md` 和项目上下文，作为后续 Java 实现的通用要求。
- 首次写入创建版本 1；Hash 变化递增版本；Hash 不变不创建快照；并发首次写入遇到幂等唯一键竞争时自动重试一次。
- 增加稳定错误结构，客户端响应不包含 Token、完整 payload、SQL 或数据库堆栈。

验证记录：

- Java 21 执行 `.\mvnw.cmd test`：共发现 21 项测试；未注入测试库凭据时 11 项本地测试全部通过，10 项数据库测试按条件跳过。
- 通过临时 SSH 隧道连接独立测试库执行
  `.\mvnw.cmd "-Dtest=Phase1SchemaMigrationIntegrationTest,InformationIngestionIntegrationTest" test`：
  10 项全部通过。
- 持久化层切换到 MyBatis-Plus 后重新执行上述两组测试，结果保持一致。
- 接入集成测试的所有正常写入均由测试事务回滚；快照模拟失败场景验证主表和扩展表无残留。
- 覆盖首次提交、完全重复、详情缺失、薪资变化、标签顺序等价、A → B → A、特殊字符 ID、rawPayload、sourceRecruiterId、detailStatus、认证、参数错误和请求体过大。

结果：TASK-005 验收通过。未修改 Flyway V1、BOSS Collector、前端或部署环境。
