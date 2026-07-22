# BOSS Zhipin Scraper 开发规则

## 一、模块定位

本模块是 BOSS 职位信息采集器。

当前已验证的能力包括：

- 启动或连接真实 Chrome；
- 通过 CDP 使用登录状态；
- 请求 BOSS 职位数据；
- 分页采集；
- 保存本地 JSON 或 CSV；
- 根据职位 ID 去重。

本模块是 Information Platform 的第一个 Collector。

## 二、保护规则

除非当前 TASK 明确要求，否则不得重写或大规模修改：

- Chrome 启动逻辑；
- CDP 连接逻辑；
- BOSS 登录检测；
- BOSS API 请求逻辑；
- 翻页逻辑；
- 现有数据去重逻辑；
- JSON/CSV 本地输出逻辑。

接入 Information Hub 时，应优先新增独立适配层，不侵入采集核心。

推荐目录：

integrations/
├── config.py
├── information_mapper.py
├── hub_client.py
└── outbox.py

##三、接入 Information Hub 的执行顺序

正确顺序：
完成当前页或当前批次采集
→ 本地文件保存成功
→ 映射为 InformationEnvelope
→ 提交 Information Hub
→ 失败时写入 Outbox
禁止：
先提交后端
→ 后端失败
→ 放弃本地保存

Information Hub 不可用时，采集任务本身仍应继续运行。

##四、字段映射规则

字段映射依据优先级：

当前采集器实际返回数据；
脱敏样例；
协议文档；
数据库设计文档。

不得根据字段名称猜测不存在的数据。

需要区分：

通用 Information 字段；
Job 扩展字段；
仅保存在 rawPayload 中的来源专有字段。

未知发布时间：
"publishTime": null
不得使用 collectedAt 填充 publishTime。

##五、输出兼容性

修改前后必须保证：

原有命令仍可运行；
原有 JSON 输出仍存在；
原有 CSV 输出不被无意删除；
原有字段含义不发生静默变化；
Hub 提交功能可以通过配置关闭；
Hub 不可用不会导致采集器崩溃。
##六、配置

Information Hub 相关配置使用环境变量，例如：

INFORMATION_HUB_ENABLED=false
INFORMATION_HUB_URL=http://localhost:8080
INFORMATION_HUB_TOKEN=replace-me
INFORMATION_HUB_TIMEOUT_SECONDS=15
INFORMATION_HUB_OUTBOX_DIR=./data/outbox

环境变量名称以当前协议文档和代码实际配置为准。

不得把真实 Token 写入代码。

##七、测试要求

修改字段映射时，至少测试：

正常职位数据；
可选字段缺失；
sourceItemId 缺失；
publishTime 缺失；
skills 为空；
rawPayload 完整保留。

修改 Hub Client 时，至少测试：

2xx 成功；
4xx 响应；
5xx 响应；
请求超时；
连接失败。

修改 Outbox 时，至少测试：

写入失败记录；
重试成功；
重复补传；
损坏文件处理。

##八、命令

在填写启动和测试命令前，必须从当前仓库实际文件确认。

不得虚构命令。

确认后在此记录：

创建虚拟环境：
<实际命令>

安装依赖：
<实际命令>

运行测试：
<实际命令>

启动采集：
<实际命令>
##九、完成任务前

必须确认：

原有采集流程仍然可以运行；
本地结果可以正常生成；
Hub 关闭时功能不受影响；
Hub 失败时数据进入 Outbox；
测试通过；
未提交 Cookie、Profile 和真实采集数据。

其中“命令”部分暂时可以保留占位，让 Codex先检查项目真实入口后再填写，避免写错。
