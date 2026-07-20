# AI Agent 协作规范

仓库处于 `TECHNICAL_MVP`，聚焦 SLA 智能预诊助手。事实优先级为：用户当前明确指令 → `AGENTS.md` → `docs/project/scope.md` → v1 的 `docs/api/openapi.yaml` 与 v2 `docs/api/openapi-v2.yaml` 中标记 `IMPLEMENTED` 的 operation → `docs/architecture/current-architecture.md` → ACTIVE 文档 → DRAFT 设计 → ARCHIVED 历史材料。代码与文档冲突时必须显式报告，不得静默选择一方。

- 使用中文；仅使用模拟、公开或脱敏数据，模拟内容标注 `模拟数据`。
- 智能输出包含依据来源、置信度、人工介入建议和待补充信息。
- 建议不得成为最终根因、最终方案或正式复盘结论，不得阻断人工继续提交。
- SLA 提交及内容均由人工确认。
- 测试必须验证真实行为，不得用恒真断言或跳过关键路径制造通过结果。
- 未经授权不得接入真实外部服务、数据库、RAG、模型或生产环境。
- 不得直接提交 `main`；使用主题分支和 PR，不自动合并、不强推。
- 已实现 v1 接口以 `docs/api/openapi.yaml` 为唯一 v1 契约；已实现 v2 接口以 `docs/api/openapi-v2.yaml` 中 operation 级 `IMPLEMENTED` 为唯一 v2 契约，未实现的未来设计保持 `NOT_IMPLEMENTED / DRAFT`。
- 真实数据、外部服务、破坏性操作、已发布接口兼容性或重大歧义须人工确认。

## 文档治理与需求访谈规则

### 复用现有文档体系

- 默认在现有 `docs/` 体系中补充内容，不创建根目录平行事实来源，包括 `CONTEXT.md`、`DOMAIN.md`、`DECISIONS.md`、`PLAN.md`、`SPEC.md`、`TODO.md`。
- 范围写入 `docs/project/scope.md`，Backlog 写入 `docs/project/backlog.md`，待确认问题写入 `docs/project/open-questions.md`。
- 产品需求与业务术语写入 `docs/product/`，架构现状与设计写入 `docs/architecture/`，长期架构决策写入 `docs/architecture/decisions/`。
- 已实现 v1 API 契约写入 `docs/api/openapi.yaml`；v2 契约及 operation 级实施状态写入 `docs/api/openapi-v2.yaml`。开发说明写入 `docs/development/`，运维治理写入 `docs/operations/`，历史材料写入 `docs/archive/`。
- 仅当现有文档无法承载、主题独立且稳定、边界明确，并且不会产生重复事实来源时，才新增文档。新增后必须同步加入 `docs/README.md` 导航，并标注状态与事实职责；此规则不禁止满足条件的合理新增文档，也不引入逐级审批。
- 新建 ACTIVE 文档或重要 DRAFT 文档采用轻量头信息：`Status`、`Owner`、`Last reviewed`、`Source of truth for`。无需为本规则追溯改造全部已有文档。
- ADR 仅记录存在多个可行方案且具有长期、高成本影响的决策，沿用 `docs/architecture/decisions/ADR-NNNN-*.md` 命名；一般实现选择直接记录在对应主题文档中。

### 需求访谈 Skill

- 访谈结论必须区分：`CONFIRMED`（用户明确确认）、`ASSUMPTION`（为推进工作作出的可撤销假设）、`OPEN QUESTION`（需用户或项目成员确认）、`EXTERNAL QUESTION`（需外部系统、供应商或其他责任方确认）。
- 访谈 Skill 只负责澄清和记录，不得自动越级生成实施计划或开始编码。用户直接下达明确实现任务时，可按任务授权直接规划和实施，不受此限制。
- 假设与未决问题不得伪装成已确认事实；需要长期跟踪的内容归入 `docs/project/open-questions.md`，外部结论返回后再由人工确认其状态。

## Agent skills

### Issue tracker

本仓库使用 GitHub Issues 跟踪 Spec 和实施 Ticket。详见 `docs/agents/issue-tracker.md`。

### Triage labels

使用五个默认 Triage 角色标签。详见 `docs/agents/triage-labels.md`。

### Domain docs

采用单上下文布局，复用现有 `docs/` 文档体系与 `docs/architecture/decisions/` ADR。详见 `docs/agents/domain.md`。
