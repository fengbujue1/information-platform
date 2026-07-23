# 上游仓库与同步关系

## BOSS Zhipin Scraper

上游仓库：

`https://github.com/eatmoreduck/boss-zhipin-scraper`

个人 Fork：

`https://github.com/fengbujue1/boss-zhipin-scraper`

Monorepo 目录：

`collectors/boss-zhipin-scraper`

导入方式：

Git subtree。

建议记录实际 remote 名称和分支：

```text
remote: boss-scraper
branch: dev
```

从 Fork 拉取更新：

```powershell
git subtree pull --prefix=collectors/boss-zhipin-scraper boss-scraper dev --squash
```

将 Monorepo 中采集器修改推回 Fork 前，应先检查提交范围，并避免把 Information Platform 专用文档或配置错误推回通用上游。

## 维护原则

1. Monorepo 是 Information Platform 开发的主要事实来源。
2. 不要同时在 Fork 和 Monorepo 中修改同一个功能后再手工覆盖。
3. 同步前保持主仓库 working tree clean。
4. 同步后重新运行采集器测试和 smoke test。
