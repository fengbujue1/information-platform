# TASK-007：实现 Information Hub Client

状态：TODO  
所属阶段：Phase 1  
优先级：P0

## 前置依赖

TASK-005、TASK-006 完成。

## 目标

让 Collector 可配置地向 Information Hub 提交单条职位。

## 本任务范围

- 独立 HTTP Client。
- URL、Token、超时从环境变量读取。
- Hub 功能默认可关闭。
- 明确处理 2xx、4xx、5xx、超时和连接失败。
- 不因 Hub 失败中断原采集流程。

## 验收标准

- [ ] 成功提交测试
- [ ] 超时和失败测试
- [ ] 日志不泄露 Token
- [ ] 原本采集命令仍可运行
