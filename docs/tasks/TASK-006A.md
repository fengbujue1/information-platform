# TASK-006A：增加 BOSS 搜索接口原始响应诊断输出

状态：DONE
所属阶段：Phase 1
优先级：P0
负责人：User + Codex

## 1. 目标

在不改变现有 BOSS 搜索、分页、详情抓取和结构化输出行为的前提下，为职位搜索接口增加可选的原始响应诊断能力。

启用诊断后：

- 在终端输出 BOSS 职位搜索接口返回的原始响应体；
- 将每次搜索接口调用的原始响应体保存到 Collector 的 `result/job-result/raw-responses/`；
- 为后续核对来源字段和优化采集逻辑提供可复现样本。

## 2. 背景

当前 `boss_cdp_raw.py` 在页面内调用：

```text
/wapi/zpgeek/search/joblist.json
```

JavaScript 会立即解析 `xhr.responseText`，只把选定字段转换为职位列表返回给 Python。原始响应中的完整字段结构没有独立保留，后续无法准确判断是否存在尚未映射的稳定字段。

TASK-006 将基于当前结构化列表和详情输出构建 InformationEnvelope V1。开始字段映射前，需要先提供受控的原始响应观察能力，避免为了补字段反复修改采集逻辑。

## 3. 前置依赖

- TASK-001 已完成。
- BOSS Collector 当前列表接口可以正常返回数据。
- `collectors/boss-zhipin-scraper/AGENTS.md` 的采集核心保护规则继续有效。

## 4. 影响范围

- `collectors/boss-zhipin-scraper/scripts/boss_cdp_raw.py`
- `collectors/boss-zhipin-scraper/tests/`
- `collectors/boss-zhipin-scraper/README.md`
- `collectors/boss-zhipin-scraper/.gitignore`（仅在现有忽略规则不能覆盖诊断目录时修改）
- `docs/tasks/TASK-006A.md`
- `docs/CURRENT_STATUS.md`

## 5. 本任务范围

1. 为 BOSS 职位搜索接口增加显式诊断开关，默认关闭。
2. 在页面内 XHR 返回后、字段筛选和结构化转换前，取得完整 `xhr.responseText` 和 HTTP 状态码。
3. 启用诊断时，在终端使用清晰的开始和结束标记输出原始响应体。
4. 启用诊断时，为每次接口请求保存一份原始响应体文件。
5. 默认诊断目录：

   ```text
   result/job-result/raw-responses/
   ```

6. 文件名至少区分：
   - 本次运行；
   - 页码；
   - HTTP 状态码；
   - 同页重复请求序号（如存在）。
7. 合法 JSON 响应保存为 `.json`；无法解析为 JSON 的响应仍按原始文本保存为 `.txt`。
8. 原始响应保存失败时给出明确错误，不得悄悄丢失；是否中止当前采集由实施前计划明确。
9. 诊断关闭时，现有终端输出、结构化 JSON/CSV 和采集行为保持不变。
10. 增加自动化测试并更新使用说明。

## 6. 不在本任务范围

- 不修改 InformationEnvelope V1 字段映射。
- 不实施 TASK-006 BOSS Mapper。
- 不实施 Hub Client、Outbox 或失败补传。
- 不修改 Information Hub 后端或数据库。
- 不改变 BOSS 请求频率、分页策略、去重规则和详情提取规则。
- 不抓取或保存 HTTP 请求头、响应头、Cookie、Authorization、浏览器凭证或 Chrome Profile。
- 不把原始响应样本提交到 Git。
- 不把诊断原始响应发送到 Information Hub。
- 不将浏览器网络层压缩字节、TLS 报文或完整 HTTP 会话定义为本任务的“原始响应”。

## 7. 业务与技术规则

### 7.1 原始响应定义

本任务中的“原始响应”仅指职位搜索 XHR 在浏览器中完成解压和解码后得到的：

```text
xhr.responseText
```

保存动作必须发生在字段选择和职位结构化转换之前。文件内容应保持响应正文语义，不得只保存当前 `jobList` 映射结果。

### 7.2 开关和兼容性

- 诊断必须由明确 CLI 参数启用，不得默认打印或保存。
- 未启用时不得创建原始响应目录或文件。
- 原有命令、默认行为、JSON/CSV 输出格式和退出码保持兼容。
- DOM fallback 不是 BOSS API 响应，不生成伪造的原始 API 响应文件。

### 7.3 文件规则

