# ADR-004：Collector 通过 HTTP 接入

状态：Accepted  
日期：2026-07-23  
决策人：项目负责人

## 背景

Collector 使用不同语言并可能运行在不同电脑上。

## 决策

Collector 不直接写 MySQL，统一通过 InformationEnvelope HTTP API 提交。

## 备选方案

- 直写数据库：简单但强耦合并暴露数据库凭证。
- 消息队列：吞吐高，但当前部署和维护成本不必要。

## 代价

后端不可用时需要本地 Outbox，并维护协议兼容性。

## 重新评估条件

采集吞吐显著增长或出现多个异步消费者时评估消息队列。
