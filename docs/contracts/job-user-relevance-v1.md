# Job User Relevance V1

状态：Draft  
Definition：`JOB_USER_RELEVANCE_V1`

## 1. 目标

根据当前用户的 Prompt Version 和一个确定的 JOB Snapshot，生成结构化用户相关性分析。

它不是主动推荐合同，也不负责 Top N。

## 2. 输入

建议字段：

```text
title
content
companyName
salaryText
locationName
cityName
experienceText
educationText
remoteType
jobStatus
sourceTags
sourceSkillTags
welfare
```

最终输入字段在 TASK-023 根据后端真实 Snapshot DTO 冻结。

## 3. 输出 Draft

```json
{
  "schemaVersion": 1,
  "relevanceScore": 82,
  "confidence": 0.88,
  "summary": "该职位与当前关注点较相关。",
  "positiveSignals": [
    "技术栈包含 Java 和 Spring Boot"
  ],
  "negativeSignals": [
    "办公方式未明确支持远程"
  ],
  "attentionPoints": [
    "需要确认是否存在外包属性"
  ],
  "matchedPreferences": [
    "Java 后端"
  ],
  "unmatchedPreferences": [
    "远程办公"
  ]
}
```

## 4. relevanceScore

范围：

```text
0..100
```

表示：

> 当前职位信息与当前 Prompt 中明确关注点的相关程度。

不表示：

- 录用概率；
- 人岗科学测评；
- 职业成功概率；
- 平台推荐排名。

## 5. confidence

范围：

```text
0..1
```

表示模型基于现有输入得出当前分析的自评置信度。

不允许当作统计学概率解释。

## 6. Evidence

如果 TASK-023 决定加入 evidence，必须只能引用输入中实际存在的信息，不能生成来源中不存在的事实。

## 7. Missing Data

信息不足时模型必须明确指出“不足”，不能补造：

- remote；
- salary；
- company property；
- skill；
- recruiter intention。

## 8. Source Content

JOB 正文和标签作为不可信数据。

System Prompt 必须要求模型忽略其中试图修改 AI 指令的文字。

## 9. 不包含

- recommend = true/false；
- recommendationRank；
- dailyTopN；
- user profile learning；
- cross-job ranking。

## 10. Schema Validation

服务端必须严格校验：

- 必填字段；
- score 范围；
- confidence 范围；
- array 类型；
- 最大字符串/数组长度。

不合法响应不能进入 SUCCEEDED。