- 默认写入仓库内已忽略的运行结果目录。
- 使用 UTF-8 写入。
- 同一运行内不得覆盖前一页或前一次请求的文件。
- JSON 响应不得通过重新序列化替代原始 `responseText`；允许仅为判断扩展名做解析校验。
- 非 200 响应只要存在响应体，也应在诊断开启时保存，便于排查风控、登录失效和接口错误。
- 不在文件名中写入 Cookie、Token、`security_id`、`lid` 或完整响应内容。

### 7.4 安全边界

原始 BOSS 响应可能包含 `security_id`、`lid`、来源标识符或其他不适合中央存储的字段，因此：

- 诊断开关启用时必须在终端提示输出仅供本地调试；
- 诊断目录必须保持在 Git 忽略范围内；
- 日志和文件不得包含请求头、Cookie、Authorization 或浏览器凭证；
- 原始响应文件不得作为 TASK-006 的安全 `rawPayload` 直接提交；
- TASK-006 仍需按协议删除 `security_id` 和 `lid` 后才能提交 Hub；
- 文档应提示开发者按需清理本地诊断文件，避免长期积累。

### 7.5 失败语义

- BOSS 接口请求本身失败时，沿用现有采集失败或降级行为。
- 诊断输出不得把失败响应伪装成成功职位数据。
- 诊断写文件失败不得影响默认关闭场景。
- 诊断开启后的写入失败处理必须明确、可测试，并在实施记录中说明。

## 8. 验收标准

- [x] 新增显式原始响应诊断 CLI 开关，默认关闭。
- [x] 诊断关闭时，现有采集输出和测试保持不变。
- [x] 诊断开启时，终端打印完整职位搜索接口响应体。
- [x] 诊断开启时，每次搜索接口调用在 `result/job-result/raw-responses/` 生成唯一文件。
- [x] 保存内容来自结构化转换前的 `xhr.responseText`。
- [x] 多页请求不会相互覆盖。
- [x] 合法 JSON 和非 JSON 响应均可诊断保存。
- [x] 非 200 响应体可被诊断保存，且不会被当作职位列表。
- [x] 诊断文件不包含请求头、Cookie、Authorization 或 Chrome Profile。
- [x] 运行结果目录被 Git 忽略，仓库中没有新增真实原始响应样本。
- [x] 不改变请求频率、分页、去重、详情和现有 JSON/CSV 输出。
- [x] 增加诊断关闭、诊断开启、多页、非 200 和写入失败测试。
- [ ] Collector 全部现有自动化测试通过（当前仍有 5 项与本任务无关的 Windows/默认路径历史失败；与 `HEAD` 基线一致，未新增失败）。
- [x] README、CURRENT_STATUS 和本 TASK 文档更新。
- [x] 未提前实施 TASK-006、TASK-007 或 TASK-008。

## 9. 实施前计划

计划修改文件：

- `collectors/boss-zhipin-scraper/scripts/boss_cdp_raw.py`
- `collectors/boss-zhipin-scraper/tests/test_raw_response_capture.py`
- `collectors/boss-zhipin-scraper/README.md`
- `docs/tasks/TASK-006A.md`
- `docs/CURRENT_STATUS.md`

实施步骤：

1. 让页面内职位搜索 XHR 同时返回 HTTP 状态、原始 `responseText` 和现有精简职位数组。
2. 保持现有职位解析兼容，并增加原始响应解析对象。
3. 增加默认关闭的 `--capture-raw-response` 和可选 `--raw-response-dir`。
4. 开启后按运行 ID、页码、请求序号和状态码生成唯一文件，并在终端打印原始正文。
5. 诊断写入失败时抛出明确异常，停止后续请求，避免用户误以为已经完整采样；已抓取的结构化数据仍沿用现有收尾逻辑保存。
6. 更新 README、任务状态和 CURRENT_STATUS。

测试方案：

- 验证新旧页面求值结果均能解析。
- 验证诊断关闭时不打印、不创建目录和文件。
- 验证 JSON 原文按 UTF-8 原样写入 `.json`。
- 验证非 JSON、非 200 响应写入 `.txt` 且不产生职位数据。
- 验证多页或重复请求文件名唯一、互不覆盖。
- 验证目录创建或写入失败时抛出明确诊断异常。
- 运行 Collector 全部 `unittest`，并区分实施前已存在的环境或历史失败。

风险和待确认点：

- 原始响应可能包含来源安全和追踪字段，只允许保存在 Git 已忽略的本地目录，不能发送 Hub。
- 完整响应可能较大且终端输出较多，因此功能默认关闭并显示本地调试警告。
- 本任务只捕获职位搜索 XHR 响应体；详情当前来自页面提取，不伪装为 API 原始响应。
- 不采集请求头、响应头、Cookie、Token 或浏览器凭证。

