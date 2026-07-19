# 领域数据模型

Status: DRAFT  
Owner: 后端负责人  
Last reviewed: 2026-07-16
Source of truth for: 领域概念及未来持久化语义

- `PrecheckSession`：一次预诊会话及当前状态；当前仅有内存中的页面会话语义。
- `PrecheckRun`：会话中的单次执行，包含独立 ID、轮次和状态。
- `PrecheckContext`：一期冻结的宿主无关问题快照语义，包含来源系统、宿主请求 ID、表单 Schema 版本、问题类型、产品线/产品/组件/版本、问题级别、服务类型、标题、纯文本描述、补充信息、影响范围和附件元数据；HTTP DTO 不是该领域概念的事实来源。
- `PrecheckContext` 最小有效条件：`sourceSystem`、`hostRequestId`、`formSchemaVersion`、`issueType`、`productLine`、`title`、`descriptionPlainText` 必须存在；其他字段可由完整度策略追问。
- `AdditionalInformationItem`：问题类型专属补充条目，包含稳定字段编码、显示名和纯文本值。未知编码可随 Run 快照保留，但不参与完整度判断，也不能影响权限或策略。
- `PrecheckIdempotency`：以 `sourceSystem + hostRequestId` 唯一关联规范化上下文哈希与首个 Session/Run。相同哈希返回既有结果，不同哈希产生冲突审计，不覆盖原记录。
- `Evidence`：一期目标中绑定不可变 KnowledgeVersion 与 Chunk，包含文档标题、版本 ID、Chunk ID、受控摘录和内容哈希；不等同于最终结论，也不保存本地文件路径。
- `KnowledgeDocument` / `KnowledgeVersion`：已实现只接受模拟、公开或脱敏数据的 PostgreSQL 持久化切片；版本携带权限范围与生命周期，并已实现不可变草稿修订、送审、退回、批准、双索引原子发布与废弃。授权在线混合检索只使用当前 `PUBLISHED` 版本，并为每个 Run 保存不可变检索审计与 Evidence 快照。
- `ParseTask`：已实现的文件解析任务，通过版本 ID 关联 `KnowledgeVersion`，独立记录 `PENDING`、`RUNNING`、`SUCCEEDED`、`FAILED`、attempt、错误分类和下次重试时间；不承载人工审核结论。瞬时错误指数退避且总尝试次数最多 3 次，确定性错误不自动重试，backend 启动时恢复未完成任务。
- `KnowledgeVersion` 发布语义：发布后内容不可变；修改创建新版本。新版本发布时原子废弃旧版本，同一 `KnowledgeDocument` 最多一个版本满足可检索条件，历史版本及其引用标识继续保留。
- `KnowledgeDraftRevision`：知识版本在 `DRAFT` 阶段的不可变修订，记录元数据、适用范围、清洗文本、解析警告说明及来源文件哈希。任一修订都会使旧 Chunk/索引失效；审核中修改先退回 `DRAFT`。
- `KnowledgeChunk`：从不可变 KnowledgeVersion 按版本化确定性规则生成，保留结构路径、顺序、受控文本、内容哈希、权限范围和切片规则版本；Chunk 继承文档产品线范围。
- `IndexTask`：为一个 `APPROVED` 版本构建 FTS 与向量索引的独立任务；只有两路索引都成功，才在同一事务中激活新 `PUBLISHED` 版本并废弃旧版本。失败不改变旧发布版本的可用性。
- `IndexTask` 记录 attempt、错误分类和下次重试时间。瞬时错误总尝试次数最多 3 次，确定性错误不自动重试；修复后创建新任务，历史失败任务不可变。
- `Feedback`：一期目标中只记录用户对预诊建议的反馈，不承载继续提交动作；采纳行为为 `ADOPTED / PARTIALLY_ADOPTED / IGNORED`，有用性评价为可选的 `HELPFUL / NOT_HELPFUL`，原因文本可选。
- `SubmissionContinuation`：一期目标中的独立用户确认，保存用户、时间、Session、幂等键和可选原因；只生成本地模拟事件，不创建 SLA、ticketId 或模拟工单。它与 Feedback 分别重试、幂等和审计。
- `AuditEvent`：一期目标中记录主体、Session/Run、动作、状态、策略/规则/Embedding/索引版本、Evidence ID、反馈、继续提交、错误与降级的不可变结构化元数据；不复制业务正文。

状态基线为 `RECEIVED → VALIDATED → COMPLETED`，允许分支 `NEED_MORE_INFORMATION`、`DEGRADED`、`FAILED`、`CANCELLED`。下一步动作是辅助建议，`CONTINUE_SUBMISSION` 必须始终存在。

一期目标约束每个 `PrecheckSession` 最多 3 个 `PrecheckRun`，sequence 只能为 1、2、3。达到上限不等于信息完整或建议已采纳；系统保留剩余待补充信息和人工继续提交动作。

`PrecheckRun` 完成只表示一次执行结束，不代表 `PrecheckSession` 已终止。Session 业务终止必须由用户明确确认，终止原因至少区分 `CONTINUED_SUBMISSION` 与 `SELF_SERVICE_CONFIRMED`；反馈记录不隐式改变 Session 终态。

每个 `PrecheckRun` 保存当轮完整规范化上下文快照和证据版本，而不是只保存增量补充文本。产品线、产品或组件变化创建新 Session；其他允许补充的字段变化创建下一 Run 并重新检索。历史快照与证据不可变。

每个 Run 还保存完整度策略版本、命中的通用/问题类型规则、缺失项和说明。完整度结论是辅助建议，不是继续提交门禁。

当前 API 为每次初始预诊返回相互独立的 `precheckId` 与 `sessionId`，并返回 `confidenceReason`、策略版本、Mock 规则版本以及明确的模型/索引不适用标识。当前无持久化，`PrecheckRun` 尚不对应数据库记录。

一期已确认使用 PostgreSQL 持久化全部业务状态，包括 Session、Run、Feedback、AuditEvent、KnowledgeDocument、KnowledgeVersion、ParseTask、KnowledgeChunk、索引任务和评估运行。当前数据库 Adapter 已实现 AuthSession、知识首次上传、ParseTask、解析预览，以及知识修订与不可变审核历史；原始文件保存在 Compose volume，不存为数据库大字段。其余内容仍是目标持久化语义。

未来需追踪策略、模型、Prompt 和索引版本，并经人工确定数据保留与删除策略。本文不代表全部目标持久化已经启用。

当前 v1 `FeedbackRequest` 同时包含 `adoptionStatus` 与 `continuedSubmission`，与上述目标领域模型存在差异。当前 v1 契约保持不变；独立 Feedback 与 SubmissionContinuation 已由 `APPROVED_FOR_IMPLEMENTATION` 的 API v2 DRAFT 表达。

当前 v1 也没有独立的有用性评价字段且保持兼容；该可选维度已由 `APPROVED_FOR_IMPLEMENTATION` 的 API v2 DRAFT 表达。
