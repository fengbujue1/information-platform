# Codex 协作流程

## 1. 每次打开项目后的恢复指令

发送给 Codex：

```text
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
```

## 2. 开始具体任务

```text
请实施 docs/tasks/TASK-XXX.md。

严格遵守：

- 只完成该任务
- 不实现任务范围之外的功能
- 不引入新的基础设施
- 不重写 BOSS 采集核心
- Controller 不写业务逻辑
- 数据库写入使用明确事务
- 增加相应测试
- 完成后更新 CURRENT_STATUS.md 和 TASK 文件
- 有新增文件需要执行 git add

开始修改前，先列出：

1. 实施步骤
2. 计划修改的文件
3. 测试方案
4. 风险和待确认点

等待我确认后再修改。
```

## 3. 审查未提交代码

```text
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
9. 文档是否和实际代码一致
10. 是否存在新增文件为列入git追踪

请按照严重程度列出问题，并给出最小修改方案。
```

## 4. 每次结束开发前

```text
本轮开发准备结束，请执行交接收尾。

请完成：

1. 检查 git status 和 git diff
2. 运行当前任务相关测试
3. 更新 docs/CURRENT_STATUS.md
4. 更新当前 TASK 状态和实施记录
5. 如产生架构决策，创建或更新 ADR
6. 记录修改文件
7. 记录测试命令和测试结果
8. 明确下一步最小任务
9. 不提交 Git，等待我检查

最后汇报：

- 修改了什么
- 测试是否通过
- 有什么遗留问题
- 下一台电脑应该从哪里继续
```

## 5. 两台电脑切换

离开当前电脑前：

```powershell
git status
git diff
git add .
git commit -m "wip: checkpoint current task"
git push
```

另一台电脑开始前：

```powershell
git fetch --all
git switch dev
git pull --rebase
git status
```

不要只依赖 `git stash`，因为 stash 默认不会同步到另一台电脑。

## 6. 需求变成或者新增
这是一次范围变更，请不要直接修改当前任务的实现范围。

当前任务：
TASK-004，原目标是创建 Phase 1 数据库结构

新增需求：
1. 我现在决定使用远程服务器的mysql容器的形式进行开发。
2. 生成可施行的mysql容器的生成指令
3. 需要解决连接安全问题

新增原因：
因为这样可以在不同的主机进行开发的时候都使用同一套数据环境

影响范围：
可能影响代码、部署、数据库、协议、测试或各种工程记录问的那个

新任务命名规范：
1. 如果是TASK-004的前置需求，建议命名为TASK-004A
2. 如果有多个，按照TASK-004B,TASK-004C这样的格式顺序命名



请先判断：

1. 该需求是否属于当前 TASK。
2. 是否应该创建新的独立 TASK。
3. 是否需要创建 ADR。
4. 原 TASK 是否需要增加前置依赖。
5. ROADMAP 和 CURRENT_STATUS 应如何更新。
6. ARCHITECTURE、PROJECT_CONTEXT 等文件是否需要更新
7. 是否会引入新的安全、部署或兼容性风险。
8. 如果这次新建有多个任务，他们之间的前置依赖也需要写明在相关的文档中

本轮先做计划和文档，不要立即实现。

请先给出：

- 任务拆分建议
- 新任务名称
- 前置和后续依赖
- 计划修改文件
- 验收标准
- 风险
- 不在范围的内容

等待我确认后再修改。
