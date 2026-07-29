# TASK-018：建立 CI 与受控构建部署配置

状态：TODO  
所属阶段：Phase 2  
优先级：P1  
负责人：User + Codex

## 1. 目标

建立基础持续集成，并提供不会把无认证读取 API 直接暴露公网的前端构建和同源代理方案。

## 2. 背景

Phase 1 和 Phase 2 已包含 Java、Python 和 Vue 三类代码。仓库需要统一的自动检查。

前端部署还需要解决静态文件、深层路由和 `/api` 代理。

## 3. 前置依赖

- TASK-017 已完成。
- 所有前端 npm scripts 已稳定。

## 4. 影响范围

- `.github/workflows/`
- 前端构建配置
- `deploy/`
- Nginx 配置模板
- `.env.example`
- 部署文档
- TASK 和 CURRENT_STATUS

## 5. 本任务范围

CI 至少运行：

- Java 21 Maven 测试；
- Python Collector 测试；
- 前端 `npm ci`；
- 前端 typecheck；
- 前端测试；
- 前端 build；
- `git diff --check`；
- 基础敏感文件检查。

构建部署至少提供：

- 前端生产构建；
- SPA 深层路由回退；
- `/api` 同源反向代理模板；
- 配置模板；
- 健康检查说明；
- 受控访问说明。

## 6. 不在本任务范围

- 不部署到公网。
- 不配置域名和证书，除非用户单独创建部署任务。
- 不增加用户认证。
- 不部署生产数据库。
- 不引入 Kubernetes。
- 不重写现有 MySQL 部署。
- 不在 CI 中连接私人远程开发库。

## 7. 业务与技术规则

- CI 不依赖私人 SSH 密钥。
- CI 不依赖远程共享 MySQL。
- 数据库条件测试保持安全跳过，或后续单独引入 Testcontainers。
- 所有秘密使用 GitHub Secrets，仓库不保存真实值。
- Nginx 不添加 `Access-Control-Allow-Origin: *`。
- 未认证环境默认只允许本机或受控网络访问。

## 8. 验收标准

- [ ] Java 测试工作流通过
- [ ] Python 测试工作流通过
- [ ] 前端 typecheck、test、build 工作流通过
- [ ] SPA 深层路由可刷新
- [ ] `/api` 同源代理配置可校验
- [ ] 没有真实秘密进入 Git
- [ ] 没有公网无认证部署
- [ ] 部署文档明确访问边界
- [ ] CURRENT_STATUS 更新为 TASK-019

## 9. 实施前计划

Codex 必须先列出：

- 工作流拆分
- 运行命令
- 缓存策略
- Nginx 拓扑
- 不可在 CI 使用的秘密和远程资源
- 验证方法

## 10. 实施记录

待填写。

## 11. 测试结果

待填写。

## 12. 遗留问题

待填写。

## 13. 完成确认

- [ ] CURRENT_STATUS 已更新
- [ ] 用户已检查 git diff
- [ ] 用户已确认测试结果
- [ ] 已提交并 push
