# Information Platform Architecture

## 一、总体结构

系统采用：

独立采集器 + 模块化单体主服务 + 独立前端

Collector 不直接连接数据库。

所有 Collector 通过统一 HTTP 协议向 Information Hub 提交数据。

## 二、核心模块

- ingestion
  - 接收、校验、认证和幂等

- information
  - 通用信息模型

- job
  - 职位业务字段和规则

- analysis
  - AI 任务和分析结果

- recommendation
  - 个性化评分和推荐

- notification
  - 站内、邮件、Webhook、微信和短信

- user
  - 用户画像和偏好

## 三、统一信息协议

公共字段：

- schemaVersion
- informationType
- source
- sourceItemId
- sourceUrl
- title
- content
- publishTime
- collectedAt
- collector
- rawPayload
- extension

服务端字段：

- firstSeenTime
- lastSeenTime
- contentHash
- status

## 四、幂等规则

幂等键：

source + informationType + sourceItemId

重复提交：

- 更新 lastSeenTime
- 更新当前标准化字段
- 比较 contentHash

内容没有变化时，不重新调用 AI。

内容变化时，保存快照并重新创建分析任务。

## 五、当前存储

MySQL：

- information_item
- job_information
- analysis_task
- analysis_result
- user_profile
- recommendation_record

当前原始 JSON 存入 MySQL JSON 字段。

未来大文件迁移到 MinIO。

## 六、未来演进

当数据量和任务量达到瓶颈后，再拆分：

- collector-gateway
- information-service
- analysis-worker
- recommendation-service
- notification-service
- search-service

未来可增加：

- Kafka
- Elasticsearch
- Qdrant
- MinIO
- Hudi

当前不得提前实现这些组件。