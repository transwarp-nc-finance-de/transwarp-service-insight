# ADR-0006：一期新语义通过 API v2 演进

Status: ACCEPTED
Owner: API 负责人
Last reviewed: 2026-07-16
Source of truth for: 一期重定义后的 HTTP API 兼容方向

## 决策

保留当前 `/api/v1` 的字段、语义和行为，重定义后的一期能力使用 `/api/v2`。v2 承载宿主无关 `PrecheckContext`、独立 Feedback 与 SubmissionContinuation、知识治理、授权引用和评估能力；v1 与 v2 DTO 均通过 Mapper 进入应用用例，领域模型不依赖 HTTP 版本。

## 原因与影响

当前 v1 将反馈与继续提交耦合，并且无法表达已冻结的 Context、权限和幂等语义；在 v1 内收紧必填字段会破坏已发布契约。该决定落实 ADR-0004 的版本化要求，但不代表 v2 已实现，也不修改当前 OpenAPI。具体 v2 路径与 Schema 已形成 DRAFT 契约并通过人工评审；v1 弃用周期仍未确定。

v2 OpenAPI 已完整定义资源与命令方法、请求/响应 Schema、错误码、幂等语义、分页、异步任务状态以及 v1→v2 映射，并于 2026-07-14 获人工批准。API v2 为 `APPROVED_FOR_IMPLEMENTATION`，一期实施为 `READY_FOR_IMPLEMENTATION`；当前已实现 AuthSession、知识上传解析预览、不可变草稿审核、双索引原子发布/废弃及持久化 Precheck Session/Run，v1 Mock 保持兼容；在线检索、Evidence、反馈、评估等其余领域确认结论或已批准契约不得表述为已实现 API。
