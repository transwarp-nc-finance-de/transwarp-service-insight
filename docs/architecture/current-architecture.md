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
```

应用层拥有 Session/Run、策略、完整度、Mock 检索、建议、Evidence 核验、下一步、状态和审计编排。Infrastructure 不依赖 HTTP DTO。初次预诊创建 Run 1，追问按 Session 递增 sequence；内存会话随进程重启清空，只保存不含正文的受控摘要。

前端单工程提供 `/sandbox`、`/embed` 与最小 `/knowledge` 模拟上传预览页。Sandbox 不是正式 SLA 入口；Embed 不复制宿主表单；反馈与继续提交相互独立，`CONTINUE_SUBMISSION` 始终允许。

本地 v2 已实现 AuthSession、知识首次上传、单任务查询、三个 parse-preview 读取端点，以及不可变草稿修订、送审、退回和批准命令。Cookie Session、知识元数据、修订、任务、解析结果与不可变审核历史持久化在本地 PostgreSQL；原始文件保存于 Compose volume；CSRF Token 仅通过响应头进入页面内存。`docs/api/openapi-v2.yaml` 仍是整体 DRAFT / PARTIALLY_IMPLEMENTED，只有 operation 级 `IMPLEMENTED` 条目可视为运行时存在；v1 无认证 Mock 行为保持不变。

尚未接入真实 AIOps、SSO、企业共享或生产数据库、真实业务数据、真实知识源、知识发布与索引、RAG、LLM、多 Agent、生产部署或真实 SLA 提交。
