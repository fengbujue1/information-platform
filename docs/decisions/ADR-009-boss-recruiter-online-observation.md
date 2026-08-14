# ADR-009：BOSS 招聘者在线观测时间不参与职位内容版本

状态：Accepted
日期：2026-07-27
决策人：项目负责人

## 背景

BOSS 职位搜索响应包含布尔字段 `bossOnline`。当前真实样本中该字段对所有职位均存在，但来源没有同时提供可靠的“最后活跃时间”。

产品希望利用该信号展示招聘者最近一次被 Collector 观察到在线的时间。由于招聘者上线和离线频繁变化，若将观测时间作为职位稳定业务内容参与 `contentHash`，会产生大量没有职位内容变化的版本快照。

## 决策

1. `bossOnline` 只表示 Collector 抓取当时观察到的瞬时在线状态。
2. 当 `bossOnline=true` 时，Collector 在搜索响应处理时生成 ISO-8601 带时区观测时间。
3. 当 `bossOnline=false`、缺失、null 或类型非法时，不生成在线观测时间。
4. Collector 输出：
   - `boss_online`
   - `boss_online_observed_at`
5. TASK-006 Mapper 将 `boss_online_observed_at` 映射到 `recruiterActiveText`。
6. `recruiterActiveText` 表示“最近一次被本 Collector 观察到在线的时间”，不是 BOSS 官方最后活跃时间。
7. false 或缺失场景向 Hub 上报 null，依靠非破坏性合并保留已有非空观测时间。
8. 新的非空观测时间覆盖旧值。
9. `recruiterActiveText` 不参与 `contentHash`，仅该字段变化时不增加版本号、不创建快照。
10. Hash 不变时 Information Hub 仍更新当前 Job 扩展记录，因此最新在线观测时间可以落到当前表。

## 备选方案

### 直接映射 ONLINE / OFFLINE 并参与 Hash

优点是可以从快照还原每次状态变化；缺点是上线和离线会频繁制造职位版本，污染职位内容历史。

### Mapper 读取 TASK-006A 原始响应

无需修改 Collector 结构化输出，但诊断文件默认不存在，包含敏感来源字段，且没有稳定文件关联协议，不适合作为正式输入。

### 新增专用时间列

语义和查询能力最好，但需要数据库迁移和协议升级。Phase 1 暂时复用已有 `recruiterActiveText`，以带时区 ISO-8601 文本保存。

### 只保留在 rawPayload

实现最简单，但前端和查询 API 无法稳定访问该信息。

## 正面影响

- 可以展示最近一次由 Collector 观察到的招聘者在线时间。
- 不因高频在线状态变化制造职位内容快照。
- false 或暂时缺失不会清空已有最后在线观测时间。
- 不需要数据库迁移或新基础设施。
- 保持旧 Collector 文件兼容。

## 负面影响

- 该时间只是 Collector 观测时间，不是来源官方最后活跃时间。
- 字符串字段不适合高效时间范围查询。
- 不保存完整的招聘者在线状态变化历史。
- 依赖 Collector 主机时钟准确。

## 重新评估条件

- BOSS 提供可靠的官方最后活跃时间。
- 产品需要按招聘者最后在线时间进行数据库范围查询或排序。
- 产品需要分析在线时长、回复速度或完整活动事件。
- 多个 Collector 实例需要合并招聘者活动观测。

## 相关文档

- `docs/tasks/TASK-006B.md`
- `docs/tasks/TASK-006.md`
- `docs/contracts/boss-job-field-mapping.md`
- `docs/contracts/information-envelope-v1.md`
- `docs/DATABASE_DESIGN.md`
- `docs/ARCHITECTURE.md`
