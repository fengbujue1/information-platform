# TASK-006：实现 BOSS 字段映射器

状态：TODO  
所属阶段：Phase 1  
优先级：P0

## 前置依赖

TASK-002 完成。

## 目标

将 BOSS 列表和详情数据映射为 InformationEnvelope V1。

## 本任务范围

- 新建独立 integrations 映射模块。
- `encrypt_job_id` 映射 sourceItemId。
- `boss_name` 映射 companyName，不映射 recruiterName。
- 拆分 tags、skills、job_labels、welfare。
- 合并详情 JD。
- 保留完整 rawPayload。
- 增加缺失字段和异常数据测试。

## 不在范围

- 不发 HTTP 请求。
- 不改 Chrome/CDP 核心。
- 不做 AI 判断。

## 验收标准

- [ ] 映射与协议一致
- [ ] rawPayload 未丢字段
- [ ] 可选字段为空不报错
- [ ] 单元测试通过
