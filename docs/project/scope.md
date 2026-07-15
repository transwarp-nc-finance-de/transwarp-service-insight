# 当前实现与一期目标范围：SLA 智能预诊助手

状态：ACTIVE，Last reviewed: 2026-07-15。当前实现与一期目标必须分开解读。

统一状态：需求范围为 `CONFIRMED`；API v2 为 `DRAFT / PARTIALLY_IMPLEMENTED / APPROVED_FOR_IMPLEMENTATION`；一期实施为 `READY_FOR_IMPLEMENTATION`；当前实现为完整兼容的 `v1 Mock` 加已实现的 v2 AuthSession 与知识上传解析预览切片。

当前已有 Vue 前端、Spring Boot API、确定性 Mock Workflow、本地模拟身份 UI、Compose PostgreSQL、OpenAPI 和 CI。AIOps 是 SLA 表单、枚举、原有校验和最终提交的权威宿主；Service Insight 只负责完整度分析、辅助建议、引用、反馈、审计与安全降级。

前端 `/sandbox` 是 Mock AIOps，仅用于本地开发、联调、演示和自动化测试；`/embed` 是不复制 SLA 表单的嵌入式预诊面板。用户始终可忽略建议并由 AIOps 继续原有提交，反馈失败不影响提交。

后端 v1 预诊继续使用应用层工作流、细粒度 Port/Mock Adapter 和进程内 Session Repository。v2 AuthSession 使用 `IdentityContextPort`、Flyway 和本地 PostgreSQL 保存四个版本化模拟身份、目录与服务端会话；知识切片将模拟原始文件保存于 Compose volume，并将文档、草稿版本、ParseTask 与解析预览保存于本地 PostgreSQL。初次预诊创建 Run 1，追问在同一 Session 中递增 Run。所有输入只允许模拟、公开或脱敏信息，不保存附件内容或真实敏感正文。

当前未接真实 AIOps、SSO/身份传递、企业共享或生产数据库、真实业务数据、真实知识源、知识审核发布、RAG、LLM、多 Agent、真实 Wiki/历史 SLA 或真实 SLA 创建接口。智能输出是 `模拟数据`，包含依据、置信度、人工介入建议和待补充信息，不是最终根因、最终方案或正式复盘结论。

能力矩阵：Engineering Baseline `DONE`；Architecture Skeleton `IN PROGRESS`；Local Identity/PostgreSQL Foundation `IMPLEMENTED`；AIOps Host Integration `PROTOTYPE`；业务 Persistence `IN PROGRESS`；Knowledge Ingestion 首次上传与解析预览切片 `IMPLEMENTED`；审核发布、Retrieval、LLM Generation、Agent Orchestration 均为 `NOT STARTED`。

## 一期范围重定义访谈（2026-07-13）

以下内容为已确认的一期目标范围，不代表当前已经实现。

### CONFIRMED

