# 技术 MVP Backlog

## 目的

按里程碑管理技术 MVP 工作，避免将真实集成混入 M1/M2。

## 适用范围

适用于研发排期、验收和后续试点准备。

## 正文

### M1（已完成）

完成日期：2026/07/12。交付依据：PR #3，merge commit `4da5765933906c79508a63e6fd4a200d2a1a593c`。

- [x] Spring Boot Mock 预诊接口、DTO、校验、异常响应。
- [x] 确定性模拟结果、缺失信息判断、人工审核硬边界。
- [x] Vue SLA 表单、预诊结果面板、失败降级和继续提交模拟。
- [x] 后端单元/API 测试与前端组件测试。
- [x] 本地启动、构建、测试和联调说明。

### M2（本地工程化交付）

- [x] Java 21 后端与 Nginx 前端多阶段交付镜像。
- [x] 根目录 Compose 一键启动，仅向宿主机暴露前端入口。
- [x] 契约化进程健康接口与 OpenAPI 规范校验门禁。
- [x] 镜像构建、Compose 健康/API/降级冒烟 CI。
- [x] 保留非容器开发方式并补齐本地验收文档。

M2 仅代表本地工程验收，不代表真实试点、业务签字、生产验收或上线批准。

### M3（架构骨架，已完成）

- [x] Controller 依赖 Use Case，应用层通过 Port 调用 Mock Adapter。
- [x] HTTP DTO 与领域模型分离，定义 Session、Run、状态和 NextAction。
- [x] Mock 迁入 infrastructure，保持 API 与页面主流程兼容。
- [x] 当前架构、目标架构、模块边界和数据模型分离维护。

### M4（反馈、审计与权限骨架，已完成 Mock 范围）

- [x] 模拟反馈接口和前端采纳、部分采纳、忽略入口。
- [x] 模拟身份与策略版本快照。
- [x] 预诊、补充信息、反馈和继续提交业务事件。
- [x] 内存反馈与审计 Adapter；审计不记录输入或反馈正文。
- [x] 继续人工提交始终可用，真实提交仍不存在且由人工确认。

### 一期本地完整纵向闭环（CONFIRMED 范围，IMPLEMENTED）

以下是已确认范围的一期交付清单。完整 `/api/v2` OpenAPI 及 v1→v2 映射已获人工批准；API v2 仍为 `DRAFT`，整体实施状态为 `PARTIALLY_IMPLEMENTED`，批准状态为 `APPROVED_FOR_IMPLEMENTATION`，一期实施状态为 `IMPLEMENTED`。当前实现为兼容的 `v1 Mock` 加 v2 AuthSession、知识上传/治理/发布、持久化 Precheck Session/Run、授权混合 Retrieval 与 Evidence、独立 Feedback、SubmissionContinuation、结构化 AuditEvent、EvaluationRun、Metrics 与仅限本地环境的受控 Admin Reset；Issue #29 已完成完整四服务、FTS-only、运行时故障恢复、重启持久化和 v1 回归验收。

- [x] 本地 Mock 登录、退出、身份切换与最小身份 UI（`模拟数据`）。
- [x] 服务端 Cookie Session 与 CSRF 校验。
- [x] 后端四角色 RBAC 授权。
- [x] 后端产品线范围过滤。
- [x] PostgreSQL 本地身份、目录种子与 AuthSession 持久化及 Flyway 迁移。
- [x] pgvector、IndexTask 与知识发布状态持久化。
- [x] Admin Reset 任务状态、幂等记录与成功/失败审计持久化；Evaluation 与 Metrics 已完成。
- [x] 仅限本地环境、需 `ADMIN` 二次确认并记录审计的受控重置。
- [x] 原始知识文件 Compose volume 与 `OriginalFileStorage` 存储边界。
- [x] Markdown/TXT/文本型 PDF 首次上传、异步 ParseTask、解析摘要、Block 与 Chunk 分页预览。
- [x] 不可变草稿修订、职责分离送审、退回与批准。
- [x] 双索引成功后原子发布及旧版本废弃。
- [x] 基于已建 PostgreSQL FTS + 本地 E5 向量索引的授权在线检索、固定 RRF 融合、FTS/Embedding 降级与不可变 Evidence 引用（Issue #26）。
- [x] v2 Precheck Session/Run、版本化完整度与置信度策略、最多三轮补充。
- [x] Issue #24：独立 Feedback、SubmissionContinuation 与按产品线授权的脱敏结构化审计。
- [x] Issue #27：聚合指标和最小评估页。
- [x] 不少于 30 条的固定模拟评估集及权限、引用、降级、Recall@5 工程门禁定义与校验。
- [x] Issue #29：默认完整 Compose 四服务、显式 FTS-only、运行时 Embedding 故障恢复、重启持久化与浏览器纵向闭环验收。

### 后续 MVP

### 知识生命周期骨架（已完成 Mock 范围）

- [x] 模拟 KnowledgeDocument、KnowledgeVersion 和权限范围元数据。
- [x] 审核、批准、发布与废弃状态约束。
- [x] 只有 PUBLISHED 版本可被未来检索端口使用。
- [x] 内存 Repository 与领域行为测试；无真实文档、导入 API、解析或索引。

以下工作不属于一期本地闭环，仍未批准：

- [ ] 真实知识源、真实历史 SLA、真实生成式模型或外部模型接口。
- [ ] 正式 SSO/JWT、真实组织权限与生产审计体系。
- [ ] AIOps 只读辅助信息、问题附件内容访问和真实 SLA 提交。
- [ ] 经评审的容器热更新开发模式（如确有需要）。

### 真实试点前

- [ ] 数据授权、保留周期、脱敏和来源白名单评审。
- [ ] 外部系统契约、权限、安全、审计和降级验收。
- [ ] SLA 人工提交责任、支持边界和回滚方案。
- [ ] 真实试点门禁与人工批准。

未完成项均为候选工作，不代表已接入或已承诺上线。
