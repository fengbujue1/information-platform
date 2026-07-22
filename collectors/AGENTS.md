# Collectors 开发规则

## 一、模块职责

`collectors/` 下的项目负责从外部信息源获取原始数据。

可能包含：

- 招聘平台采集器
- 新闻采集器
- 政府信息采集器
- 房价采集器
- 教育信息采集器

采集器只负责：

1. 访问信息源；
2. 获取原始数据；
3. 保留本地原始输出；
4. 转换为统一 InformationEnvelope；
5. 通过 HTTP 提交给 Information Hub；
6. 提交失败时保证数据不丢失。

## 二、架构边界

采集器不得：

- 直接连接 Information Hub 的 MySQL；
- 承担用户推荐逻辑；
- 承担 AI 分析逻辑；
- 直接发送短信或微信通知；
- 依赖另一个采集器的内部实现。

采集器和后端通过统一 HTTP 协议解耦。

协议文档：

- `../../docs/contracts/information-envelope-v1.md`

## 三、通用数据规则

1. 原始 payload 必须完整保留。
2. 不得将采集时间伪装成来源发布时间。
3. 来源发布时间未知时传 null。
4. sourceItemId 必须优先使用来源平台稳定 ID。
5. 不得随意生成随机 ID 代替来源业务 ID。
6. 所有协议必须携带 schemaVersion。
7. 提交后端失败不能导致已采集数据丢失。
8. 本地文件成功保存后，才允许尝试远程提交。

## 四、配置和安全

下列信息只能通过环境变量或本地配置提供：

- Cookie
- Token
- API Key
- Chrome Profile 路径
- Information Hub 地址
- Information Hub 认证信息

不得提交：

- `.env`
- Cookie 文件
- Chrome Profile
- 登录缓存
- 真实采集结果
- 用户隐私数据

只提交：

- `.env.example`
- 脱敏样例
- 配置说明

## 五、编码要求

- 每个采集器保持独立依赖和启动方式。
- 外部系统适配代码与数据提交代码分离。
- HTTP Client、Mapper、Outbox 不要写入采集核心。
- 网络调用必须设置超时。
- 外部接口失败必须输出可定位的错误。
- 不使用无限重试。
- 重要字段解析必须有测试。

## 六、完成任务前

必须：

1. 运行当前采集器测试；
2. 验证原有本地输出未被破坏；
3. 检查是否提交敏感数据；
4. 更新当前 TASK 和 CURRENT_STATUS；
5. 汇报实际执行过的命令和结果。