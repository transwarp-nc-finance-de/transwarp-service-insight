# 模块边界

Status: ACTIVE  
Owner: 技术负责人  
Last reviewed: 2026-07-16
Source of truth for: 模块职责、依赖方向与替换边界

## Precheck

负责会话、运行轮次、状态、下一步动作和人工继续提交硬边界。API 只依赖应用用例；领域模型不得依赖 Spring MVC DTO；应用层通过 Port 调用能力。

## Knowledge

当前已实现模拟 `KnowledgeDocument`、`KnowledgeVersion`、`KnowledgeDraftRevision`、`ParseTask`、解析结果、`KnowledgeChunk`、不可变审核历史、`IndexTask` 与双索引发布状态的 PostgreSQL 持久化，以及不可变 Compose volume 原始文件 Adapter；API 已交付首次上传、任务查询、解析预览、草稿修订、送审、退回、批准、原子发布和废弃。在线检索、Evidence 和真实/外部知识源仍未实现；仅双索引成功并处于当前 `PUBLISHED` 状态的版本可供后续检索端点使用。

## 未来模块（DRAFT）

- Retrieval：权限过滤、查询、排序和 Evidence；
- Generation：结构化建议、引用绑定、护栏与降级；
- Feedback：采纳、忽略、继续提交和结果回流；
- Policy/Audit：身份策略快照、门禁、事件和脱敏审计；
- Integration：AIOps、ITSM、对象存储和外部模型适配。

一期只新增具有本地身份调用方的 `IdentityContextPort`。`HistoricalSlaPort`、`TicketSubmissionPort`、`AttachmentAccessPort` 与 `AiopsFormContextPort` 仅保留为二期 DRAFT 职责名称，不在一期创建无调用方接口或 Mock/NoOp Adapter。前端现有 HostBridge 继续作为 Sandbox、Embed 与未来宿主的通信边界。

依赖只能由 API 指向 application、application 指向 domain/port、infrastructure 实现 port。领域层禁止依赖 Controller、HTTP DTO、数据库 SDK、模型 SDK或外部系统协议。在线 Precheck 禁止同步执行离线知识导入。

可独立部署候选仅包括离线知识处理与高负载检索/生成 Adapter；是否拆分必须由容量、发布和权限边界证明。
