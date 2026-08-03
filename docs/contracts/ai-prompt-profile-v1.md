# AI Prompt Profile V1

状态：Accepted
接受日期：2026-08-03
适用阶段：Phase 3

## 1. Profile

Profile 表示当前用户的一组长期关注点：

```text
id
name
analysisDefinitionKey
activeVersionId
status
createdAt
updatedAt
```

- 一个用户可以有多个 Profile；
- `(userId, name)` 唯一；
- 第一版仅允许 `analysisDefinitionKey=JOB_USER_RELEVANCE`；
- Profile 只保存 User Prompt 的版本引用，不保存 System Prompt；
- 停用使用 `DISABLED`，不硬删除历史。

## 2. Version

```text
id
promptProfileId
versionNo
content
contentHash
createdAt
```

- Version 不可变；
- 同一 Profile 的 `versionNo` 从 1 递增且唯一；
- 同一 Profile 的 `contentHash` 唯一；
- 内容未变化时复用已有 Version；
- 切换 Active Version 必须验证 Version 属于当前用户和该 Profile；
- 创建 Version 与切换 Active Version使用同一明确事务。

## 3. User Prompt 边界

User Prompt：

- 最大 8,000 字符；
- 作为用户偏好输入；
- 不能覆盖 System Prompt；
- 不能修改 Output Schema；
- 不得包含服务器秘密；
- 页面需要提示来源内容和模型输出可能不准确。

平台控制：

- System Prompt；
- Definition key/version；
- Input Projection；
- Output Schema；
- `maxOutputTokens`。

## 4. API

TASK-022 实现以下同源 Session API，具体响应包络沿用项目统一格式：

```http
GET  /api/v1/ai/prompt-profiles
POST /api/v1/ai/prompt-profiles
GET  /api/v1/ai/prompt-profiles/{profileId}
POST /api/v1/ai/prompt-profiles/{profileId}/versions
GET  /api/v1/ai/prompt-profiles/{profileId}/versions
PUT  /api/v1/ai/prompt-profiles/{profileId}/active-version
PUT  /api/v1/ai/prompt-profiles/{profileId}/status
```

所有写请求要求 CSRF。客户端不得提交 `userId` 来决定 Owner。

## 5. Schedule

Schedule 只保存 Profile ID。触发时解析当前 Active Version，并把 Version ID 冻结到 Batch。Profile 无 Active Version、已停用或 Definition 不可用时，触发创建 NOOP/失败结果，不调用 Provider。

## 6. 删除

- Version 不提供物理删除；
- Profile 不提供物理删除；
- 相同内容不创建无意义版本；
- 历史 Analysis/Batch 引用的 Version 永久可追溯。
