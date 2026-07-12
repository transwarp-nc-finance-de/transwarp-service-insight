# 完整重构建议实施审计

Status: ACTIVE  
Owner: 技术负责人  
Last reviewed: 2026-07-12  
Source of truth for: 重构建议逐项完成度、证据与人工门禁

## 已完成且有实现证据

| 要求 | 状态 | 证据 |
| --- | --- | --- |
| 模块化单体与业务模块 | 已完成骨架 | `precheck`、`feedback`、`policy`、`audit`、`knowledge`；ADR-0001 |
| Controller 依赖 Use Case | 已完成 | `PrecheckController` 只依赖 `CreatePrecheckUseCase`、`ContinuePrecheckUseCase` |
| DTO 与领域模型分离 | 已完成 | HTTP DTO → Command → Domain Result → HTTP DTO |
| Port/Adapter | 已完成当前范围 | Precheck、Policy、Audit、Feedback、Knowledge 均有 Port 和 Mock/Memory Adapter |
| Session、Run、状态与 NextAction | 已完成领域/API 骨架 | `PrecheckSession`、`PrecheckRun`、`PrecheckStatus`、OpenAPI 字段 |
| 始终允许人工提交 | 已完成 | `allowedActions` 固定含 `CONTINUE_SUBMISSION`，前端/API 行为测试 |
| 反馈与业务事件 | 已完成 Mock 范围 | 反馈 API、采纳状态、继续提交事件和前端入口 |
| 模拟身份、策略和脱敏审计 | 已完成 Mock 范围 | `mock-user`、`mock-policy-v1`、内存审计不记录正文的测试 |
| 知识生命周期 | 已完成领域骨架 | 模拟文档/版本、审核发布状态、仅 PUBLISHED 可检索的测试 |
| 策略/规则/模型/索引追踪 | 已完成 Mock 语义 | API 返回策略版本、规则版本及明确的模型/索引不适用标识 |
| 当前/目标架构分离 | 已完成 | current、target、module-boundaries、data-model、在线/离线流程 |
| 关键 ADR | 已完成 | 模块化单体、Workflow-first、存储演进、OpenAPI 契约 |
| 工程门禁 | 已完成当前范围 | 后端格式/测试、前端 lint/格式/测试/构建、OpenAPI、Compose smoke |

## 部分完成

| 要求 | 当前状态 | 剩余工作 |
| --- | --- | --- |
| 多轮会话 | 页面内无状态 Mock | 持久化 Session/Run、并发与幂等保护需数据库授权后实施 |
| 状态机 | 领域状态与知识状态已有约束 | 在线工作流仍由确定性 Adapter 一次执行，尚无完整步骤级状态持久化 |
| 权限过滤 | 原则、策略 Port 与模拟快照已有 | 真实身份、组织、文档/Chunk 权限和检索前过滤未授权 |
| 可观测性与评估 | 事件模型和测试门禁已有 | 真实指标后端、评估集、成本和延迟观测未接入 |
| 数据保留删除 | ADR 与安全边界已声明 | 实际策略需数据负责人和安全负责人确认后随持久化实现 |

## 未实施且必须人工批准

- M5 PostgreSQL、迁移工具、Testcontainers 和持久化恢复；
- 真实文档导入、对象存储、解析、切片、索引和单一知识源；
- Embedding、pgvector、全文检索、Reranker 和真实 RAG 评估；
- LLM Gateway、真实 Prompt、外部模型和模型调用审计；
- 受控多 Agent 实验；
- AIOps、ITSM、历史 SLA、真实提交和生产部署；
- Redis、Elasticsearch/OpenSearch、消息队列或微服务拆分。

这些项目涉及真实数据、外部服务、数据库、生产环境、权限或显著基础设施扩张。当前 AGENTS 规则要求明确人工确认，因此不得从本审计状态推断为已批准。

## 当前验证口径

完成声明必须同时具备代码/文档证据、对应行为测试、OpenAPI 一致性、全量构建和运行时冒烟。仅存在接口、类名、绿色 CI 或文档描述不能单独证明能力完成。
