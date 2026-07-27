# TASK-007A：增加本地与服务器外部配置文件

状态：DONE
所属阶段：Phase 1
优先级：P0

## 前置依赖

TASK-007 完成。

## 目标

让 Information Hub 和 Collector 可以直接从仓库外部配置文件启动，同时保留环境变量覆盖能力，避免每次手工设置环境变量。

## 本任务范围

- Information Hub 提供 Spring Boot 外部 `application.yml` 示例。
- Collector 支持默认 `config/collector.ini` 和可选 `--config` 路径。
- Collector 环境变量优先于 INI，兼容既有部署方式。
- 真实配置文件加入 Git 忽略，仓库只提交无秘密的示例模板。
- 不修改 BOSS 采集核心，不实现 Outbox。

## 验收标准

- [x] Collector 可从默认 INI 读取 Hub、超时和映射器配置。
- [x] Collector 可通过 `--config` 指定配置文件。
- [x] 环境变量可以覆盖 INI。
- [x] Hub 可使用标准外部 `config/application.yml`。
- [x] 示例模板不包含真实密码、Token 或 Cookie。
- [x] 真实配置文件不会被 Git 追踪。
- [x] 配置错误不会中断原采集流程。

## 实施记录

完成时间：2026-07-27

- Collector 使用 Python 标准库 `configparser` 读取 INI，没有新增依赖。
- 默认配置路径相对于 Collector 项目定位，不依赖命令执行目录。
- 显式指定且不存在、不可读取或格式错误的配置文件会产生安全配置错误；集成边界捕获该错误且不改变原采集命令结果。
- Hub Client 配置优先级为环境变量、INI、默认值；Hub 默认保持关闭。
- Mapper 的 Collector 标识、版本和历史时间解释时区可从同一 INI 读取。
- Information Hub 使用 Spring Boot 原生外部配置机制，没有新增配置加载基础设施。
- Information Hub 服务端配置说明位于后端模块，Collector 文档仅说明客户端连接配置并提供跨模块链接。
- 未修改 Chrome、CDP、登录、搜索、详情、分页、去重或本地 JSON/CSV 输出核心。

## 验证结果

- `python -m unittest tests.test_hub_client`：23 项全部通过。
- `python -m unittest tests.test_boss_job_merger tests.test_information_mapper`：14 项全部通过。
- Python UTF-8 模式完整回归：128 项中 123 项通过；5 项为此前已记录的 Windows Chrome 路径/端口基线失败，本任务未新增失败。
- `python -m py_compile ...`：通过。
- `python scripts/boss_cdp_raw.py --help`：显示新增 `--config` 参数。
- `.\mvnw.cmd test`：24 项，0 失败、0 错误、11 项因未连接集成测试库而跳过。
