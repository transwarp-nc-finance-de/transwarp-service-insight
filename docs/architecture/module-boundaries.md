# 模块边界

Status: ACTIVE  
Owner: 技术负责人  
Last reviewed: 2026-07-12  
Source of truth for: 模块职责、依赖方向与替换边界

## Precheck

负责会话、运行轮次、状态、下一步动作和人工继续提交硬边界。API 只依赖应用用例；领域模型不得依赖 Spring MVC DTO；应用层通过 Port 调用能力。

## Knowledge 骨架

当前只实现模拟 `KnowledgeDocument`、`KnowledgeVersion`、生命周期状态、应用用例和内存 Repository Adapter。只有完成 `DRAFT → IN_REVIEW → APPROVED → PUBLISHED` 的版本才标记为可检索；没有导入 API、真实文档、解析、索引或外部存储。

## 未来模块（DRAFT）

- Retrieval：权限过滤、查询、排序和 Evidence；
- Generation：结构化建议、引用绑定、护栏与降级；
- Feedback：采纳、忽略、继续提交和结果回流；
- Policy/Audit：身份策略快照、门禁、事件和脱敏审计；
- Integration：AIOps、ITSM、对象存储和外部模型适配。

依赖只能由 API 指向 application、application 指向 domain/port、infrastructure 实现 port。领域层禁止依赖 Controller、HTTP DTO、数据库 SDK、模型 SDK或外部系统协议。在线 Precheck 禁止同步执行离线知识导入。

可独立部署候选仅包括离线知识处理与高负载检索/生成 Adapter；是否拆分必须由容量、发布和权限边界证明。
