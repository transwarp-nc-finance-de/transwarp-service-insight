# 测试计划

## 目的

本文档定义技术 MVP M1、M2 和后续目标架构的测试范围。

## 适用范围

适用于 QA、产品、前端和后续 RAG、LLM、安全、集成团队评审。当前阶段验证本地 Vue 前端、Spring Boot Mock API、本地身份/PostgreSQL 切片、OpenAPI 契约和人工降级边界。

## 正文

当前阶段测试包括：后端单元与 API 行为测试、Spotless 格式检查、前端组件行为测试、ESLint、Prettier、生产构建、版本锁定的 OpenAPI 校验、三服务镜像构建及 Compose 冒烟。API 测试验证 v1 健康/预诊/追问/反馈、v2 AuthSession、知识治理，以及持久化预诊的创建、规范化幂等、多轮补充、上限、恢复、权限与显式终止；数据库测试验证并发幂等键串行化、知识审核历史和预诊 Run 快照的不可变写保护；组件测试覆盖模拟登录、知识审核和 v2 预诊恢复/补充/继续人工提交；Compose 使用真实本地 PostgreSQL 和文件 volume 验证迁移、并发创建与重启恢复。

Issue #16 的 `mock-eval-v1` 固定模拟评估集由独立 CI 门禁校验样例、身份、产品线、Evidence 引用、权限拒绝目标、覆盖矩阵、三轮上限、安全内容、稳定排序和 checksum；维护与升级命令见[固定模拟评估集维护指南](evaluation-dataset.md)。Issue #27 另以 API 行为测试验证持久化异步 EvaluationRun、实际 v2 检索轨迹、四项门禁、安全失败分页和事件聚合；这些门禁不声称生产效果。

## 一期实施测试 seam

测试只验证公共行为和复杂、确定性的领域规则。不得测试私有方法、内部调用次数、Mapper 的机械调用关系、Spring Bean 是否被调用、Vue `ref` 内部状态或 Repository 实现细节。

### API seam

- 当前门禁同时校验已实现的 v1 OpenAPI 和 `APPROVED_FOR_IMPLEMENTATION`、`NOT_IMPLEMENTED` 的 v2 DRAFT OpenAPI；任一契约结构无效都必须使本地检查和 CI 失败。
- v1 行为测试继续覆盖健康、初始预诊、Session/Run、追问、反馈、校验错误和安全异常响应，证明字段、状态码和行为保持兼容。
- 已实现的 v2 AuthSession 切片通过 HTTP 公共行为验证四个模拟身份、Cookie、CSRF Token 发放/恢复/轮换/过期/退出、RBAC/产品线矩阵以及 `401 UNAUTHENTICATED`、`403 CSRF_VALIDATION_FAILED`。后续 v2 路径再按各自 Ticket 验证安全 `404`、状态机、错误码、幂等、任务状态、Run 上限和降级；不得把 AuthSession 测试用于声称整体 v2 已经可用。

### 领域 seam

只测试复杂且确定的业务规则，包括知识生命周期与职责分离、ParseTask/IndexTask 三次尝试上限、原子发布前置条件、完整度策略、置信度规则、Session/Run 终止语义、RRF 融合与稳定排序。本地身份阶段的角色/产品线矩阵通过公共 API 与真实种子联合验证；ADMIN 只能得到 ADMIN，不隐含审核或继续提交能力。不为 DTO 映射、简单访问器或框架装配编写领域测试。

### 浏览器 seam

测试用户可见的关键纵向流程，包括模拟登录/刷新恢复/切换/退出、知识上传与解析预览、提交/审核/发布、授权预诊、多轮补充、Evidence 查看、独立 Feedback、独立 SubmissionContinuation、Audit 及 Evaluation/Metrics 页面。当前模拟身份断言用户看到 `模拟数据` 标识、角色和授权产品线，不检查 CSRF `ref` 等 Vue 内部状态。未修改页面的变更仍应运行现有 v1 前端行为测试作为兼容回归。

Issue #24 额外验证两个命令的独立成功/失败/重试与幂等冲突、终态只读、角色和产品线安全 404、结构化审计脱敏/分页/筛选，以及 H2 和 PostgreSQL 对 Feedback、Continuation、AuditEvent 的数据库级更新与删除拒绝。

### 系统 seam

验证 Compose 服务健康、关键纵向闭环、容器重启持久化，以及显式 FTS 降级和运行中 Embedding 故障。当前完整四服务 Compose 必须同时回归无认证 v1，并验证 PostgreSQL/pgvector 迁移、固定制品真实离线 768 维推理、双索引发布、登录与重启恢复；显式 `compose.fts-only.yaml` 不启动 embedding 服务且应用仍健康。

### PostgreSQL/pgvector 专项 seam

仅验证必须依赖 PostgreSQL/pgvector 的行为。当前专项覆盖空库迁移、版本化种子、约束、重复启动幂等、AuthSession/预诊恢复，以及 `vector(768)`、IndexTask 幂等/最多三次尝试、双索引原子切换、失败时旧版本继续服务和不可变尝试历史；Issue #26 进一步覆盖真实 SQL 的查询前权限/当前发布版本过滤、FTS/向量各 Top 20、RRF `k=60`、Top 5 稳定排序、检索审计与 Evidence 快照不可变、逐次重新授权和故障降级。测试只在 Embedding Port 边界模拟服务故障，不用恒真断言或手写命中映射伪造检索通过。

可直接执行的本地命令及 Windows、macOS/Linux 差异统一见根目录 [README 的“测试与构建”](../../README.md#9-测试与构建)，本文只定义测试范围和通过标准。

若验收不再需要运行服务，应执行 `docker compose down --remove-orphans` 并确认无残留容器；用户明确要求继续验收时可保持服务运行并在交接中说明。测试数据必须标注 `模拟数据`；错误或信息不足不得阻断人工继续提交，智能建议不得替代人工审核。

后续阶段测试包括接口契约测试、经授权的检索评估、集成测试、降级测试、审计测试和权限测试。真实生成式模型仍不属于一期范围；相关安全测试只能在另行授权后开展。

任何后续真实数据测试都必须经过授权、脱敏和安全评审。
