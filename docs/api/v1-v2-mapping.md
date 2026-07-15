# API v1 → v2 映射与迁移限制

Status: DRAFT（APPROVED_FOR_IMPLEMENTATION）

Owner: API 负责人

Last reviewed: 2026-07-15

Source of truth for: 已实现 v1 与部分实现 v2 DRAFT 契约之间的字段、状态、错误及迁移边界

Approval: 2026-07-14，用户以 API、产品、安全负责人身份明确批准进入实施

> **DRAFT / PARTIALLY_IMPLEMENTED / APPROVED_FOR_IMPLEMENTATION**：本文用于一期本地模拟闭环的契约实施边界。一期实施状态为 `READY_FOR_IMPLEMENTATION`；[`openapi.yaml`](openapi.yaml) 定义的 v1 Mock 继续作为兼容契约。[`openapi-v2.yaml`](openapi-v2.yaml) 已获准进入实施。当前实现 AuthSession，以及 `POST /api/v2/knowledge-documents`、`GET /api/v2/parse-tasks/{taskId}` 和三个 parse-preview 读取端点。其余 v2 operation 均为 `NOT_IMPLEMENTED`，当前切片不代表一期完成，也不接真实身份、生产数据库或外部系统。所有示例语义均为 `模拟数据`。

## 1. 兼容结论

- `/api/v1` 保持现有字段、必填性、状态码和耦合行为，不设置未经人工确认的下线日期。
- `/api/v2` 与 v1 并行承载新语义。v1 的 `precheckId` 不是 v2 主键；v2 以 `sessionId` 与 `runId` 标识会话和不可变执行快照。
- 旧客户端继续调用 v1。新客户端必须显式收集 v2 最小 Context；服务端不得用猜测值把不完整 v1 请求静默升级为 v2。
- Mapper 是未来实现边界，只负责 DTO 与应用用例之间的显式转换；本文不声明 Mapper 或 v2 运行时已存在。
- v2 不创建正式 SLA、工单草稿、`ticketId` 或提交回执。预诊建议不是最终根因、最终方案或正式复盘结论，人工继续提交始终可用。

## 2. 端点级映射

| v1 已实现入口 | v2 DRAFT 入口 | 映射结论 |
| --- | --- | --- |
| `POST /api/v1/precheck` | `POST /api/v2/precheck-sessions` | 非一一兼容。v2 创建 `PrecheckSession + Run 1`，要求完整最小 Context，并使用业务幂等键。 |
| `POST /api/v1/precheck-sessions` | `POST /api/v2/precheck-sessions` | 资源意图相近，但请求、状态码和返回模型不兼容；v1 成功为 `200`，v2 新建为 `201`、业务重放为 `200`。 |
| `POST /api/v1/precheck/follow-up` | `POST /api/v2/precheck-sessions/{sessionId}/runs` | v1 用 `precheckId + message`；v2 用 `sessionId + 完整 Context 快照 + Idempotency-Key`。不能只转换增量消息。 |
| `POST /api/v1/precheck-sessions/{sessionId}/runs` | 同名 v2 Run 集合入口 | 路径相似但语义不兼容。v2 强制完整快照、三轮上限、终态只读及新 Session 边界。 |
| `GET /api/v1/precheck-sessions/{sessionId}` | `GET /api/v2/precheck-sessions/{sessionId}` | v2 每次重新校验本人身份和产品线权限，并返回 v2 Session/Run 模型。 |
| `POST /api/v1/precheck/feedback` | `POST /api/v2/feedback` + `POST /api/v2/submission-continuations` | v1 的耦合请求必须拆成两个独立命令、幂等键和审计事件，不能用一次调用模拟原子事务。 |
| 无 | `POST /api/v2/auth-sessions` + `GET /api/v2/auth-sessions/current` + `DELETE /api/v2/auth-sessions/current` | **已实现**。v2 本地模拟 Cookie Session 通过响应头发放或恢复绑定当前 Session 的 CSRF Token，并支持退出；v1 没有认证或 CSRF 语义，不得反向映射。 |
| 无 | `POST /api/v2/knowledge-documents` + `GET /api/v2/parse-tasks/{taskId}` | **已实现**。仅支持模拟 Markdown/TXT/文本型 PDF 的不可变本地文件、草稿版本和异步解析任务；不包含修订、审核、发布或索引。 |
| 无 | `GET /api/v2/knowledge-versions/{versionId}/parse-preview` 及 `/blocks`、`/chunks` | **已实现**。提供受对象权限保护的解析摘要、规范化文本块与 Chunk 独立分页预览；固定按 `sequence ASC`，v1 没有等价资源。 |
| 无 | 除上述切片外的 v2 知识、任务、Evidence、评估、指标、审计、策略和本地重置资源 | 均为未实现 DRAFT 能力，不得从 v1 是否存在推断运行时可用。 |

