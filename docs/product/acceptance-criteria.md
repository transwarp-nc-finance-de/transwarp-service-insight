# 当前验收标准

状态：ACTIVE。

- AIOps 是 SLA 表单、枚举、原有校验和最终提交主体；Service Insight 不创建 SLA。
- `/sandbox` 是本地 Mock AIOps；`/embed` 不显示完整 SLA 表单；PrecheckPanel 可复用。
- HostBridge 和 postMessage v1.0 校验 origin、version、requestId、大小与重复初始化。
- 应用层拥有预诊工作流；Infrastructure 不依赖 HTTP DTO；ArchUnit 规则真实执行。
- 初次预诊创建 Session/Run 1，追问创建递增 Run；无效 Session 返回结构化错误。
- 所有结果包含模拟依据、置信度原因、人工介入建议、待补充信息、结构化降级和 `CONTINUE_SUBMISSION`。
- 置信度必须由版本化确定性规则计算并展示理由：无证据、权限过滤后无证据、关键信息缺失或 Embedding 降级不得高于 `LOW`；单一证据、检索路径不一致或仍需补充信息不得高于 `MEDIUM`；`HIGH` 必须同时满足完整性通过、证据充分、FTS 与向量结果相互印证且无降级。
- 反馈失败、预诊失败和用户未采纳均不阻断 AIOps 原流程人工提交。
- OpenAPI、Java DTO、TypeScript 类型和行为测试一致；后端、前端、Compose 与浏览器冒烟通过后方可交付。
- 仅接本地 Compose PostgreSQL/pgvector 保存模拟身份、目录、AuthSession、知识治理、预诊、Evidence、Feedback、SubmissionContinuation 与结构化 AuditEvent；不接真实 AIOps、企业共享/生产数据库、生成式 RAG/LLM、多 Agent、真实数据或真实 SLA 提交。

## 一期范围重定义后的目标验收门禁

以下为 `CONFIRMED` 的目标标准，不代表当前实现已经满足：固定评估集使用不少于 30 条 `模拟数据` 样例；权限泄漏率必须为 0%，引用错误率必须为 0%，降级场景通过率必须为 100%，Recall@5 必须不低于 80%。评估结果必须注明“小样本工程评估，不代表生产效果”。

契约门禁已完成：`/api/v2` OpenAPI 的方法、请求/响应 Schema、错误码、幂等、分页、异步任务状态和 v1→v2 映射已于 2026-07-14 获人工批准。API v2 为 `APPROVED_FOR_IMPLEMENTATION`，一期实施为 `READY_FOR_IMPLEMENTATION`；当前已实现 AuthSession、知识上传/治理/发布、持久化 Precheck Session/Run、授权混合 Retrieval 与 Evidence、独立 Feedback、SubmissionContinuation 和结构化 AuditEvent，v1 Mock 保持兼容；Evaluation、Metrics、Admin Reset 等未标记 `IMPLEMENTED` 的目标不得被表述为已实现接口。

混合检索必须在两路过滤后各取前 20 条，使用版本化 RRF（`k=60`）融合并稳定返回前 5 条；相同输入、索引与规则版本必须产生相同顺序。

一期目标必须能通过一次 Compose 命令启动 `frontend`、`backend`、`PostgreSQL/pgvector` 与 `local-embedding`，并挂载原始文件数据卷；不要求 Redis、消息队列、MinIO、生成模型服务或监控大屏。

默认完整模式必须等待四个服务健康且是唯一可通过完整验收的模式；显式降级 profile 必须证明 Embedding 不可用时仍可使用 FTS 完成预诊并继续人工流程。Embedding 恢复不得改写历史 Run。

每条预诊引用必须可追溯到不可变的 KnowledgeVersion 与 Chunk，并能核验受控摘录和内容哈希；打开引用时重新授权。不得返回宿主机路径或绕过当前权限读取历史证据。

切片必须优先保持标题、段落和 PDF 文本块边界；超长结构块使用最大 400 Token、重叠 50 Token 的版本化窗口。重新切片不得改变历史 Run 已引用的 Chunk。

Sandbox 必须验证 `sourceSystem + hostRequestId` 的重复请求复用与不同上下文冲突行为；幂等不得阻止使用新 ID 主动重新预诊。

一期必须支持版本化 `模拟数据` 初始化和仅限本地环境的受控重置；重置要求 `ADMIN` 二次确认与审计。不验收备份/恢复演示，也不得宣称具备生产恢复能力。
