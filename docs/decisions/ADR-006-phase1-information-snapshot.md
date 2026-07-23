# ADR-006：Phase 1 保存信息历史快照

状态：Accepted  
日期：2026-07-23  
决策人：项目负责人

## 背景

职位的 JD、薪资、福利和状态会发生变化。只保留当前记录会丢失历史信息。

## 决策

Phase 1 创建 information_snapshot。

- 首次接入保存版本 1。
- 当前业务 Hash 未变化时不新增版本。
- Hash 变化时 current_version_no 加 1，并保存不可变快照。
- information_item 和 job_information 保存最新版本。

## 版本唯一性

使用：

```text
UNIQUE (information_id, version_no)
```

不使用：

```text
UNIQUE (information_id, content_hash)
```

原因是需要保存：

```text
A → B → A
```

第三次恢复为 A 仍然是一个新的历史版本。

## 代价

- 接入事务更复杂。
- 需要行锁或等效并发控制生成版本号。
- 快照表会持续增长。

## 重新评估条件

快照规模影响 MySQL 时，评估冷热分层、MinIO 或数据湖归档。
