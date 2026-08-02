# AI Prompt Profile V1

状态：Draft  
适用阶段：Phase 3

## 1. Profile

Prompt Profile 属于当前登录用户。

字段语义：

```text
id
name
analysisDefinitionKey
activeVersion
status
createdAt
updatedAt
```

## 2. 多 Profile

一个用户可创建多个 Profile。

名称只需在当前用户范围内可识别；是否唯一由 TASK-022 决定。

## 3. Version

每次保存 Prompt 内容修改：

```text
create new immutable version
→ set activeVersion
```

禁止修改已经存在的历史 Version 内容。

## 4. Prompt Version 字段

```text
id
profileId
versionNo
content
contentHash
createdAt
```

## 5. User Prompt 边界

User Prompt 用于描述用户关注点。

它不能：

- 替换 System Prompt；
- 修改 Output Schema；
- 关闭安全规则；
- 请求读取 rawPayload；
- 请求暴露 API Key；
- 改变 Definition 的 Information Type。

## 6. 长度

必须设置最大 Prompt 长度。

具体数值由 TASK-022 根据 Token Budget 冻结。

## 7. Schedule

Schedule 保存 Profile ID。

运行时解析 Active Version。

Batch 一旦创建，只保存并使用冻结的 Version ID。

## 8. 删除

被历史 Analysis/Batch 使用的 Version 不物理删除。

Profile 优先使用停用状态。

## 9. API

需要支持：

- Profile 列表；
- 创建；
- 查看；
- 创建新 Version；
- 查看 Version 历史；
- 切换 Active Version；
- 停用。

最终 endpoint 在 TASK-022 冻结。