- 一期本地完整纵向闭环必须从用户可操作的最小知识治理 UI 开始，覆盖上传、解析预览、提交审核、审核发布与废弃；不建设通用企业知识平台。
- 一期知识导入格式限定为 Markdown、TXT 与文本型 PDF。文本型 PDF 指包含可直接提取文本层的 PDF，不包含依赖 OCR 才能识别正文的扫描件。
- 文本型 PDF 采用文本优先解析：段落正常提取，表格按阅读顺序尽力展平，图片内容忽略。解析预览必须展示内容丢失或顺序不确定警告，只有经审核人员确认后才可发布。
- 原始知识文件不可变；编辑人员可修改元数据、适用范围、清洗文本和解析警告说明。每次修改形成新草稿修订并使已有 Chunk/索引失效；审核中修改退回 `DRAFT` 并重新生成解析/切片预览，已发布内容仍需创建新版本。
- 知识版本治理状态与文件解析任务状态分离：`KnowledgeVersion` 沿用 `DRAFT → IN_REVIEW → APPROVED → PUBLISHED → DEPRECATED`，`ParseTask` 独立使用 `PENDING → RUNNING → SUCCEEDED/FAILED` 并允许失败重试。
- 一期知识治理采用提交人与审核人职责分离：`KNOWLEDGE_EDITOR` 不得批准或发布自己提交的知识版本；一名不同身份的 `KNOWLEDGE_REVIEWER` 审核即可，不要求双人会签。该规则必须由后端真实执行。
- 已发布知识版本不可原地修改；修改必须创建新 `DRAFT`。新版本发布时原子地将旧发布版本置为 `DEPRECATED`，同一知识文档最多只有一个可检索的当前发布版本，历史版本保留用于引用与审计追溯。
- `APPROVED` 新版本只有在 FTS 与向量索引都成功后才原子转为 `PUBLISHED` 并废弃旧版本。索引失败时新版本保持 `APPROVED`，旧发布版本继续服务；独立 `IndexTask` 负责状态与重试，半索引版本不得检索。
- `IndexTask` 只对数据库暂时不可用、Embedding 超时等瞬时错误自动退避重试，总尝试次数最多 3 次；维度不匹配、内容无效等确定性错误不自动重试。耗尽后保持版本 `APPROVED`，修复后创建新任务，历史失败任务不可变。
- 一期使用 PostgreSQL 持久化全部业务状态，包括知识治理、预诊 Session/Run、反馈、审计与评估记录；容器重启后数据可恢复，并提供数据库迁移、`模拟数据` 初始化和受控重置。原始文件不存入数据库。
- 一期不提供备份/恢复演示。版本化 `模拟数据` 初始化与受控重置仅作用于明确标识的本地环境；重置需要 `ADMIN` 二次确认并记录审计，不得表述为生产恢复能力。
- 一期本地数据不自动过期、不提供逐条删除，保留至 `ADMIN` 执行受控重置；该规则只适用于非生产的模拟、公开或脱敏数据，不能沿用到二期真实数据。
- 原始知识文件保存在 Compose 管理的本地挂载目录，PostgreSQL 仅保存不透明文件 ID、相对存储键、哈希、大小和媒体类型。业务 API、审计与日志不得暴露宿主机绝对路径；存储通过 `FileStoragePort` 隔离，以便后续替换对象存储 Adapter。
- 一期采用 PostgreSQL 全文检索与 pgvector 混合检索。两路召回必须先应用身份权限、`PUBLISHED` 当前版本和产品适用范围过滤，再按固定、可审计规则融合；模型能力不可用时可降级为全文检索。
- pgvector 向量由固定版本、CPU 可运行且可离线使用的公开 Embedding 模型生成，只处理模拟、公开或脱敏文本，不调用外部模型接口。模型加载或推理失败时降级为全文检索，不阻断预诊或人工继续提交。
- Embedding 必须支持中英混合技术文本及中英文资料互检索；固定评估集包含中文、英文和中英混合样例。错误码、版本号和配置键的精确召回主要由 FTS 保障。
- 默认 `local-embedding` 模型制品不超过 2 GB、服务内存不超过 4 GB、仅需 CPU 且不要求 GPU；并发 1 时单查询 P95 目标不超过 1 秒。批量索引吞吐单独统计，超限模型不得作为默认组件。
- 一期默认 Embedding 为 `intfloat/multilingual-e5-base` revision `d13f1b27baf31030b7fd040960d60d909913633f`，固定 768 维、512 Token 上限及 `query:`/`passage:` 前缀；只加载必要的 Safetensors 与 tokenizer，不执行远程自定义代码。许可证复核、资源门槛或固定评估集未通过时不得作为验收默认组件。
- 模型在构建 `local-embedding` 镜像时从受控来源按固定 revision 下载，并校验固定文件清单与 SHA-256；制品不提交 Git，运行时不得联网。未获下载授权时不得隐式拉取。
- 一期预诊建议由确定性模板/规则基于检索证据生成并标注 `模拟数据`，不调用真实生成式模型。Generation Port 保持可替换，本地 Embedding 仅用于语义检索，不得表述为生成式预诊能力。
- 置信度采用版本化、可审计的确定性规则并输出判定依据：Embedding 降级、无有效证据、权限过滤后无证据或关键信息缺失时为 `LOW`；存在有效证据但仅有单一证据、检索路径不一致或仍有待补充信息时为 `MEDIUM`；仅当完整性检查通过、证据充分、FTS 与向量检索相互印证且无降级时为 `HIGH`。不得由模型或模板自由给出置信度。
- 一期身份采用预置 `模拟数据` 用户的本地 Mock 登录/切换与服务端 Session/Cookie，后端真实执行 RBAC 并记录审计主体。不提供注册、找回密码、真实组织同步或正式 SSO；身份来源通过 `IdentityContextPort` 隔离。
- 一期固定 `PRECHECK_USER`、`KNOWLEDGE_EDITOR`、`KNOWLEDGE_REVIEWER`、`ADMIN` 四个角色。`ADMIN` 可查看基本评估与结构化审计并执行受控数据重置，但不得绕过知识审核职责分离，也不得替用户确认继续提交。
- 一期资料授权采用“角色 + 产品线范围”，知识 Chunk 继承文档范围，过滤在关键词和向量召回前执行。无权限资料不得进入候选、引用、生成上下文或非必要审计摘要；不建设组织树、任意文档 ACL 或用户组治理。
- 每个 `PrecheckSession` 最多创建 3 个 Run：Run 1 为初诊，最多补充两轮。用户可随时跳过补充并继续提交；达到上限后只展示剩余待补充信息和人工介入建议，不得自动创建第 4 个 Run。
- Run 完成、信息完整或达到 Run 上限都不自动结束 Session。只有用户明确确认模拟继续提交或确认采纳建议并结束预诊时，Session 才以 `CONTINUED_SUBMISSION` 或 `SELF_SERVICE_CONFIRMED` 原因进入业务终态；记录反馈本身不终止 Session。
- 用户可恢复自己的未终止且仍可补充的 Session；终态 Session 只读。管理员不得接管或恢复其他用户的 Session。恢复时必须重新校验当前身份与产品线权限。
- 产品线、产品或组件变化时必须新建 Session；版本、级别、描述等变化可在原 Session 创建下一 Run，但必须重新检索。每个 Run 保存完整规范化上下文快照，旧 Run 与证据不可变。
- 反馈与模拟继续提交是两个独立命令和审计事件，具有独立失败、重试与幂等边界；反馈失败不得影响继续提交。当前 v1 OpenAPI 将两者耦合，本轮只记录目标差异，不修改已实现契约。
- Feedback 分别记录采纳行为 `ADOPTED / PARTIALLY_ADOPTED / IGNORED` 与可选有用性评价 `HELPFUL / NOT_HELPFUL`，原因文本可选。缺少有用性评价不影响采纳记录或继续提交。
- 模拟继续提交保存用户、时间、Session、幂等键和可选原因，不生成 `ticketId` 或模拟工单。`hostTicketId` 仅作为二期 DRAFT 关联概念，待 AIOps 返回真实标识。
- 审计只保存结构化元数据与稳定引用 ID，包括主体、Session/Run、动作、状态、策略/规则/Embedding/索引版本、Evidence ID、反馈、继续提交、错误和降级，不复制业务正文。正文只能经原领域对象重新授权读取。
- 一期提供最小管理员评估页，可手动运行固定评估集并查看历史结果摘要和失败案例；评估运行与结果持久化。不建设实时运营大屏、A/B、告警或复杂趋势分析。
- 一期固定评估集版本为 `mock-eval-v1`，不少于 30 条 `模拟数据` 策划样例，覆盖中文、英文、中英混合与中英文资料互检，以及精确词、语义改写、证据不足、权限隔离、Embedding 降级、多轮补充和引用核验。每条使用稳定 `caseId`，保存语言/场景标签、Precheck 输入、期望 Evidence ID、允许产品线范围、预期检索模式、降级状态及待补充字段；结果标注“小样本工程评估，不代表生产效果”。
- 评估失败案例仅由 `ADMIN` 分页读取，公共表示只包含稳定模拟案例 ID、场景标签、失败检查/失败码及结构化期望与实际摘要；不得返回评估输入正文、无权 Evidence ID/摘录、宿主路径或内部推理。正常执行但工程门禁未通过的评估任务状态仍为 `SUCCEEDED`，由独立 `gatePassed=false` 表示未通过。
- 最小管理员评估页同时展示预诊次数、成功/降级率、平均 Run 数、信息补充率、引用命中率、采纳率、继续提交率及检索/Embedding 延迟的聚合摘要；不读取正文、不自动判责，也不设业务收益硬门槛。
- 版本化本地评估集的验收门槛为：权限泄漏率 0%、引用错误率 0%、降级场景通过率 100%、Recall@5 不低于 80%。这些是小样本工程门禁，不构成生产 SLA、业务收益或模型效果承诺。
- 一期不引入 Reranker 或 Query Rewrite；全文与向量在过滤后各召回前 20 条，使用 RRF 及固定 `k=60` 融合并返回前 5 条，同分时按知识版本 ID、Chunk ID 稳定排序。参数和规则版本受控、可审计，可由固定评估集驱动调整，但不提供运行时调参页面。
- 一期单文件上限为 Markdown/TXT 5 MB、文本型 PDF 20 MB，按原始字节数在持久化前校验。超限返回安全、明确的 UI/API 错误，`ADMIN` 也不得绕过。
- 解析任务只对瞬时 I/O 或临时资源错误自动退避重试，总尝试次数最多 3 次；格式不支持、超限、无文本层等确定性错误不自动重试。耗尽后进入 `FAILED`，修正输入后创建新任务，历史失败任务不可变。
- 一期采用结构优先的确定性切片：优先按标题、段落和 PDF 文本块切分，超长结构块使用最大 400 Token、重叠 50 Token 的窗口。切片规则和参数版本化、可审计，通过固定评估集受控调整，不提供 UI 调参；重新切片创建新的索引任务，不改写既有 Run 引用的 Chunk。
- 一期 Compose 目标包含 `frontend`、`backend`、`PostgreSQL/pgvector`、`local-embedding` 四个服务；解析与索引后台任务由 backend 执行，原始文件使用 Compose volume。Redis、消息队列、MinIO、生成模型服务和监控大屏不进入一期。
- 默认完整 Compose 模式要求四个服务健康，只有完整模式可通过一期验收；另提供显式降级 profile 验证仅 FTS 路径。运行中 Embedding 故障时 backend 自动降级并明确标记，恢复后不改写历史 Run。
- 二期外部系统 Port 中，一期只实际实现有本地身份调用方的 `IdentityContextPort`。`HistoricalSlaPort`、`TicketSubmissionPort`、`AttachmentAccessPort`、`AiopsFormContextPort` 只记录 DRAFT 职责，不创建无调用方的 Mock/NoOp 接口；前端现有 HostBridge 抽象继续保留。
- 一期冻结宿主无关的 `PrecheckContext` 领域语义，包括来源系统、宿主请求 ID、表单 Schema 版本、问题类型、产品线/产品/组件/版本、问题级别、服务类型、标题、纯文本描述、补充信息、影响范围和附件元数据；不冻结当前 HTTP 字段形状，DTO 通过 Mapper 隔离。
- 创建 Session 必须包含 `sourceSystem`、`hostRequestId`、`formSchemaVersion`、`issueType`、`productLine`、`title`、`descriptionPlainText`。缺失时不能启动预诊，但不阻断用户绕过预诊并模拟继续提交；产品、组件、版本等由完整度策略追问。
- `additionalInformation` 使用结构化条目集合，每项包含稳定字段编码、显示名和纯文本值。只有当前完整度策略认识的编码参与判断；未知编码保留追溯但不自动信任，也不得改变权限或策略。
- 一期 Sandbox 只使用 `模拟数据` 问题附件 ID 与元数据（文件名、媒体类型、大小），不读取、上传、解析或保存问题附件内容；元数据不得包含宿主路径、可直达内容的 URL 或长期凭据。
- 一期引用绑定不可变 `KnowledgeVersion` 与 `Chunk`，包含文档标题、版本 ID、Chunk ID、受控摘录和内容哈希；只能通过授权 API 打开，不返回本地路径。每次读取均重新校验当前身份和产品线权限，包括历史 Run 引用。
- 补充信息由版本化确定性完整度策略触发，规则由通用必需项与问题类型专属清单组成；每个 Run 保存策略版本与命中说明。用户可跳过任何补充项，策略只能建议，不得阻断模拟继续提交。
- 完整度策略由版本化配置文件随 `模拟数据` 初始化进入数据库；`ADMIN` 只能查看规则与版本，不提供编辑 UI。策略变更通过评审后的新配置版本完成，历史 Run 保留原策略快照。
- 一期本地问题类型固定为 `FUNCTIONAL_FAILURE`、`PERFORMANCE_DEGRADATION`、`INSTALLATION_CONFIGURATION`、`DATA_CORRECTNESS`，使用稳定编码和中文显示名。它们属于 `模拟数据`，不代表未来 AIOps 正式枚举。
- 完整度策略的通用待补充项为产品、组件、版本、问题级别、影响范围和 `OCCURRED_AT`。类型专属项为：功能故障的 `ERROR_MESSAGE / REPRODUCTION_STEPS / RECENT_CHANGES`；性能下降的 `TIME_WINDOW / BASELINE_METRIC / CURRENT_METRIC / WORKLOAD_IDENTIFIER`；安装配置的 `OPERATION_GOAL / EXECUTED_STEPS / ENVIRONMENT_SUMMARY / ERROR_MESSAGE`；数据正确性的 `EXPECTED_RESULT / ACTUAL_RESULT / AFFECTED_DATA_SCOPE / QUERY_OR_JOB_ID`。全部为建议项，不是提交门禁。
- `sourceSystem + hostRequestId` 作为一期真实幂等键：相同规范化上下文重试返回原 Session/Run，同键不同上下文返回安全冲突并审计，不静默覆盖；新的 `hostRequestId` 可主动重新预诊。
- 当前 `/api/v1` 字段、语义与行为保持不变；重定义后的一期 Context、知识治理、独立反馈/继续提交、引用和评估能力使用 `/api/v2`。新旧 DTO 通过 Mapper 进入应用用例，领域模型不依赖 HTTP 版本。本轮不修改 OpenAPI。
- 一期冻结以下 v2 资源组职责与路径前缀：`/api/v2/auth-sessions`、`/api/v2/knowledge-documents`、`/api/v2/knowledge-versions`、`/api/v2/parse-tasks`、`/api/v2/index-tasks`、`/api/v2/evidence`、`/api/v2/precheck-sessions`、`/api/v2/precheck-sessions/{sessionId}/runs`、`/api/v2/feedback`、`/api/v2/submission-continuations`、`/api/v2/evaluation-runs`、`/api/v2/metrics`、`/api/v2/audit-events`、`/api/v2/completeness-policies`、`/api/v2/admin/resets`。具体方法、Schema、错误码与分页已通过 DRAFT 契约评审；API v2 为 `APPROVED_FOR_IMPLEMENTATION`，但本轮不承诺完整 CRUD 已实现。
- v2 OpenAPI 已完整定义资源与命令方法、请求/响应 Schema、错误码、幂等语义、分页、异步任务状态及 v1→v2 映射，并已获人工批准；契约整体仍为 `DRAFT / PARTIALLY_IMPLEMENTED / APPROVED_FOR_IMPLEMENTATION`。当前实现三个 AuthSession operation、知识首次上传、单 ParseTask 查询和三个 parse-preview 读取 operation；其他 v2 operation 仍未实现。`docs/api/openapi.yaml` 的 v1 字段、状态码和无认证 Mock 行为保持不变。
- 一期本地完整纵向闭环以“模拟继续提交已记录，反馈和审计已持久化，固定评估集可产出最小质量评估结果”为验收终点。
- 最小评估用于证明检索、引用、权限与降级行为，不要求建设完整运营后台。
- 一期闭环不以生成或保存模拟 SLA 单据、工单草稿或提交回执为验收内容；Service Insight 仍不创建正式 SLA。

