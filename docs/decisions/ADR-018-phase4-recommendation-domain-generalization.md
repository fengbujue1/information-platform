# ADR-018：Recommendation 使用 Generic Core + Information-Type-specific Extension

状态：Accepted

日期：2026-08-09

## 背景

V3 将 JOB 偏好直接放入 `user_recommendation_profile`，并将 `jobDisposition` 放入通用 Interaction。该结构可支持 JOB MVP，但会使 Recommendation Core 随未来 Information Type 持续增加领域专属 nullable 字段，也限制同一用户只能拥有一个 Profile。

## 决策

Recommendation 数据模型采用：

```text
Generic Recommendation Core
        ↓
Information-Type-specific Domain Extension
```

与既有 `information_item` + `job_information` 的边界一致。

### Profile

- `user_recommendation_profile` 只保存 Owner、`informationType`、Prompt 绑定、通用窗口/数量、完整组合 hash；
- Core 按 `(userId, informationType)` 唯一；
- JOB 偏好保存到 1:1 `job_recommendation_profile`；
- `contentHash` 和 Run snapshot 覆盖 Core + 对应领域扩展的完整组合 Profile。

### Interaction

- `user_information_interaction` 只保存通用 view/feedback/attribution；
- JOB workflow 状态保存到 1:1 `user_job_disposition`；
- 通用 hard exclusion 是 `NOT_INTERESTED`；
- JOB 额外 hard exclusion 是 `CONTACTED_NOT_SUITABLE`；`CONTACTED` 不排除。

### Run 与 Algorithm

- `recommendation_run` 显式冻结 `informationType`；
- Profile、Refresh、Run 与 Feed API 显式选择 `informationType`；
- Algorithm 按领域独立标识和版本化；`JOB_RECOMMENDATION / V1` 的 70/20/10 只属于 JOB。

## Phase 4 实际范围

Phase 4 仍只实现：

```text
informationType = JOB
```

本 ADR 只冻结扩展边界，不实施 EDUCATION、MEDICAL、REAL_ESTATE、POLICY、NEWS 等未来领域，也不采用 Generic JSON 万能 Profile 或 EAV。

## Migration

V3 已执行且保持不可变。TASK-034A 使用 V4：

- 回填既有 Profile/Run 为 JOB；
- 将 V3 JOB 偏好和 disposition 无损复制到领域扩展；
- 校验复制结果后删除 Core 中的 JOB 专属列；
- 调整唯一约束和包含 `informationType` 的 Run 索引。

## 影响

- TASK-035 及后续 Recommendation 任务必须从 Generic Core 与 JOB Extension 组合领域对象；
- Service 必须校验 JOB Extension 只关联 JOB Core，Job disposition 只关联 JOB Information；
- 旧 ADR-014～ADR-016 的预计算、触发、Feedback/Disposition 语义继续有效，本 ADR 纠正其物理与领域边界。
