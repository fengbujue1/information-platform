# Information Platform 集成说明

## 来源

Upstream：

`eatmoreduck/boss-zhipin-scraper`

Fork：

`fengbujue1/boss-zhipin-scraper`

Monorepo：

`collectors/boss-zhipin-scraper`

## 当前验证环境

- 操作系统：Windows
- Python 版本：待本机执行 `python --version` 后填写
- Chrome 版本：待执行 `chrome://version` 后填写
- 验证日期：2026-07-23

不要把不完整的 `3.1` 当作 Python 版本。

## 当前验证结果

- Chrome CDP 启动成功
- BOSS 登录成功
- 职位列表采集成功
- 职位详情采集成功
- JSON 输出成功

## 当前输出注意事项

- `encrypt_job_id` 是职位稳定 ID 候选。
- `boss_name` 当前来自 `brandName`，实际表示公司品牌，不是招聘者姓名。
- `boss_title` 表示招聘者职位。
- `tags` 当前混合经验和学历。
- `skills`、`job_labels`、`welfare` 当前是 ` | ` 分隔字符串。
- 当前没有稳定输出招聘者姓名和活跃状态。
- 当前没有可信的来源发布时间。

## Monorepo 接入原则

1. 不重写 Chrome、CDP、登录、BOSS 请求和翻页核心。
2. 通过独立 `integrations/` 模块增加：
   - Mapper
   - Hub Client
   - Outbox
   - 配置
3. 本地文件保存成功后，再尝试提交 Hub。
4. Hub 不可用时不得中止采集。
5. 保持原有 JSON/CSV 输出兼容。
