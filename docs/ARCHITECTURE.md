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
    ├── information_item
    ├── job_information
    └── information_snapshot
```

Collector 不直接连接 MySQL。

## 2. Phase 1 边界

Phase 1 跑通：

```text
BOSS Collector
→ 本地 JSON/CSV
→ Information Hub API
→ MySQL 幂等归档与快照
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
- 非破坏性合并；
- 幂等接入编排；
- 事务边界；
- 接入结果响应。

### information

- 通用信息模型；
- 来源、类型、来源 ID；
- 原始 payload；
- firstSeen / lastSeen；
- 业务内容 Hash；
- 平台生命周期状态；
- 历史快照。

### job

- 公司、薪资、地点、经验、学历；
- 职位描述；
- 技能和福利；
- 来源职位状态；
- 后续远程类型标准化。

## 4. 更新和快照

重复接入时：

```text
读取当前记录
→ 非破坏性合并
→ 对最终业务内容计算 Hash
→ Hash 不变：只更新最近采集元数据
→ Hash 变化：更新当前记录并新增不可变快照
```

一次临时详情抓取失败不得清空已有完整 JD。

## 5. 状态边界

`information_item.status`：

- 平台内部生命周期。

`job_information.job_status`：

- 来源职位的业务状态。

两者不得混用。

## 6. 时间规则

1. Collector 提交 ISO-8601 带时区时间。
2. Backend 使用 `OffsetDateTime` 接收。
3. Backend 转换为 UTC 后写入 MySQL `DATETIME(3)`。
4. API 返回 UTC `Z` 或明确偏移。
5. `publishTime` 未知时必须为 `null`。

## 7. 未来演进

出现真实吞吐、独立扩容或团队协作瓶颈后，再评估：

- Kafka / RabbitMQ
- Elasticsearch
- Qdrant
- MinIO
- Hudi
- 微服务拆分