## 3. 请求字段映射

### 3.1 `PrecheckRequest` → `PrecheckContext`

| v1 字段 | v2 字段 | 迁移规则 |
| --- | --- | --- |
| `context.sourceSystem`（可选） | `context.sourceSystem`（必填） | v2 仅允许 `AIOPS`、`SANDBOX`。缺失时不能自动升级。 |
| `context.hostRequestId`（可选） | `context.hostRequestId`（必填） | 与 `sourceSystem` 组成业务幂等键。缺失时不能生成猜测值。 |
| `context.formSchemaVersion`（可选） | `context.formSchemaVersion`（必填） | 缺失时不能自动升级。 |
| 无 | `context.issueType`（必填 `{code, displayName}`） | 一期编码固定为四种；v1 无来源，必须由新客户端收集。 |
| 无 | `context.productLine`（必填 `{code, displayName}`） | v1 的 `product` 不能推导产品线；必须显式收集并用于权限过滤。 |
| `title` | `context.title` | 仅字符串值可直接候选映射；仍需满足 v2 完整 Context。 |
| `description` | `context.descriptionPlainText` | 显式作为纯文本处理；不得携带真实或未脱敏数据。 |
| `product` | `context.product`（目录值） | v1 自由文本不能可靠推导稳定 `code`；需客户端选择目录值。 |
| `module` | `context.component`（目录值） | v1 自由文本不能可靠推导稳定 `code`。 |
| `version` | `context.version` | 可候选映射，但产品线/产品/组件变化仍决定是否需要新 Session。 |
| `severity` | `context.severity`（目录值） | v1 自由文本不能可靠推导稳定 `code`。 |
| `impactScope` | `context.impactScope` | 可候选映射。 |
| `attachmentsSummary` | 无直接映射 | 不能解析为 v2 附件。v2 只接受不透明 `attachmentId`、文件名、媒体类型和字节数，不接收路径、URL 或正文。 |
| 无 | `context.serviceType` | 可选目录值；不得从 v1 其他字段猜测。 |
| 无 | `context.additionalInformation[]` | 每项为 `{fieldCode, displayName, value}`。未知编码保留、参与幂等哈希，但不参与策略或权限判断。 |
| 无 | `context.attachments[]` | 必须由宿主或 Sandbox 显式提供脱敏模拟元数据。 |

v2 创建 Session 的七个最小字段是 `sourceSystem`、`hostRequestId`、`formSchemaVersion`、`issueType`、`productLine`、`title`、`descriptionPlainText`。v1 至少缺少 `issueType`、`productLine`，且三个 `context` 字段均可省略，因此不存在通用自动升级路径。

### 3.2 规范化与幂等

