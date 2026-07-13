# 当前架构

状态：ACTIVE。全部业务内容为 `模拟数据`。

```text
AIOps（未来真实宿主） / Mock AIOps Sandbox
→ PrecheckContext / HostBridge → Embedded Precheck UI
→ Controller → API Mapper → Use Case → PrecheckWorkflowService
→ Session Repository / Policy / Completeness / Knowledge / Suggestion / Evidence / Audit Ports
→ 线程安全内存 Adapter 与确定性 Mock Adapter
```

应用层拥有 Session/Run、策略、完整度、Mock 检索、建议、Evidence 核验、下一步、状态和审计编排。Infrastructure 不依赖 HTTP DTO。初次预诊创建 Run 1，追问按 Session 递增 sequence；内存会话随进程重启清空，只保存不含正文的受控摘要。

前端单工程提供 `/sandbox` 与 `/embed`。Sandbox 不是正式 SLA 入口；Embed 不复制宿主表单；反馈与继续提交相互独立，`CONTINUE_SUBMISSION` 始终允许。

尚未接入真实 AIOps、身份、数据库、知识导入、RAG、LLM、多 Agent、生产部署或真实 SLA 提交。
