# Collectors 开发规则

## 一、模块职责

`collectors/` 下的项目负责从外部信息源获取原始数据。

采集器只负责：

1. 访问信息源。
2. 获取并本地保存原始数据。
3. 映射为统一 InformationEnvelope。
4. 通过 HTTP 提交 Information Hub。
5. 提交失败时保证数据不丢失。

## 二、架构边界

采集器不得：

- 直接连接 Information Hub 的 MySQL；
- 承担 AI 分析或用户推荐；
- 直接发送短信或微信通知；
- 依赖其他采集器内部实现。

协议文档：

`../docs/contracts/information-envelope-v1.md`

## 三、数据规则

- 原始业务 payload 完整保留。
- publishTime 未知时传 null。
- 不得将 collectedAt 当作 publishTime。
- sourceItemId 必须使用来源稳定 ID。
- 本地保存成功后再尝试提交后端。
- 后端失败不得导致已采集数据丢失。
- 缺失字段传 null 或省略，不得伪造。
- 不得用空值主动覆盖服务端已有数据。

## 四、安全

不得提交：

- `.env`
- Cookie
- Token
- Chrome Profile
- 登录缓存
- 真实采集结果
- 用户隐私数据

仓库只保留 `.env.example` 和脱敏样例。

## 五、代码要求

- 采集核心与 Mapper、HTTP Client、Outbox 分离。
- 网络调用必须设置超时。
- 不允许无限重试。
- 错误日志必须可定位但不得泄露凭证。
- 重要字段映射必须有测试。