### DEFERRED_TO_PHASE_2

- 扫描 PDF、图片 OCR、DOCX、HTML 与 Wiki 自动同步推迟到一期之后，以控制解析与安全测试范围。一期通过可替换文档解析边界预留扩展点；当出现经过授权的明确资料需求、样本集和解析质量验收标准时再触发扩展评审。
- 真实生成式模型及其 Generation Adapter 推迟到二期。原因是一期优先验证知识治理、检索、引用、权限和可重复工作流；一期保留 Generation Port，待模型授权、评估集、安全边界和资源预算确认后再接入。
- 真实历史 SLA、工单提交、AIOps 附件访问和 AIOps 表单主动读取推迟到二期；一期仅在 DRAFT 文档中定义对应 Port 的职责边界，待真实宿主契约、授权和调用场景确认后再创建接口与 Adapter。

## 一期明确非目标（CONFIRMED）

- 不联调真实 AIOps、ITSM、SSO、Wiki、历史 SLA、客户日志、生产数据库或其他生产系统；一期仅保留已确认的 HostBridge、PrecheckContext 与 DRAFT 外部 Port 职责边界。
- 不自动创建、提交或关闭 SLA，不自动判责，不自动执行运维操作；这些能力不是一期 Mock 目标，也不会因预诊结果触发。
- 不接真实生成式模型或外部模型接口，不引入多 Agent、Reranker 或 Query Rewrite；一期只运行本地公开 Embedding 和确定性模板/规则生成。
- 不支持扫描 PDF/OCR、DOCX、HTML、Wiki 自动同步或全格式解析；一期 Parser 边界只为后续扩展预留。
- 不建设 Kubernetes、高可用、灾备、多租户、计费、跨地域部署、企业级容量压测或生产上线批准能力；这些内容不作为一期验收，也不承诺二期自动纳入。
- 不引入 MinIO、Redis、消息队列、完整运营大屏或 A/B 平台；除现有模块边界与可替换 Adapter 外，不为这些候选创建空实现。
