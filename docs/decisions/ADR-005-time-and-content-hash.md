# ADR-005：统一 UTC 时间与规范化业务内容 Hash

状态：Accepted  
日期：2026-07-23  
决策人：项目负责人

## 背景

Collector 可能运行在不同地区和电脑，且同一信息会重复采集。需要可靠比较数据是否真正变化。

## 决策

1. 接口接收 ISO-8601 带偏移时间。
2. Backend 转为 UTC。
3. MySQL `DATETIME(3)` 保存 UTC。
4. `content_hash` 使用 SHA-256。
5. Hash 输入是规范化业务字段，不包含采集时间、Collector 实例和 first/last seen 等易变字段。

## 正面影响

- 跨时区行为一致。
- 重复采集不会无意义触发 AI。
- Collector 版本变化不会自动造成业务内容变化。

## 代价

必须维护稳定的规范化和 Canonical JSON 规则。

## 重新评估条件

需要保存来源原始时区语义，或协议升级为事件溯源时重新评估。