- v1 没有已发布的等价语义。v2 创建 Session 以 `sourceSystem + hostRequestId` 为业务幂等键。
- 语义哈希包含稳定业务编码、正文、补充信息和附件元数据；统一 Unicode 与换行、去除字符串首尾空白并稳定排序。
- 目录 `displayName` 不参与语义哈希。未知 `additionalInformation.fieldCode` 保留且参与哈希，但不参与策略或权限判断。
- 同一业务键且相同规范化上下文返回原 Session/Run、HTTP `200` 和 `Idempotency-Replayed: true`；不同上下文返回 `409 IDEMPOTENCY_CONTEXT_CONFLICT`，原记录不被覆盖。
- 后续 Run、Feedback、SubmissionContinuation、知识命令、评估和重置使用各自独立 `Idempotency-Key`，不能跨命令复用语义。

## 4. 响应字段映射

### 4.1 `PrecheckResponse` / `FollowUpResponse` → v2 Session 与 Run

| v1 字段 | v2 位置 | 说明 |
| --- | --- | --- |
| `precheckId` | 无主键映射 | 仅可作为迁移追踪元数据；不得作为 v2 `sessionId` 或 `runId`。 |
| `sessionId` | `PrecheckSession.sessionId` | 只有未来显式迁移程序确认同一领域记录时才可关联，不能仅因 UUID 形状相同就复用。 |
| `runId` | `PrecheckRun.runId` | 同上；v2 Run 是完整不可变 Context 与 Evidence 快照。 |
| `runSequence` | `PrecheckRun.sequence` | v2 只允许 `1..3`。v1 历史值超过 3 时不能直接导入活动 Session。 |
| `summary` / `reply` | `PrecheckResult.summary` | v2 明确要求 `模拟数据` 和免责声明。 |
| `recommendations` | `PrecheckResult.recommendations` | 辅助建议，不是最终方案。 |
| `references[]` | `PrecheckResult.evidence[]` | 仅在未来能授权解析为稳定 Evidence 时转换；不能凭 URL 自动生成。 |
| `confidence` | `PrecheckResult.confidence` | 枚举相同，但 v2 必须携带结构化理由并遵循确定性规则。 |
| `confidenceReason` | `PrecheckResult.confidenceReasons[]` | v1 单字符串可成为一个候选理由，但不能证明满足 v2 规则。 |
| `humanReviewRequired` | `humanInterventionAdvice[]` + `disclaimer` | v2 不用一个布尔值替代具体人工介入建议。 |
| `missingInformation[]` | `missingInformation[]` + `completeness` | v2 同时保存策略版本、缺失编码和命中理由。 |
| `fallbackReason` / `degradation` | `retrieval` | v2 分别记录 FTS 与 Embedding 状态，模式只允许 `HYBRID`、`FTS_ONLY`、`UNAVAILABLE`。 |
| `allowedActions` | `allowedActions` | v2 始终保留 `CONTINUE_SUBMISSION`；达到上限或降级也不得移除。 |
| `status` | `PrecheckRun.status` | 不映射为 Session 终止状态。 |
| `policyVersion` | `completeness.policyVersion` | v2 将完整度规则快照放入结果结构。 |
| `modelVersion` / `promptVersion` / `indexVersion` / `executionMetadata` | 无机械一一映射 | v2 一期没有生成式模型依赖；相关版本进入未来 Run/审计实现边界，不得据 v1 Mock 值伪造索引事实。 |

### 4.2 `ReferenceItem` → `Evidence`

v1 `ReferenceItem` 是 `sourceType + title + excerpt + url + mockData`。v2 Evidence 固定引用 `KnowledgeDocument`、不可变 `KnowledgeVersion` 和 `Chunk`，并包含受控摘录与内容哈希；读取只通过 `evidenceId`，每次重新授权，不返回本地路径或直接文件 URL。

因此：

- v1 `ReferenceItem.url` **不能**自动转换为 v2 Evidence，也不能把 URL 当作 `evidenceId`。
- 只有未来迁移工具能证明文档、版本、Chunk、内容哈希和当前权限范围时，才可创建显式关联。
- 无法证明的 v1 Reference 保持 v1 历史展示，不进入 v2 Evidence 候选、检索上下文或审计正文。
- v2 对“资源不存在”和“无权限”统一返回安全 `404 RESOURCE_NOT_FOUND`，避免资源枚举。

