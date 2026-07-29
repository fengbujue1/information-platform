# TASK-016：实现历史快照查看

状态：TODO  
所属阶段：Phase 2  
优先级：P0  
负责人：User + Codex

## 1. 目标

实现职位历史版本列表和单个版本内容查看。

## 2. 背景

Phase 1 已保存不可变历史快照。Phase 2 需要把这些版本转化为可浏览界面。

## 3. 前置依赖

- TASK-015 已完成。

## 4. 影响范围

- `/jobs/:id/snapshots`
- 快照列表组件
- 快照详情组件
- JSON 只读展示
- 测试
- TASK 和 CURRENT_STATUS

## 5. 本任务范围

展示：

- 快照版本号；
- 快照创建时间；
- 采集时间；
- 标题；
- 正文；
- contentHash；
- standardizedPayload；
- collectorVersion。

实现：

- 快照 Loading；
- 无历史版本；
- 请求错误；
- 选择版本；
- 从快照返回当前详情；
- 直接访问快照路由。

## 6. 不在本任务范围

- 不查看 snapshot rawPayload。
- 不实现逐字 Diff。
- 不修改快照。
- 不删除快照。
- 不重新执行 AI。
- 不新增数据库字段。

## 7. 业务与技术规则

- 使用后端既定排序。
- standardizedPayload 只读格式化展示。
- JSON 展示必须处理超长内容。
- 快照为空不是错误。
- contentHash 可放在次要信息区。
- 页面不能假设版本号连续无缺口。

## 8. 验收标准

- [ ] 多版本正常显示
- [ ] 无快照状态正确
- [ ] 选中版本可查看内容
- [ ] 直接刷新路由可用
- [ ] standardizedPayload 可读
- [ ] 不返回或显示 rawPayload
- [ ] typecheck、test、build 通过
- [ ] CURRENT_STATUS 更新为 TASK-017

## 9. 实施前计划

Codex 必须先说明：

- 路由与组件结构
- 版本选择交互
- JSON 展示方案
- 大文本性能风险
- 测试场景

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
