# Information Platform Architecture

## 1. 当前总体结构

当前采用：

- 独立采集器；
- 模块化单体 Information Hub；
- 独立 Web 前端；
- MySQL 主数据库。

```text
BOSS Collector
    │
    │ InformationEnvelope V1 / HTTP
    ▼
Information Hub
    ├── ingestion
    ├── information
    ├── job
    ├── analysis（后续）
    ├── recommendation（后续）
    └── notification（后续）
    │
    ▼
MySQL
```

Collector 不直接连接 MySQL。

## 2. Phase 1 边界

Phase 1 只跑通：

```text
BOSS Collector
→ 本地 JSON/CSV
→ Information Hub API
→ MySQL 幂等归档
→ Job Query API
```

Phase 1 不包含：

- Vue 页面；
- AI 分析；
- 用户画像；
- 推荐；
- 消息队列；
- 微服务。

## 3. 模块职责

### ingestion

- Collector 认证；
- 协议版本校验；
- 请求参数校验；
- 幂等接入编排；
- 事务边界；
- 接入结果响应。

### information

- 通用信息模型；
- 来源、类型、来源 ID；
- 原始 payload；
- firstSeen / lastSeen；
- 业务内容 Hash；
- 通用状态。

### job

- 公司、薪资、地点、经验、学历；
- 职位描述；
- 技能和福利；
- 职位来源状态；
- 后续远程类型标准化。

### analysis（Phase 3）

- AI 任务；
- Prompt 与模型版本；
- 结构化分析结果；
- 重试与费用记录。

### recommendation（Phase 4）

- 内容质量分；
- 用户匹配分；
- 最终推荐分；
- 相似职位去重；
- 公司多样性。

### notification（Phase 5）

- 站内通知；
- 邮件和 Webhook；
- 后续微信与短信。

## 4. 统一协议

协议文档：

`docs/contracts/information-envelope-v1.md`

幂等键：

```text
source + informationType + sourceItemId
```

服务端维护：

- `firstSeenTime`
- `lastSeenTime`
- `contentHash`
- `createdAt`
- `updatedAt`

## 5. 数据变化判断

`contentHash` 不是对完整请求 JSON 直接计算。

Hash 输入只包含规范化后的稳定业务内容，例如：

- title
- content
- sourceUrl
- Job extension 中的公司、薪资、地点、经验、学历、技能、福利和状态

Hash 输入不包含：

- collectedAt
- collectorId
- collectorVersion
- firstSeenTime
- lastSeenTime
- rawPayload 中的易变追踪字段

相同幂等键且 Hash 未变化：

- 更新 `last_seen_time` 和最近采集元数据；
- 不创建后续 AI 任务。

Hash 发生变化：

- 更新当前记录；
- 后续 Phase 可保存 snapshot；
- 重新创建分析任务。

## 6. 时间规则

1. Collector 提交 ISO-8601 带时区时间。
2. Backend 使用 `OffsetDateTime` 接收。
3. Backend 转换为 UTC 后写入 MySQL `DATETIME(3)`。
4. API 返回 UTC `Z`，或返回明确的时区偏移。
5. `publishTime` 未知时必须为 `null`。
6. 不得使用 `collectedAt` 伪造 `publishTime`。

## 7. 当前存储

Phase 1：

- `information_item`
- `job_information`

后续规划：

- `information_snapshot`
- `analysis_task`
- `analysis_result`
- `user_profile`
- `recommendation_record`
- `notification_task`

当前原始 JSON 存入 MySQL JSON 字段。大文件和网页快照后续迁移到 MinIO。

## 8. 未来演进

出现真实吞吐、独立扩容或团队协作瓶颈后，再评估：

- Kafka / RabbitMQ
- Elasticsearch
- Qdrant
- MinIO
- Hudi
- 微服务拆分

不得仅为了“看起来大型”而提前引入这些组件。
