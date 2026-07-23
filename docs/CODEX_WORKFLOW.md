## 1. 每次打开项目后的恢复指令
请先恢复项目上下文，不要立即修改代码。

按顺序阅读：

1. 根目录 AGENTS.md
2. docs/PROJECT_CONTEXT.md
3. docs/ARCHITECTURE.md
4. docs/ROADMAP.md
5. docs/CURRENT_STATUS.md
6. CURRENT_STATUS 中指定的当前 TASK
7. 当前模块下的 AGENTS.md
8. 最近 5 条 Git 提交

然后执行：

git status
git log --oneline -5

请告诉我：

- 当前项目目标
- 当前阶段
- 已完成内容
- 当前任务
- 下一步最小任务
- 当前风险
- 准备修改哪些文件

不要修改代码，等待我确认。

## 2. 开始具体任务时
请实施 docs/tasks/TASK-XXX.md。

严格遵守：

- 只完成该任务
- 不实现任务范围之外的功能
- 不引入新的基础设施
- 不重写 BOSS 采集核心
- Controller 不写业务逻辑
- 所有数据库写入使用事务
- 增加相应测试
- 完成后更新 CURRENT_STATUS.md 和 TASK 文件

开始修改前，先列出：

1. 实施步骤
2. 计划修改的文件
3. 测试方案

等我确认后再修改。

##3. 让 Codex 审查代码
请对当前未提交修改进行代码审查，不要立即重写。

重点检查：

1. 是否违反 AGENTS.md
2. 是否超出当前 TASK 范围
3. 是否存在业务逻辑写入 Controller
4. 是否存在事务问题
5. 是否存在幂等问题
6. 是否可能丢失原始数据
7. 是否有密码、Token、Cookie 被提交
8. 测试是否覆盖重复提交和异常场景

请按照严重程度列出问题，并给出最小修改方案。

##4. 每次结束开发前
本轮开发准备结束，请执行交接收尾。

请完成：

1. 检查 git status 和 git diff
2. 运行当前任务相关测试
3. 更新 docs/CURRENT_STATUS.md
4. 更新当前 TASK 状态
5. 如产生架构决策，创建或更新 ADR
6. 记录本轮修改文件
7. 记录测试命令和测试结果
8. 明确下一步最小任务
9. 不提交 Git，等待我检查

最后汇报：

- 修改了什么
- 测试是否通过
- 有什么遗留问题
- 下一台电脑应该从哪里继续

##5.让 Codex自动生成任务文件的完整提示词
请为当前项目进行需求拆分和任务规划。

先阅读：

- AGENTS.md
- docs/PROJECT_CONTEXT.md
- docs/ARCHITECTURE.md
- docs/ROADMAP.md
- docs/CURRENT_STATUS.md
- docs/DATABASE_DESIGN.md
- collectors/boss-zhipin-scraper 的实际代码

当前需要规划的是 Phase 1：

BOSS采集器
→ Information Hub HTTP接口
→ MySQL幂等归档
→ 职位查询API

任务拆分原则：

1. 一个任务应具有单一主要目标。
2. 一个任务应当能够独立测试和独立提交。
3. 一个任务不要同时跨越后端、采集器和前端三个模块。
4. 明确前置依赖。
5. 明确本任务范围和不在范围。
6. 每个任务必须有可验证的验收标准。
7. 数据库结构由 Flyway 管理。
8. BOSS采集核心已经验证可运行，不得重写。
9. 当前不实现 Vue、AI、推荐、Kafka、MongoDB、微服务。
10. 任务粒度控制在个人开发约半天到两天。

请先输出：

- 任务列表
- 执行顺序
- 任务依赖关系
- 每项任务的目标
- 风险和待确认问题

本轮不要创建文件，不要修改代码，等待我确认。