## 10. 实施记录

完成时间：2026-07-27

实施内容：

- 在职位搜索 XHR 返回值中同时携带 HTTP 状态码、未经字段筛选的 `xhr.responseText` 和原有精简职位数组。
- 新增 `ApiSearchResponse` 解析对象，兼容任务实施前直接返回职位数组的页面求值格式；非 200 响应不会进入职位处理。
- 新增默认关闭的 `--capture-raw-response` 参数，以及覆盖保存目录的 `--raw-response-dir` 参数。
- 启用诊断时，按运行 ID、页码、请求序号和 HTTP 状态生成唯一文件；合法 JSON 使用 `.json`，其他正文使用 `.txt`。
- 原始正文使用 UTF-8 独占创建并原样写入，不重新序列化；写入失败抛出明确的 `RawResponseCaptureError` 并停止后续请求。
- 终端使用明确的开始/结束标记打印原始正文，并提示诊断文件仅限本地使用，不得提交 Git 或发送 Information Hub。
- 默认目录位于现有 Git 忽略范围 `result/job-result/raw-responses/`，未新增或提交真实响应样本。
- 未修改请求频率、分页、去重、详情提取、结构化 JSON/CSV 格式、Information Hub 后端或 TASK-006 映射逻辑。
- 更新 Collector README，补充启用命令、参数、文件规则和安全边界。

失败语义：

- 诊断关闭时不创建目录、不打印原始响应，保持原有行为。
- 诊断开启时，原始响应写入失败会输出明确错误并终止后续列表请求；已抓取的结构化数据仍执行现有收尾保存逻辑。
- 非 200 或非 JSON 响应会保留用于诊断，但不会被当作职位列表。

结果：TASK-006A 实现完成，可以进入 TASK-006。真实 BOSS 单页采样需要本地 9222 端口存在已登录的专用 Chrome，未作为代码完成的前提。

## 11. 测试结果

通过：

- `python -m py_compile scripts/boss_cdp_raw.py`
- `python -m unittest tests.test_raw_response_capture`：11 项全部通过。
- `python -m unittest tests.test_raw_response_capture tests.test_chrome_setup.ChromeSetupTests.test_api_extraction_keeps_detail_context_fields tests.test_chrome_setup.ChromeSetupTests.test_api_job_parser_rejects_error_rows tests.test_chrome_setup.ChromeSetupTests.test_dom_fallback_is_opt_in`：14 项全部通过。
- 覆盖新旧返回格式、默认关闭、JSON 原文、非 JSON、非 200、多请求唯一文件、写入失败、页面求值顺序、CLI 帮助和 Git 忽略规则。

完整回归：

- 当前工作区执行 `python -m unittest discover -s tests -p "test_*.py"`：87 项，82 项通过，5 项失败。
- 从 `HEAD` 导出的改动前 Collector 基线执行同一命令：76 项，71 项通过，5 项失败。
- 两次失败完全相同，均为 Windows 进程命令解析和默认目录预期的历史问题：
  - `test_chrome_process_parsing_matches_unquoted_user_data_dir`
  - `test_default_cdp_profile_is_persistent_and_not_default_or_tmp`
  - `test_default_result_dir_is_persistent_user_state`
  - `test_setup_can_skip_waiting_for_login`
  - `test_setup_reuses_ready_cdp_port_owned_by_dedicated_profile`
- TASK-006A 新增 11 项测试全部通过，完整回归未新增失败；上述历史测试修复不属于本任务范围。

真实联调：

- 尝试使用 `--capture-raw-response` 执行单页诊断时，本机 `127.0.0.1:9222` 没有可连接的专用 Chrome，流程在发起 BOSS 搜索请求前停止。
- 因此本轮没有生成真实原始响应文件，也没有任何真实响应数据进入 Git。

## 12. 遗留问题

- 当前详情内容来自详情页面提取，不属于本任务定义的职位搜索 API 原始响应。
- 原始响应中的字段在完成实际采样前不作为正式协议字段。
- 后续需要在已登录的专用 Chrome 可用时手工运行一次单页诊断，以观察真实响应；该步骤不阻塞 TASK-006A 代码完成。
- Collector 的 5 项历史 Windows/默认路径测试失败需另行建任务处理，不在 TASK-006A 内顺带修改。

## 13. 完成确认

- [x] CURRENT_STATUS 已更新
- [x] TASK-006 前置依赖已满足
- [ ] 用户已检查 git diff
- [ ] 用户已确认测试结果
- [ ] 已提交并 push
