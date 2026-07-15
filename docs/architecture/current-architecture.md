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
```

应用层拥有 Session/Run、策略、完整度、Mock 检索、建议、Evidence 核验、下一步、状态和审计编排。Infrastructure 不依赖 HTTP DTO。初次预诊创建 Run 1，追问按 Session 递增 sequence；内存会话随进程重启清空，只保存不含正文的受控摘要。

前端单工程提供 `/sandbox` 与 `/embed`。Sandbox 不是正式 SLA 入口；Embed 不复制宿主表单；反馈与继续提交相互独立，`CONTINUE_SUBMISSION` 始终允许。

本地身份切片只实现 `POST /api/v2/auth-sessions` 与 `GET/DELETE /api/v2/auth-sessions/current`。Cookie Session 持久化在本地 PostgreSQL，CSRF Token 仅通过响应头进入页面内存；四个身份、角色和产品线授权均为 `模拟数据`。`docs/api/openapi-v2.yaml` 继续作为整体 v2 DRAFT 标注 `NOT_IMPLEMENTED`，除上述 AuthSession 切片外的 v2 路径均未实现；v1 无认证 Mock 行为保持不变。

尚未接入真实 AIOps、SSO、企业共享或生产数据库、真实业务数据、知识导入、RAG、LLM、多 Agent、生产部署或真实 SLA 提交。