## 5. Session、Run 与状态映射

### 5.1 状态域分离

| 概念 | v1 | v2 DRAFT | 映射限制 |
| --- | --- | --- | --- |
| 预诊执行状态 | `RECEIVED / VALIDATED / COMPLETED / NEED_MORE_INFORMATION / DEGRADED / FAILED / CANCELLED` | `PrecheckRun.status` 使用同一集合 | 可按枚举候选映射，但不代表已满足 v2 不可变快照、权限或持久化约束。 |
| Session 状态 | v1 响应复用预诊状态 | `ACTIVE / TERMINATED` | 不能从 `COMPLETED`、信息完整、达到轮次上限或 Feedback 推导 `TERMINATED`。 |
| Session 终止原因 | 无独立字段 | `CONTINUED_SUBMISSION / SELF_SERVICE_CONFIRMED` | 必须来自用户显式命令，不能从建议或反馈推断。 |
| 解析、索引、评估、重置任务 | 无 | `PENDING / RUNNING / SUCCEEDED / FAILED` | 任务最大总尝试次数为 3；历史失败任务不可变。解析成功但有警告仍为 `SUCCEEDED + warnings`，不增加 `SUCCEEDED_WITH_WARNINGS`。 |
| 知识版本 | 无 | `DRAFT → IN_REVIEW → APPROVED → PUBLISHED → DEPRECATED` | 与技术任务状态严格分离。 |

### 5.2 多轮限制

- v2 Run 1 由创建 Session 产生，最多再补充两轮；第 4 次返回 `409 RUN_LIMIT_REACHED`。
- 后续 Run 必须发送完整 Context 快照。产品线、产品或组件改变返回 `409 NEW_SESSION_REQUIRED`；版本、级别、描述等允许变化但会重新检索。
- Run 完成、信息完整、达到 Run 3 或记录 Feedback 均不自动终止 Session。
- `TERMINATED` Session 只读；恢复和读取都重新校验本人身份与当前产品线权限，`ADMIN` 不能接管他人 Session。

## 6. Feedback 与继续提交拆分

v1 `FeedbackRequest` 的 `adoptionStatus + continuedSubmission` 是一个请求；v2 将其拆为：

1. `POST /feedback`：记录 `adoptionStatus`、可选 `helpfulness` 与可选原因，不终止 Session。
2. `POST /submission-continuations`：记录用户人工确认、终止原因为 `CONTINUED_SUBMISSION`，不生成正式 SLA 或工单。

迁移调用方必须分别处理两个命令的成功、失败、超时、重试、幂等键和审计事件。两者没有原子性承诺：

- Feedback 成功、Continuation 失败时，只保留 Feedback，调用方可用 Continuation 自己的幂等键重试。
- Feedback 失败、Continuation 成功时，继续提交确认仍有效，Feedback 可独立重试。
- 不得回滚已成功的一个命令来模拟 v1 的单调用外观，也不得用同一个幂等键绑定两者。

## 7. 错误模型映射

v1 `ApiError` 只有有限错误码，并以 `traceId` 和对象形 `fieldErrors` 表达。v2 固定返回：

`code`、`message`、`requestId`、`timestamp`、`fieldErrors[]`、`retryable`、`safeDetails`。

