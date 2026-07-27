# TASK-007：实现 Information Hub Client

状态：DONE
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

- [x] 成功提交测试
- [x] 超时和失败测试
- [x] 日志不泄露 Token
- [x] 原本采集命令仍可运行

## 实施记录

完成时间：2026-07-27

- 新增独立 `InformationHubClient`，复用项目已有 `requests` 依赖，按单条 InformationEnvelope 调用 Hub 写入接口。
- 新增环境变量配置：`INFORMATION_HUB_ENABLED`、`INFORMATION_HUB_URL`、`INFORMATION_HUB_COLLECTOR_TOKEN`、连接超时和读取超时；Hub 默认关闭。
- Token 从配置对象字符串表示、日志、异常文本和响应日志中隐藏，只用于 Bearer 请求头。
- 2xx 视为成功；4xx 和 413 记录单条失败并继续；5xx、非预期状态、超时或连接失败会停止本批次剩余请求，避免累计等待。
- 列表和详情完成本地保存后才执行 Mapper 和 Hub 提交；配置、映射、HTTP 或集成层异常均不会中断原采集流程。
- 未实现自动重试、Outbox 或补传；失败数据继续保留在本地采集文件，持久化补传留给 TASK-008。
- 未修改 Chrome、CDP、登录、搜索、详情、分页、去重或本地 JSON/CSV 输出核心。

## 验证结果

- `python -m py_compile integrations/config.py integrations/hub_client.py integrations/information_mapper.py integrations/boss_job_merger.py scripts/boss_cdp_raw.py tests/test_hub_client.py`：通过。
- `python -m unittest tests.test_hub_client`：17 项全部通过。
- `python -m unittest tests.test_boss_job_merger tests.test_information_mapper`：14 项全部通过。
- Python UTF-8 模式完整回归：122 项中 117 项通过；5 项为 TASK-006 前已记录的 Windows Chrome 路径/端口基线失败，本任务未新增失败。
- `python scripts/boss_cdp_raw.py --help`：通过，原命令入口保持可用。
