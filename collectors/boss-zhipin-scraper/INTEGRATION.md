# Information Platform 集成说明

## 来源

Upstream:
eatmoreduck/boss-zhipin-scraper

Fork:
fengbujue1/boss-zhipin-scraper

## 当前验证环境

- Windows
- Python 版本：3.1
- Chrome 版本：未知
- 运行日期：20260723

## 当前验证结果

- Chrome CDP 启动成功
- BOSS 登录成功
- 职位列表采集成功
- 职位详情采集成功
- JSON 输出成功

## Monorepo 接入原则

不修改核心采集流程。
通过 integrations/ 增加 Information Hub 提交能力。