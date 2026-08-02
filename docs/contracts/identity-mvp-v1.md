# Identity MVP V1

状态：Draft  
适用阶段：Phase 3

## 1. 目标

为账号级 Prompt、Analysis、Batch、Schedule 和 Usage 提供最小身份能力。

## 2. 功能

至少支持：

```http
POST /api/v1/auth/login
POST /api/v1/auth/logout
GET  /api/v1/auth/me
```

最终路径由 TASK-021 结合现有 API 约定确认。

## 3. 用户字段

最小用户模型：

```text
id
username
displayName
timezone
status
```

密码摘要不返回前端。

## 4. 密码

- 不保存明文；
- 使用 Spring Security 支持的安全 PasswordEncoder；
- 不在日志打印密码；
- 不在 Git 保存真实初始密码。

## 5. Session

推荐：

- HttpOnly；
- SameSite；
- 生产 HTTPS 下 Secure；
- Session fixation protection；
- 退出登录失效 Session。

## 6. CSRF

如果采用 Cookie Session：

- 所有状态修改请求必须有 CSRF 防护；
- 前端按 Spring Security 方案携带 token；
- Collector Bearer Token API 不使用用户 CSRF 流程。

## 7. Ownership

以下资源必须带 Owner：

- Prompt Profile；
- Prompt Version；
- Analysis；
- Batch；
- Schedule；
- Usage。

普通用户只能读取和修改自己的资源。

## 8. Collector Authentication

现有 Collector Bearer Token 保持独立，不转换成 User Session。

## 9. Job Query API

是否要求 Session 为 TASK-020 必须确认的兼容性决定。

不管最终如何，AI 写接口必须认证。

## 10. 不包含

- signup；
- password reset；
- OAuth；
- MFA；
- RBAC；
- tenant；
- organization。

## 11. 初始账号

Phase 3 不提供公共注册。

TASK-021 必须选择安全的受控 Bootstrap 方式，并保证真实密码不进入仓库。
