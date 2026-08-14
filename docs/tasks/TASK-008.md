# TASK-008：实现本地 Outbox

状态：DONE
所属阶段：Phase 1
优先级：P0

## 前置依赖

TASK-007 和 TASK-007B 完成。

## 目标

后端不可用时将待提交数据可靠保存，并支持后续补传。

## 本任务范围

- 原子写入 Outbox。
- 重试次数和下次重试时间。
- 成功后移除或归档。
- 损坏文件隔离。
- 同一幂等键重复补传安全。
- 启动时或独立命令补传。

## 实施决策

- Outbox 使用 Collector 本地文件目录 `result/outbox/`，不引入新基础设施。
- 使用独立的 `--flush-outbox` 命令补传，不在 Collector 启动时自动补传，避免抓取启动阶段引入额外网络等待。
- `pending/` 保存待补传数据，`quarantine/` 隔离损坏文件，`rejected/` 保存补传时收到永久失败响应的数据。
- 临时文件与目标文件写入同一目录，执行 flush、`fsync` 和原子替换后才对补传程序可见。
- 新入队数据立即到期；补传失败后采用 60 秒起步、最大 1 小时的指数退避，不设置最大重试次数。
- 仅将 5xx、非预期 HTTP 状态、超时、连接失败、请求失败和未知客户端异常视为可重试失败；4xx、413、配置错误和映射错误不进入 Outbox。
- 遇到可重试失败时，将当前项及本批次尚未发送的安全 Envelope 入队；补传成功后删除待处理文件。
- Outbox 只保存 Mapper 清理后的 InformationEnvelope 和安全重试元数据，不保存 Token、请求头、Cookie 或 Collector 配置。
- 同一 `source + sourceItemId` 可能存在多个待处理文件，依赖 Information Hub 已实现的幂等写入保证重复补传安全。

## 验收标准

- [x] 后端停止时采集不失败
- [x] 数据进入 Outbox
- [x] 后端恢复可补传
- [x] 损坏数据不会阻塞全部队列

## 实施记录

- 新增 `integrations/outbox.py`，实现原子入队、到期判断、退避重排、成功删除、永久失败归档和损坏文件隔离。
- 扩展 `integrations/hub_client.py`，对可重试失败保存当前及剩余 Envelope，并提供独立 Outbox 补传协调入口。
- 扩展 `scripts/boss_cdp_raw.py`，新增不初始化 Chrome、不访问 BOSS 的 `--flush-outbox` 命令。
- 新增 Outbox 单元测试，并补充 Hub Client 与任意工作目录 CLI 子进程回归测试。
- 更新 Collector README、集成说明和项目当前状态，记录目录、失败分类、补传命令及安全边界。
- 缺陷修复：`scrape_list()` 最终返回对象与最终落盘文件共享同一个 `scraped_at` 及采集上下文，避免新采集完成后因内存对象缺少必填时间而在进入 Outbox 前触发 `BossMappingError`。
- 增加新采集到 Hub 交接回归测试，验证返回时间与落盘时间一致，并验证 Hub 503 时生成 `pending` Outbox 文件。

## 验证记录

- `python -m py_compile integrations/outbox.py integrations/hub_client.py scripts/boss_cdp_raw.py tests/test_outbox.py tests/test_hub_client.py tests/test_script_entrypoint.py`
- `python -m unittest tests.test_outbox tests.test_hub_client tests.test_script_entrypoint`：初始专项 37 项通过。
- `python -m unittest tests.test_raw_response_capture tests.test_outbox tests.test_hub_client tests.test_script_entrypoint`：缺陷修复后相关回归 49 项通过。
- `$env:PYTHONUTF8='1'; python -m unittest discover -s tests`：共运行 143 项；TASK-008 及本次缺陷回归测试全部通过，保留 5 项既有 Windows 路径兼容基线失败，本次修复未新增失败。
