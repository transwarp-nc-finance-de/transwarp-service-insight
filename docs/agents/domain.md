# Domain docs

- Status: `ACTIVE`
- Owner: Transwarp Service Insight Maintainers
- Last reviewed: 2026-07-14
- Source of truth for: 工程 Skill 消费现有领域文档的顺序与约束

工程 Skill 在探索和实施前，应按 `AGENTS.md` 的事实优先级读取现有领域文档。

## Single-context layout

本仓库采用单上下文结构，不创建根级 `CONTEXT.md`、`CONTEXT-MAP.md` 或平行事实来源。

主要事实来源：

- 范围与项目状态：`docs/project/`
- 产品需求与业务术语：`docs/product/`
- 当前和目标架构：`docs/architecture/`
- 长期架构决策：`docs/architecture/decisions/`
- 已实现 API 契约：`docs/api/openapi.yaml`
- 已批准但尚未实现的 v2 契约：`docs/api/openapi-v2.yaml`
- v1/v2 演进关系：`docs/api/v1-v2-mapping.md`

## Consumer rules

- 始终先读取根目录 `AGENTS.md`
- 遵循其中定义的事实优先级
- 已实现接口以 `docs/api/openapi.yaml` 为唯一契约
- v2 在实现完成前必须明确标注为尚未实现
- 使用现有产品和架构文档中的领域术语，不随意创建同义词
- 输出与 ADR 冲突时必须显式指出，不得静默覆盖
- 发现代码与文档冲突时必须显式报告
- 新的长期、高成本架构决策写入 `docs/architecture/decisions/`
