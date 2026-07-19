# 目标架构

Status: DRAFT  
Owner: 架构负责人  
Last reviewed: 2026-07-19
Source of truth for: 一期已确认目标架构与未实现边界

目标采用模块化单体与 Workflow-first，不把微服务或多 Agent 作为基础依赖。

```text
预诊助手 / 知识治理 / 运营评估
              ↓
API 与应用用例
              ↓
确定性工作流、状态机与策略
              ↓
precheck / knowledge / retrieval / generation / feedback / policy / audit
              ↓
Port → 可替换 Adapter → 数据、模型及外部系统
```

在线预诊要求低延迟、只读、权限先于检索、可降级且永不阻断人工提交。离线知识处理负责导入、解析、审核、发布、索引、失败重试和版本管理，两条链路彻底分离。

未来可替换 Adapter 包括关键词/向量检索、生成网关、持久化、身份策略、审计与 AIOps/ITSM。任何真实数据、真实模型、外部服务、生产访问或自动提交均需另行人工批准。受控 Agent 仅在固定工具、轮次、预算、超时和完整回放下实验，且必须证明优于单工作流。

## 一期已确认目标边界

一期使用 PostgreSQL 持久化知识治理、预诊 Session/Run、反馈、审计和评估等全部业务状态，要求容器重启可恢复，并支持迁移、`模拟数据` 初始化与受控重置。原始文件不作为数据库大字段保存。当前已实现 AuthSession、知识上传解析预览、不可变审核历史、IndexTask/双索引发布及预诊 Session/Run 的 PostgreSQL 切片；反馈、完整审计、评估和受控重置等其余持久化目标尚未实现。

原始知识文件使用 Compose 管理的本地挂载目录，通过 `FileStoragePort` 访问；数据库仅保存不透明文件 ID、相对存储键、哈希、大小与媒体类型。未来对象存储只能作为替换 Adapter 接入，不得让领域层依赖具体文件系统或对象存储 SDK。

一期检索采用 PostgreSQL 全文检索与 pgvector 双路召回，并在权限、当前发布版本及产品适用范围过滤之后执行固定、可审计的融合。Embedding 来源与融合默认参数已经确认，Reranker 与 Query Rewrite 已排除；当前 Mock 检索仍是唯一已实现事实。

一期不引入 Reranker 或 Query Rewrite。全文与向量在过滤后各召回前 20 条，使用 RRF 及固定 `k=60` 融合并返回前 5 条；同分时按知识版本 ID、Chunk ID 稳定排序。参数和规则版本由受控配置管理并进入审计，可由固定评估集驱动调整，但不提供运行时调参页面。

默认 Embedding 已确认为 `intfloat/multilingual-e5-base@d13f1b27baf31030b7fd040960d60d909913633f`，固定 768 维、512 Token 上限及 `query:`/`passage:` 前缀，只使用必要 Safetensors 与 tokenizer，不执行远程代码。模型制品不超过 2 GB、服务内存不超过 4 GB、CPU 并发 1 查询 P95 不超过 1 秒；内部非商用使用边界、许可证剩余风险、最终供应链证据、本机实测与固定评估集表现已在 Issue #19 于 2026-07-19 经人工复核通过。

Issue #39 已完成固定 revision 的受控取件并生成、复核文件清单与 SHA-256；Issue #19 最终状态为 `PASS`，Issue #25 已将批准制品用于外置只读 `local-embedding` 运行时和双索引闭环。制品不提交 Git 或写入镜像，服务启动前必须校验批准的逐文件 SHA-256，容器运行时禁止联网或隐式下载；模型、revision、依赖锁或 manifest 变化后必须重新评估。

一期 Generation Adapter 使用确定性模板/规则，根据检索证据生成结构化 `模拟数据` 输出；不调用真实生成式模型。本地 Embedding 只负责检索向量，不能被视为生成能力。真实生成模型继续由 Generation Port 隔离并推迟到二期评审。

一期身份由本地 Mock 登录 Adapter 建立服务端 Session/Cookie，后端真实执行 RBAC；未来 AIOps/SSO 身份通过 `IdentityContextPort` 替换，不让核心用例依赖具体 Cookie、JWT 或宿主协议。

一期 Compose 目标拓扑为 `frontend + backend + PostgreSQL/pgvector + local-embedding`。解析与索引任务在 backend 内后台执行，不拆独立 Worker；原始文件使用 Compose volume。Redis、消息队列、MinIO、生成模型服务和监控大屏均不进入一期。以上不代表当前 Compose 已实现这些组件。

默认完整模式要求四个服务健康，只有该模式可通过完整验收；显式降级 profile 用于验证无 Embedding 时的 FTS 路径。运行中 Embedding 故障不停止 backend，当前请求标记降级；服务恢复不追溯改写历史 Run。

一期实际落地 `IdentityContextPort` 以服务本地身份；`HistoricalSlaPort`、`TicketSubmissionPort`、`AttachmentAccessPort` 与 `AiopsFormContextPort` 只作为二期 DRAFT 职责边界。没有真实调用方前不创建空接口或 Mock/NoOp Adapter。前端继续保留现有 HostBridge 抽象。

一期明确不建设真实外部系统集成、自动 SLA/运维动作、真实生成式模型、多 Agent、高级检索链、全格式解析、生产级平台能力或完整运营平台。候选技术不得仅为未来可能性创建空接口、空服务或 Compose 组件。

当前 `/api/v1` 保持兼容；一期新语义由 `/api/v2` 承载。v1/v2 DTO 通过 Mapper 隔离，领域模型不依赖 HTTP 版本。完整 DRAFT OpenAPI 已覆盖方法、请求/响应 Schema、错误码、幂等、分页、异步任务状态和 v1→v2 映射，并于 2026-07-14 获人工批准。API v2 为 `APPROVED_FOR_IMPLEMENTATION`，一期实施为 `READY_FOR_IMPLEMENTATION`；当前已实现 AuthSession、知识上传解析预览、不可变草稿审核、双索引原子发布/废弃、持久化 Precheck Session/Run、授权混合 Retrieval 与 Evidence 读取，反馈、评估等其余 v2 operation 仍未实现，v1 契约与行为不变。

一期冻结的 v2 资源组为：`/api/v2/auth-sessions`、`/api/v2/knowledge-documents`、`/api/v2/knowledge-versions`、`/api/v2/parse-tasks`、`/api/v2/index-tasks`、`/api/v2/evidence`、`/api/v2/precheck-sessions`、`/api/v2/precheck-sessions/{sessionId}/runs`、`/api/v2/feedback`、`/api/v2/submission-continuations`、`/api/v2/evaluation-runs`、`/api/v2/metrics`、`/api/v2/audit-events`、`/api/v2/completeness-policies`、`/api/v2/admin/resets`。冻结只覆盖职责和路径前缀，不代表具体方法、Schema 或完整 CRUD 已实现。
