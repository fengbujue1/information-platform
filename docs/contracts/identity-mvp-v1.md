# Identity MVP V1

状态：Accepted
接受日期：2026-08-03
适用阶段：Phase 3

## 1. 目标

为账号级 Prompt、Analysis、Batch、Schedule 和 Usage 提供最小身份、Session 与 Owner 隔离，并保持 Collector Bearer Token 链独立。

## 2. API

```http
POST /api/v1/auth/login
POST /api/v1/auth/logout
GET  /api/v1/auth/me
GET  /api/v1/auth/csrf
```

Login 输入：

```json
{
  "username": "admin",
  "password": "<secret>"
}
```

`username` 在服务端 `trim + lower-case`。密码和摘要不得返回。

`me` 最小输出：

```json
{
  "id": 1,
  "username": "admin",
  "displayName": "Admin",
  "timezone": "Asia/Shanghai"
}
```

## 3. 用户字段

```text
id
username
passwordHash
displayName
timezone
status
createdAt
updatedAt
```

状态为 `ACTIVE/DISABLED`。默认 timezone 为 `Asia/Shanghai`。

## 4. 密码

- 使用 Spring Security 的安全 `PasswordEncoder`；
- 只保存摘要；
- 明文、摘要和认证 Header 均不得写日志；
- 仓库、前端和示例配置不保存真实密码；
- 登录失败使用统一响应，避免泄露账号是否存在。

## 5. Session

第一版使用单实例内存 Session，不引入 Spring Session JDBC。

Cookie：

- `HttpOnly=true`；
- `SameSite=Lax`；
- 生产 HTTPS `Secure=true`；
- Path 覆盖同源应用；
- 登录后防止 Session fixation；
- Logout 使 Session 失效。

## 6. CSRF

- Cookie Session 下所有状态修改请求要求 CSRF；
- Web 先调用 `GET /api/v1/auth/csrf`，再按约定 Header 携带 token；
- Collector Bearer Token API 不进入用户 CSRF 流程；
- Login/Logout 的具体 Spring Security matcher 必须有自动化测试，不能全局关闭 CSRF。

## 7. 认证范围

Phase 3：

- `/api/v1/jobs/**` 要求用户 Session；
- `/api/v1/ai/**` 要求用户 Session；
- `/api/v1/auth/login` 与 CSRF 初始化按安全配置允许未登录访问；
- `/api/v1/collector/**` 保持独立 Bearer Token。

现有 Job Web/E2E 必须先登录并携带 Session；这属于 Phase 3 Identity 迁移，不改变 Job Query 的只读字段契约。

## 8. Ownership

以下资源必须带 `user_id` 并在 Application Service 查询条件中隔离：

- Prompt Profile / Version；
- Analysis；
- Model Invocation / Usage；
- Batch / Batch Item；
- Schedule。

普通用户只能读取或修改自己的资源。Controller 不接受客户端传入的 Owner 作为可信身份，Owner 来自认证上下文。

## 9. 初始账号 Bootstrap

Phase 3 不提供公共注册。

Bootstrap 规则：

1. 用户表非空时不执行；
2. 用户表为空且显式提供服务端环境变量时创建一次；
3. 无默认用户名和密码；
4. 环境变量缺失时不自动创建弱账号；
5. 创建完成后运维移除明文密码变量；
6. 重启不得重复创建；
7. 日志只记录结果，不记录用户名以外的秘密。

Bootstrap 必须使用明确事务和数据库唯一约束处理并发启动。

## 10. 不包含

- signup；
- password reset；
- OAuth / MFA；
- RBAC；
- tenant / organization；
- Spring Session JDBC；
- API token 管理；
- Provider 或 Collector 凭据管理。
