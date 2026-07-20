# 当前架构

状态：ACTIVE。全部业务内容为 `模拟数据`。

```text
AIOps（未来真实宿主） / Mock AIOps Sandbox
→ PrecheckContext / HostBridge → Embedded Precheck UI
→ Controller → API Mapper → Use Case → PrecheckWorkflowService
→ Session Repository / Policy / Completeness / Knowledge / Suggestion / Evidence / Audit Ports
→ 线程安全内存 Adapter 与确定性 Mock Adapter

本地模拟登录 UI → v2 AuthSession HTTP → AuthSession Use Case → IdentityContextPort
→ JDBC Adapter → 本地 Compose PostgreSQL（Flyway + local-identity-v1/local-catalog-v1）

知识上传 UI → v2 multipart + Cookie/CSRF/幂等键 → 不可变本地 volume
→ PostgreSQL KnowledgeDocument/KnowledgeVersion/ParseTask → 异步解析
→ 受角色、产品线和对象归属保护的摘要/Block/Chunk 预览

预诊 UI → 独立 Feedback / SubmissionContinuation 命令 → 各自事务与幂等记录
→ 不可变 AuditEvent → ADMIN 按授权产品线查看脱敏结构化元数据
```

应用层拥有 Session/Run、策略、完整度、Mock 检索、建议、Evidence 核验、下一步、状态和审计编排。Infrastructure 不依赖 HTTP DTO。初次预诊创建 Run 1，追问按 Session 递增 sequence；内存会话随进程重启清空，只保存不含正文的受控摘要。

前端单工程提供 `/sandbox`、`/precheck-v2`、`/embed`、`/knowledge` 与最小 `/audit` 页面。Sandbox 不是正式 SLA 入口；Embed 不复制宿主表单；反馈与继续提交相互独立，`CONTINUE_SUBMISSION` 由人工明确确认且不会创建 SLA 或工单内容。

Issue #24 进一步实现独立 Feedback、SubmissionContinuation 与结构化 AuditEvent；这三类记录持久化且数据库级不可变，两个命令使用独立事务、幂等键和失败边界。

本地 v2 已实现 AuthSession、知识首次上传、单任务查询、三个 parse-preview 读取端点、不可变草稿审核命令，以及持久化预诊 Session/Run 与完整度策略只读端点。预诊以 `sourceSystem + hostRequestId` 串行化业务幂等，Run 保存完整 Context、策略版本、授权检索审计、Evidence 与结果快照，并由数据库拒绝修改或删除；已使用的完整度策略版本同样由数据库拒绝原地修改或删除，确保版本引用可追溯。每个 Session 最多三次 Run，完整、完成或达到上限均不自动终止，只有负责人明确确认自助结束才进入终态。检索在排序前按当前 Context 产品线授权和当前 `PUBLISHED` 版本过滤，采用 FTS/pgvector 各 Top 20、固定 RRF `k=60`、最终 Top 5；本地 Embedding 不可用时降级为 `FTS_ONLY`，FTS 不可用时降级为无 Evidence 的 `UNAVAILABLE`，两者均限制为 `LOW` 且保留人工继续提交。Evidence 为不可变快照，每次读取都按当前身份重新鉴权，不暴露宿主路径或直接文件 URL。Cookie Session、业务状态和不可变历史持久化在本地 PostgreSQL；原始知识文件保存于 Compose volume；CSRF Token 仅通过响应头进入页面内存。`docs/api/openapi-v2.yaml` 仍是整体 DRAFT / PARTIALLY_IMPLEMENTED，只有 operation 级 `IMPLEMENTED` 条目可视为运行时存在；v1 无认证 Mock 行为保持不变。

尚未接入真实 AIOps、SSO、企业共享或生产数据库、真实业务数据、真实知识源、生成式 RAG/LLM、多 Agent、生产部署或真实 SLA 提交。当前对模拟、公开或脱敏知识实现本地 FTS + 固定 E5 向量双索引发布，以及授权优先的在线混合 Retrieval、不可变 Run/Evidence 快照和逐次 Evidence 重新授权读取。
