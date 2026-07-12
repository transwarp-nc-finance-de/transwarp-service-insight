# 领域数据模型

Status: DRAFT  
Owner: 后端负责人  
Last reviewed: 2026-07-12  
Source of truth for: 领域概念及未来持久化语义

- `PrecheckSession`：一次预诊会话及当前状态；当前仅有内存中的页面会话语义。
- `PrecheckRun`：会话中的单次执行，包含独立 ID、轮次和状态。
- `Evidence`：来源类型、标题、摘要、链接和 Mock 标识；不等同于最终结论。
- `KnowledgeDocument` / `KnowledgeVersion`：已实现只接受模拟数据的领域骨架；版本携带内容摘要、权限范围与生命周期，只有 `PUBLISHED` 可检索，不代表存在真实索引。
- `Feedback`：未来记录采纳、部分采纳、忽略、继续提交和人工原因。
- `AuditEvent`：未来记录主体、事件、策略版本、来源 ID、动作和降级，不记录敏感正文。

状态基线为 `RECEIVED → VALIDATED → COMPLETED`，允许分支 `NEED_MORE_INFORMATION`、`DEGRADED`、`FAILED`、`CANCELLED`。下一步动作是辅助建议，`CONTINUE_SUBMISSION` 必须始终存在。

当前 API 为每次初始预诊返回相互独立的 `precheckId` 与 `sessionId`，并返回 `confidenceReason`、策略版本、Mock 规则版本以及明确的模型/索引不适用标识。当前无持久化，`PrecheckRun` 尚不对应数据库记录。

未来需追踪策略、模型、Prompt 和索引版本，并经人工确定数据保留与删除策略。本文不代表已启用持久化。
