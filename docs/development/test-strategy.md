# 测试计划

## 目的

本文档定义技术 MVP M1、M2 和后续目标架构的测试范围。

## 适用范围

适用于 QA、产品、前端和后续 RAG、LLM、安全、集成团队评审。当前阶段验证本地 Vue 前端、Spring Boot Mock API、OpenAPI 契约和人工降级边界。

## 正文

当前阶段测试包括：后端单元与 API 行为测试、Spotless 格式检查、前端组件行为测试、ESLint、Prettier、生产构建、版本锁定的 OpenAPI 校验、前后端镜像构建及 Compose 冒烟。API 测试验证健康、初始预诊、追问、反馈、校验错误和安全异常响应；领域测试覆盖会话/运行约束、知识审核发布状态；审计测试验证不记录反馈正文。组件测试覆盖多轮顺序、失败保留、反馈以及人工继续提交硬边界。

## 一期实施测试 seam

测试只验证公共行为和复杂、确定性的领域规则。不得测试私有方法、内部调用次数、Mapper 的机械调用关系、Spring Bean 是否被调用、Vue `ref` 内部状态或 Repository 实现细节。

### API seam

- 当前门禁同时校验已实现的 v1 OpenAPI 和 `APPROVED_FOR_IMPLEMENTATION`、`NOT_IMPLEMENTED` 的 v2 DRAFT OpenAPI；任一契约结构无效都必须使本地检查和 CI 失败。
- v1 行为测试继续覆盖健康、初始预诊、Session/Run、追问、反馈、校验错误和安全异常响应，证明字段、状态码和行为保持兼容。
- 后续 v2 实施通过 HTTP 公共行为验证认证、CSRF、RBAC、产品线授权、安全 `404`、状态机、错误码、幂等、任务状态、Run 上限和降级；API 测试不得被用来声称尚未实现的 v2 已经可用。

### 领域 seam

只测试复杂且确定的业务规则，包括知识生命周期与职责分离、ParseTask/IndexTask 三次尝试上限、原子发布前置条件、完整度策略、置信度规则、Session/Run 终止语义、RRF 融合与稳定排序。不为 DTO 映射、简单访问器或框架装配编写领域测试。

### 浏览器 seam

测试用户可见的关键纵向流程，包括模拟登录、知识上传与解析预览、提交/审核/发布、授权预诊、多轮补充、Evidence 查看、独立 Feedback、独立 SubmissionContinuation 及 Audit/Evaluation 页面。测试用户操作及可见结果，不测试 Vue 内部实现。未修改页面的变更仍应运行现有 v1 前端行为测试作为兼容回归。

### 系统 seam

验证 Compose 服务健康、关键纵向闭环、容器重启持久化，以及显式 FTS 降级和运行中 Embedding 故障。完整模式与降级模式必须分别有可观察断言。未引入目标依赖的变更继续运行当前 frontend/backend Compose v1 冒烟；只有经授权实现对应能力后才扩展 PostgreSQL、pgvector 或 local-embedding 系统断言。

### PostgreSQL/pgvector 专项 seam

仅验证必须依赖 PostgreSQL/pgvector 的行为：查询前权限过滤、唯一约束、任务并发/幂等、双索引成功后的原子版本切换、旧版本继续服务、FTS/向量/RRF 排序及重启恢复。该 seam 在对应能力实现前不适用；不得以 Mock 或恒真断言伪造专项通过。

可直接执行的本地命令及 Windows、macOS/Linux 差异统一见根目录 [README 的“测试与构建”](../../README.md#9-测试与构建)，本文只定义测试范围和通过标准。

若验收不再需要运行服务，应执行 `docker compose down --remove-orphans` 并确认无残留容器；用户明确要求继续验收时可保持服务运行并在交接中说明。测试数据必须标注 `模拟数据`；错误或信息不足不得阻断人工继续提交，智能建议不得替代人工审核。

后续阶段测试包括接口契约测试、经授权的检索评估、集成测试、降级测试、审计测试和权限测试。真实生成式模型仍不属于一期范围；相关安全测试只能在另行授权后开展。

任何后续真实数据测试都必须经过授权、脱敏和安全评审。
