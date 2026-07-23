# ADR-007：BOSS 职位身份与列表详情合并

状态：Accepted  
日期：2026-07-23  
决策人：项目负责人

## 背景

BOSS Collector 分别输出职位列表 JSON 和职位详情 JSON。

列表和详情包含两个不同用途的标识：

- job_id
- encrypt_job_id

## 决策

1. `job_id` 只作为 Collector 内部列表与详情 Join Key。
2. `encrypt_job_id` 作为平台 sourceItemId。
3. Mapper 在提交 Hub 前完成列表和详情合并。
4. 列表是结构化字段主要来源。
5. 详情主要补充 JD。
6. 重复字段冲突时记录警告并保留双边 rawPayload。
7. security_id 和 lid 在提交 Information Hub 前删除。
8. 重复提交采用非破坏性合并，缺失、null、空字符串或空数组不覆盖已有有效值。

## 理由

- 平台幂等键必须基于来源稳定身份。
- 平台不应依赖 Collector 本地派生算法。
- 合并逻辑放在 Collector 适配层，可避免后端绑定 BOSS 两文件格式。

## 重新评估条件

BOSS Collector 改为一次输出完整职位对象，或者来源 ID 规则发生变化时重新评估。
