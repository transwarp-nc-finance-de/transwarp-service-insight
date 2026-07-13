# AIOps 宿主与工作流重构审计

状态：ACTIVE。

| 检查项 | 当前事实 |
| --- | --- |
| 产品边界 | AIOps 是宿主和最终提交主体；Service Insight 是辅助预诊能力 |
| 前端 | Sandbox、Embed、PrecheckPanel、HostBridge 已进入代码 |
| 协议 | postMessage v1.0 骨架，配置化 origin，未接真实 AIOps |
| 工作流 | PrecheckWorkflowService 在 Application 层编排细粒度 Port |
| Session/Run | 线程安全内存 Repository；Run sequence 递增；重启清空 |
| API | 旧接口兼容；新增 session 创建、run 创建和状态查询 |
| 降级与提交 | 结构化降级；继续提交与反馈解耦且始终允许 |
| 依赖边界 | ArchUnit 验证 domain/application/infrastructure/api 方向 |
| 数据边界 | 只允许模拟、公开或脱敏输入，不保存真实敏感正文 |

数据库、真实知识导入、RAG、LLM、身份、真实 AIOps 和多 Agent 仍为 `NOT STARTED`，不属于本轮实现。