| v1 情况 | v2 稳定错误码 | 说明 |
| --- | --- | --- |
| `VALIDATION_ERROR` | `VALIDATION_ERROR` | v2 `fieldErrors` 是 `{field, code, message}` 数组。 |
| 未显式区分认证失败 | `UNAUTHENTICATED` | 本地 Cookie 会话无效。 |
| 未显式区分权限失败 | `INSUFFICIENT_ROLE` | Session/CSRF 均有效但角色或产品线不足。 |
| 无 | `CSRF_VALIDATION_FAILED` | Session 有效但 Token 缺失、为空、不匹配或属于其他 Session；HTTP `403` 且 `retryable=false`。 |
| v1 404 文本响应 | `RESOURCE_NOT_FOUND` | v2 统一 Schema；无权限与不存在使用相同安全 404。 |
| 无 | `DUTY_SEPARATION_VIOLATION` | 提交人不得批准/发布自己的版本，ADMIN 不绕过。 |
| 无 | `ILLEGAL_STATE_TRANSITION` | 显式知识命令或终态 Session 的非法状态。 |
| 无 | `IDEMPOTENCY_KEY_CONFLICT` | 同一命令幂等键对应不同规范化请求。 |
| 无 | `IDEMPOTENCY_CONTEXT_CONFLICT` | Session 业务键相同但规范化 Context 不同。 |
| 无 | `NEW_SESSION_REQUIRED` | 产品线、产品或组件变化。 |
| 无 | `RUN_LIMIT_REACHED` | 尝试创建第 4 次 Run。 |
| 无 | `FILE_TOO_LARGE` | Markdown/TXT 5 MB、文本型 PDF 20 MB 原始字节限制。 |
| 无 | `UNSUPPORTED_FILE_TYPE` / `PDF_TEXT_LAYER_REQUIRED` | 不接受扫描 PDF、OCR、DOCX、HTML 或其他格式。 |
| 无 | `PARSE_PREVIEW_NOT_READY` | ParseTask 为 `PENDING / RUNNING` 时预览端点返回 `409`、`retryable=true`；失败任务沿用 `TASK_FAILED`。 |
| `INTERNAL_ERROR` | `SERVICE_UNAVAILABLE` 或受控内部映射 | v2 不返回堆栈、路径、正文或越权详情；仅确属可重试时 `retryable=true`。 |

Mapper 不得仅凭 HTTP 状态码猜测业务语义；必须按稳定 `code` 显式分支。

## 8. 安全、分页和异步任务

- v2 本地身份使用服务端 `SESSION` Cookie；所有 Cookie 写操作要求 `X-CSRF-Token`。登录 `201` 首次发放 Token，current `200` 用于页面刷新后恢复，两者均返回 `Cache-Control: no-store`。
- CSRF Token 绑定单个 Session，同一 Session 内稳定且不滚动轮换；新建、切换模拟身份或替换 Session 时生成新 Token。退出或会话过期时 Session 与 Token 同步失效，一期不提供独立刷新端点。
- Token 只允许保存在浏览器内存中，不进入 JSON、可读 Cookie、localStorage、`safeDetails`、日志或审计。无有效 Session 为 `401 UNAUTHENTICATED`；有效 Session 下 CSRF 失败为 `403 CSRF_VALIDATION_FAILED`；身份权限不足为 `403 INSUFFICIENT_ROLE`。
- Feedback、Continuation、Run、知识命令、评估与重置等可重试命令要求 `Idempotency-Key`；创建 Session 使用已冻结的业务幂等键。
- 所有集合统一为 `items + page`。`page` 从 1 开始，`size` 默认 20、最大 100，`sortDirection` 只允许 `ASC / DESC`；各资源只收紧 `sortBy` 与过滤字段。
- 解析预览将摘要、文本块和 Chunk 拆为三个只读端点；文本块与 Chunk 各自使用 `items + page` 独立分页，固定 `sequence ASC`。聚合警告最多 100 条，并仅返回安全描述，不暴露路径、URL、堆栈或原始字节。
- `parseResultHash` 绑定当前解析结果、警告集合、解析器版本和结构顺序。送审请求必须携带当前哈希；批准请求还必须携带 `acknowledgedWarningCodes`，完整覆盖当前警告码。过期哈希或漏确认返回 `409 ILLEGAL_STATE_TRANSITION`，审计不复制解析正文。
- 上传、修订或发布命令创建任务，不提供任意任务状态更新 API。ParseTask、IndexTask、EvaluationRun 与 AdminReset 使用统一任务基础字段；失败修复后创建新任务，历史任务不可改写。
- 知识新版本只有 FTS 与 Embedding 两路索引都成功后才原子成为 `PUBLISHED`，旧发布版才转为 `DEPRECATED`；失败时新版本保持 `APPROVED`、旧版继续服务。

