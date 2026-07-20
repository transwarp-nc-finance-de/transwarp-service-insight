# 模块边界

Status: ACTIVE  
Owner: 技术负责人  
Last reviewed: 2026-07-20
Source of truth for: 模块职责、依赖方向与替换边界

## Precheck

负责持久化会话、不可变运行轮次、状态、下一步动作、独立 Feedback 与人工继续提交硬边界。Feedback 和 SubmissionContinuation 使用独立事务、幂等记录与审计，不提供原子成功承诺。API 只依赖应用用例；领域模型不得依赖 Spring MVC DTO；应用层通过 Port 调用能力。

## Knowledge

当前已实现模拟 `KnowledgeDocument`、`KnowledgeVersion`、`KnowledgeDraftRevision`、`ParseTask`、解析结果、`KnowledgeChunk`、不可变审核历史、`IndexTask`、双索引发布状态、Run 检索审计与 Evidence 快照的 PostgreSQL 持久化，以及不可变 Compose volume 原始文件 Adapter；API 已交付首次上传、任务查询、解析预览、草稿修订、送审、退回、批准、原子发布、废弃、授权混合检索和 Evidence 重新授权读取。真实/外部知识源仍未实现；检索只使用双索引成功且处于当前 `PUBLISHED` 状态的版本。

## 已实现与未来模块边界

- Retrieval（已实现本地切片）：权限优先过滤、FTS/向量融合、降级、Evidence 快照与重新授权读取；
- Feedback/Audit（已实现本地切片）：独立采纳反馈、人工继续提交，以及身份、知识、发布、预诊、Evidence、Feedback、Continuation 的脱敏结构化审计；
- Policy（部分实现）：版本化完整度策略读取与 Run 快照；运行时编辑/发布仍未实现；
- Generation（DRAFT）：真实生成式模型、引用绑定和生成护栏；当前仅使用确定性模板/规则；
- Integration（DRAFT）：真实 AIOps、ITSM、对象存储和外部模型适配。

一期只新增具有本地身份调用方的 `IdentityContextPort`。`HistoricalSlaPort`、`TicketSubmissionPort`、`AttachmentAccessPort` 与 `AiopsFormContextPort` 仅保留为二期 DRAFT 职责名称，不在一期创建无调用方接口或 Mock/NoOp Adapter。前端现有 HostBridge 继续作为 Sandbox、Embed 与未来宿主的通信边界。

依赖只能由 API 指向 application、application 指向 domain/port、infrastructure 实现 port。领域层禁止依赖 Controller、HTTP DTO、数据库 SDK、模型 SDK或外部系统协议。在线 Precheck 禁止同步执行离线知识导入。

可独立部署候选仅包括离线知识处理与高负载检索/生成 Adapter；是否拆分必须由容量、发布和权限边界证明。
