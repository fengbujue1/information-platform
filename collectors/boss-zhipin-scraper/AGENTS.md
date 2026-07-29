# BOSS Zhipin Scraper 开发规则

## 一、模块定位

本模块是 BOSS 职位 Collector，已验证真实 Chrome、CDP、登录、分页、详情和本地输出可以运行。

## 二、保护区域

除非当前 TASK 明确要求，不得重写：

- Chrome 启动；
- CDP 连接；
- BOSS 登录检测；
- 搜索和详情请求；
- 翻页；
- 现有去重；
- JSON/CSV 本地输出。

接入 Information Hub 时通过独立 `integrations/` 模块实现。

## 三、推荐目录

```text
integrations/
├── config.py
├── information_mapper.py
├── hub_client.py
└── outbox.py
```

## 四、执行顺序

```text
采集
→ 本地保存成功
→ 映射 InformationEnvelope
→ 提交 Hub
→ 失败写 Outbox
```

Hub 不可用时，采集任务仍应继续。

## 五、字段规则

- `encrypt_job_id` 映射为 sourceItemId。
- `boss_name` 来自 `brandName`，表示公司品牌，不是招聘者姓名。
- `boss_title` 可以映射 recruiterTitle。
- recruiterName 当前没有稳定来源，通常为空。
- `boss_online=true` 时，`boss_online_observed_at` 映射为 recruiterActiveText；false 或缺失时本次映射为空。
- `tags` 使用规则识别经验和学历；无法识别时只保留 sourceTags。
- `skills`、`job_labels`、`welfare` 需要从分隔字符串转换为数组。
- rawPayload 保留安全清理后的原始业务字段，递归移除 security_id、lid 和凭证。
- publishTime 当前保持 null。

## 六、兼容性

修改后必须保证：

- 原有命令仍可运行；
- JSON/CSV 输出未被删除；
- Hub 功能可以关闭；
- Hub 失败不导致采集崩溃；
- 临时缺失 JD 不会要求服务端清空旧 JD。

## 七、测试

Mapper 至少覆盖：

- 正常数据；
- 字段缺失；
- 空字符串；
- 列表字段拆分；
- rawPayload 完整性；
- 错误的 boss_name 映射防护。

Client 至少覆盖：

- 2xx；
- 4xx；
- 5xx；
- 413；
- 超时；
- 连接失败。

Outbox 至少覆盖：

- 原子写入；
- 补传成功；
- 重复补传；
- 损坏文件隔离。
