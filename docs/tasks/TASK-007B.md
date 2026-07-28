# TASK-007B：修复 Collector 任意工作目录启动

状态：DONE
所属阶段：Phase 1
优先级：P0

## 前置依赖

TASK-007A 完成。

## 背景

TASK-007 和 TASK-007A 完成后，真实采集运行暴露了启动入口缺陷：

1. 按 README 使用 `python scripts/boss_cdp_raw.py ...` 可以完成 BOSS 列表和详情采集，并正常保存本地结果。
2. 采集完成后进入 Information Hub 提交阶段，`boss_cdp_raw.py` 延迟执行 `from integrations import submit_boss_results_to_hub`。
3. 直接执行脚本时，Python 将 `scripts/` 作为首要模块搜索目录，不能稳定找到同级的 `integrations/` 包。
4. 集成边界捕获异常后只记录 `ModuleNotFoundError`，导致本地数据保存成功但本批数据没有提交到 Information Hub。
5. 既有 `--help` 子进程测试没有进入延迟导入和提交边界，因此未发现该问题。

当前可以通过切换到 Collector 根目录、设置 `PYTHONPATH`、使用 `python -m` 或可编辑安装临时规避，但这些方式不应成为原脚本入口的使用前提。

## 目标

让 `boss_cdp_raw.py` 根据自身绝对路径解析 Collector 项目根目录，并在不依赖当前工作目录和手工设置 `PYTHONPATH` 的情况下加载 `integrations`。

用户从任意工作目录使用 `boss_cdp_raw.py` 的绝对路径启动采集时，原采集流程、本地保存和 Information Hub 集成均应正常工作。

## 本任务范围

- 在 `boss_cdp_raw.py` 启动入口解析 Collector 项目根目录。
- 在导入 Information Hub 集成层前，确保 Collector 根目录处于有效的 Python 模块搜索路径中。
- 路径处理必须基于 `Path(__file__).resolve()`，不得依赖终端当前目录。
- 保持现有 `python scripts/boss_cdp_raw.py ...`、`python -m scripts.boss_cdp_raw ...` 和已安装的 `boss-scraper` 入口兼容。
- 增加从临时目录或其他任意工作目录执行绝对脚本路径的子进程回归测试。
- 回归测试必须实际覆盖 `integrations` 导入，不能只验证 `--help`。
- 测试不得访问真实 BOSS、真实 Information Hub、真实数据库或真实 Chrome Profile。
- 更新 Collector README 和集成说明，记录支持的启动方式及工作目录无关约束。
- 完成后更新本任务实施记录和 `docs/CURRENT_STATUS.md`。

## 实施约束

- 不修改 Chrome、CDP、登录、搜索、详情、分页、随机延迟、去重和本地 JSON/CSV 输出核心。
- 不修改 InformationEnvelope 映射规则、Hub HTTP 处理策略或数据库逻辑。
- 不实现 Outbox、自动重试或独立补传。
- 不新增第三方依赖。
- 不要求用户设置永久 `PYTHONPATH`。
- 不在日志中输出 Token、Cookie、完整 payload 或其他敏感信息。
- 路径初始化必须幂等，不得重复插入相同目录，也不得删除调用方已有的模块搜索路径。

## 计划修改文件

- `collectors/boss-zhipin-scraper/scripts/boss_cdp_raw.py`
- `collectors/boss-zhipin-scraper/tests/test_script_entrypoint.py`
- `collectors/boss-zhipin-scraper/README.md`
- `collectors/boss-zhipin-scraper/INTEGRATION.md`
- `docs/tasks/TASK-007B.md`
- `docs/CURRENT_STATUS.md`

如实现过程中不需要独立测试文件，可以将回归用例放入职责最接近的既有测试文件，但必须保留清晰的任意工作目录子进程测试边界。

## 测试方案

1. 从临时工作目录使用 Python 解释器和 `boss_cdp_raw.py` 绝对路径启动安全子进程。
2. 在子进程中实际触发 `integrations` 导入，并使用明确关闭 Hub 的临时配置，保证不发生真实网络请求。
3. 验证进程成功退出，且不会出现 `ModuleNotFoundError`。
4. 验证原 `--help`、`--check` 参数解析和既有命令入口未被破坏。
5. 运行 Hub Client、Mapper 和 Collector 相关回归测试。
6. 运行 `python -m py_compile` 和 `git diff --check`。

## 验收标准

- [x] 从 Collector 根目录执行原脚本命令仍然正常。
- [x] 从任意工作目录使用绝对脚本路径启动时，不需要执行 `Set-Location`。
- [x] 不需要手工设置 `PYTHONPATH`。
- [x] Information Hub 集成层能够被成功导入。
- [x] 回归测试实际经过集成层延迟导入边界。
- [x] 测试过程不访问真实外部服务或浏览器数据。
- [x] 原采集和本地保存逻辑未被改写。
- [x] README 与 INTEGRATION 的启动说明和实际代码一致。
- [x] 相关测试通过，且未新增既有测试失败。

## 不在本任务范围

- 安装或注册系统级命令。
- 修改用户 PowerShell Profile 或系统 `PATH`。
- 改造为新的 Python 包结构。
- 重写 Collector 核心。
- Outbox、自动重试、补传和任务调度。
- Information Hub 后端、数据库或部署配置变更。

## 实施记录

完成时间：2026-07-28

- `boss_cdp_raw.py` 使用既有的 `Path(__file__).resolve().parent.parent` 解析 Collector 根目录。
- 仅当项目根目录尚未存在于 `sys.path` 时，将其插入模块搜索路径；不删除或重排调用方的其他路径。
- 保留 `integrations` 的延迟导入和最外层安全异常边界，未改变 Hub 提交失败不影响本地采集结果的语义。
- 新增独立入口回归测试，从临时工作目录使用脚本绝对路径验证 `--help`，并通过无真实网络的子进程实际触发 Hub 集成层导入。
- 回归测试显式移除 `PYTHONPATH` 和 `INFORMATION_HUB_*` 环境变量，并使用 Hub 关闭的临时 INI，防止依赖开发主机环境或访问真实服务。
- README 和 INTEGRATION 增加任意工作目录启动示例，并明确默认配置、结果和 Chrome Profile 仍相对于 Collector 项目定位。
- 未修改 Chrome、CDP、登录、搜索、详情、分页、随机延迟、去重和本地 JSON/CSV 输出核心。

## 验证结果

- `python -m py_compile scripts/boss_cdp_raw.py integrations/config.py integrations/hub_client.py integrations/information_mapper.py tests/test_script_entrypoint.py`：通过。
- `python -m unittest tests.test_script_entrypoint tests.test_hub_client tests.test_boss_job_merger tests.test_information_mapper`：39 项全部通过。
- Python UTF-8 模式完整回归：130 项中 125 项通过；5 项为此前已记录的 Windows Chrome 路径/端口基线失败，本任务未新增失败。
- `git diff --check`：通过。
