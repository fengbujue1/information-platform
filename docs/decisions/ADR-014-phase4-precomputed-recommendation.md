# ADR-014：Phase 4 使用预计算、可解释 Recommendation Run

状态：Accepted

## 决策

Recommendation 使用：

```text
Candidate
→ deterministic scoring
→ ranking
→ persist Run/Item
→ Feed read
```

不在 Feed GET 时实时重算。

Recommendation V1 不新增 LLM。

Run 冻结：

- user；
- Profile snapshot/hash；
- Prompt Version；
- Algorithm Version；
- time window。

Item 冻结：

- Information；
- Snapshot；
- Analysis；
- score breakdown；
- reasons；
- rank。

## Algorithm V1

```text
70% AI relevance
20% structured profile
10% freshness
```

再做 dedup/diversity/Top N。

## Current Interaction

历史 Item 不因用户反馈删除。

Feed 可以依据当前 Interaction 隐藏 hard-excluded Item。

## 不采用

- GET realtime calculation；
- new Recommendation LLM；
- Embedding/Vector Recommendation。
