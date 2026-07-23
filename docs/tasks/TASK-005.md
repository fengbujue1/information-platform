# TASK-005：实现信息接入 API

状态：TODO  
所属阶段：Phase 1  
优先级：P0

## 前置依赖

TASK-004 完成。

## 目标

实现 InformationEnvelope V1 的幂等接入、非破坏性合并和版本快照。

## 处理流程

```text
认证
→ 请求校验
→ 幂等查询
→ 非破坏性合并
→ Canonical JSON
→ contentHash
→ 当前版本写入
→ 必要时增加 versionNo 和快照
→ 响应
```

## 必须支持

- sourceItemId 中的特殊字符原样保存。
- sourceRecruiterId。
- detailStatus。
- sourceTags 和 sourceSkillTags。
- 安全 rawPayload。
- A → B → A 三个版本。
- content=null 不清空已有 JD。
- Hash 未变化不增加版本号。

## 测试

- 首次提交。
- 完全重复提交。
- 详情补充后内容变化。
- 详情暂时缺失。
- 薪资变化。
- 来源标签顺序变化但内容等价。
- 主表、扩展表和快照事务回滚。
