# Information Platform Architecture

## 1. 当前总体结构

```text
BOSS Collector
    │
    │ 合并列表 JSON 和详情 JSON
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
BOSS 列表采集
→ BOSS 详情采集
→ 按 job_id 合并
→ 使用 encrypt_job_id 构建幂等身份
→ Information Hub
→ 当前版本和历史快照
→ Job Query API
```

Phase 1 不包含 Vue、AI、推荐、消息队列和微服务。

## 3. 身份边界

- `job_id`：Collector 内部列表与详情 Join Key。
- `encrypt_job_id`：BOSS 来源职位 ID。
- `information_item.source_item_id`：保存 encrypt_job_id。
- 平台不依赖 job_id 的派生算法。

## 4. 数据变化

```text
读取当前记录
→ 非破坏性合并
→ 规范化稳定业务字段
→ 计算 contentHash
→ Hash 相同：更新最近采集元数据
→ Hash 不同：更新当前版本并新增 version_no 快照
```

快照使用版本号唯一约束，允许记录：

```text
A → B → A
```

## 5. 原始数据

中央 rawPayload 保存：

- 安全清理后的列表对象；
- 安全清理后的详情对象。

中央 rawPayload 不保存：

- security_id
- lid
- Cookie
- Token
- 浏览器凭证

## 6. 状态边界

- `information_item.status`：平台内部生命周期。
- `job_information.job_status`：来源职位业务状态。
- `job_information.detail_status`：详情抓取状态。

三者不得混用。

## 7. 时间

- 新输出必须包含时区。
- 历史无时区数据必须通过显式配置解释。
- 所有数据库时间统一存 UTC。
- publishTime 不得由 collectedAt 伪造。

## 8. 开发与集成测试数据库

开发和集成测试使用远程服务器中的 Docker MySQL：

```text
开发主机 A ── SSH 隧道 ──┐
                         ▼
开发主机 B ── SSH 隧道 ── 远程服务器 127.0.0.1:MySQL 宿主机端口
                         │
                         ▼
                    MySQL 容器
                         │
                         ▼
                    持久化数据卷
```

安全边界：

- MySQL 宿主机端口只绑定远程服务器的回环地址，不直接暴露公网。
- 服务器防火墙或安全组不允许公网入站访问 MySQL 端口。
- 开发主机通过使用密钥认证的 SSH 隧道连接数据库。
- 开发库与集成测试库分离，并使用不同的非 root 最小权限账号。
- 自动测试只允许连接测试库。
- 密码、SSH 私钥和其他连接凭据不进入 Git。
- 数据库结构只通过 Flyway 迁移，禁止在共享环境执行 `flyway clean`。
- 远程共享数据库只用于开发和集成测试，生产环境必须使用独立数据库。
- Collector 仍然只连接 Information Hub，不直接连接 MySQL。