## 9. 降级与人工边界

- `HYBRID`：FTS 与 Embedding 均可用；只有完整度通过、证据充分、两路互证且无降级时置信度才可为 `HIGH`。
- `FTS_ONLY`：Embedding 故障，明确降级并将置信度置为 `LOW`；不阻断预诊或人工继续提交。
- `UNAVAILABLE`：FTS 不可用且无已确认替代路径时返回无 Evidence 的降级结果；若请求整体暂时失败，安全错误仍须包含可继续人工提交的提示。
- Evidence、低置信度和建议都不能成为最终根因、最终方案或正式复盘结论。SLA 内容和提交由人工确认。

## 10. 契约对照自审

| 事实来源 | v2 落点 | 自审结果 |
| --- | --- | --- |
| `docs/project/scope.md`：一期本地模拟闭环、三 Run、终态与幂等 | Session/Run Schema、创建与冲突响应 | 已覆盖；契约已于 2026-07-14 获人工批准进入实施。 |
| `docs/architecture/data-model.md`：不可变快照、Feedback/Continuation 拆分、任务不可变 | PrecheckRun、Feedback、SubmissionContinuation、TaskBase | 已覆盖。 |
| `docs/product/user-flow.md`：用户可跳过补充并继续提交 | `allowedActions` 与 Continue 命令 | 已覆盖，`CONTINUE_SUBMISSION` 不因降级或上限消失。 |
| `docs/product/roles-and-permissions.md`：固定角色、职责分离、ADMIN 不绕过 | Cookie/CSRF、403 结构化错误、批准/发布命令 | 已覆盖；真实身份未接入。 |
| `docs/architecture/security-boundaries.md`：Evidence 再授权、安全 404、无路径 | 单 Evidence GET、安全 404、Schema 禁止 URL/路径 | 已覆盖。 |
| `docs/architecture/knowledge-ingestion-flow.md`：双索引发布、最大三次任务尝试 | IndexTask 双状态、发布 `202`、`maxAttempts=3` | 已覆盖。 |
| `docs/architecture/online-precheck-flow.md`：FTS/Embedding 降级和置信度规则 | `RetrievalDegradation`、`PrecheckResult` | 已覆盖。 |
| 已实现 `openapi.yaml` | v1 零变更；本文逐项说明非兼容映射 | 已覆盖；v1 仍是唯一完整已实现契约，v2 目前只实现 AuthSession 基础切片。 |

## 11. 评审状态

| 项目 | 结论 |
| --- | --- |
| 依据来源 | 上表所列 ACTIVE/已确认范围文档、ADR-0006 与已实现 v1 OpenAPI。 |
| 置信度 | `HIGH`（仅针对契约批准状态）：DRAFT 已通过结构校验，并由用户以 API、产品、安全负责人身份于 2026-07-14 明确批准进入实施；不代表实现质量或生产效果。 |
| 人工介入建议 | 实施 PR 必须逐项对照本契约；已发布接口兼容性变化、真实数据或外部系统接入仍须另行人工确认。 |
| 待补充信息 | 未来 v1 下线策略（当前不设日期）；真实 AIOps 字段与附件标识映射。 |

当前达到的条件是“API 契约已获人工批准，且 AuthSession 与知识上传解析预览切片已实现”，不是“一期已完成”。需求范围为 `CONFIRMED`；API v2 仍为 `DRAFT`，整体实施状态为 `PARTIALLY_IMPLEMENTED`，批准状态为 `APPROVED_FOR_IMPLEMENTATION`，一期实施为 `READY_FOR_IMPLEMENTATION`。当前实现标识为 `V1_MOCK_PLUS_V2_AUTH_SESSION_AND_KNOWLEDGE_INGESTION`；v1 Mock 保持兼容，未明确标记为 `IMPLEMENTED` 的 v2 operation 仍为 `NOT_IMPLEMENTED`。
