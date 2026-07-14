# 文档导航

状态定义：`ACTIVE` 为当前事实，`DRAFT` 为未实现设计，`ARCHIVED` 仅供历史追溯。

首次安装、启动、验收、故障排查、升级或清理请从根目录 [M2 本地安装部署手册](../README.md) 开始。

## ACTIVE

- 项目：[范围](project/scope.md)、[Backlog](project/backlog.md)、[重构审计](project/refactoring-audit.md)、[路线图](project/roadmap.md)、[待确认问题](project/open-questions.md)
- 产品：[PRD](product/prd.md)、[用户流程](product/user-flow.md)、[角色权限](product/roles-and-permissions.md)、[指标](product/metrics.md)、[验收标准](product/acceptance-criteria.md)
- 架构：[导航](architecture/overview.md)、[当前架构](architecture/current-architecture.md)、[模块边界](architecture/module-boundaries.md)、[在线流程](architecture/online-precheck-flow.md)、[安全边界](architecture/security-boundaries.md)、[数据模型](architecture/data-model.md)、[决策记录](architecture/decisions.md)、[ADR-0005 混合检索](architecture/decisions/ADR-0005-hybrid-retrieval.md)、[ADR-0006 API v2 演进](architecture/decisions/ADR-0006-v2-api-evolution.md)
- 开发：[M2 安装部署](../README.md)、[本地开发](development/local-development.md)、[测试策略](development/test-strategy.md)
- 运维治理：[可观测性](operations/observability.md)、[数据保留与删除](operations/data-retention.md)
- 接口：[OpenAPI](api/openapi.yaml)

## DRAFT / ARCHIVED

[API v2 候选契约](api/openapi-v2.yaml)、[v1→v2 映射](api/v1-v2-mapping.md)、[目标架构](architecture/target-architecture.md)、[离线知识流程](architecture/knowledge-ingestion-flow.md) 与 `architecture/drafts/` 中的材料均为未实现目标设计。API v2 文档只负责进入实现前的候选契约与兼容边界，人工批准前保持 `NOT_READY`；`archive/` 保存 Demo、旧计划、旧 API 草案、源文档和历史提示词，不作为当前入口。

## 文档内容应该写在哪里

优先更新已有文档。仅当现有文档无法承载、主题独立且稳定、边界明确，并且不会产生重复事实来源时才新增文档；新增后需加入上方导航，并标注状态与事实职责。

| 内容 | 归档位置 | 事实职责 |
| --- | --- | --- |
| 项目范围 | [项目范围](project/scope.md) | 当前目标、边界与非目标 |
| Backlog | [项目 Backlog](project/backlog.md) | 待办事项与优先级 |
| 待确认问题 | [待确认问题](project/open-questions.md) | 尚未确认、需要持续跟踪的问题 |
| 产品需求、流程、权限、指标与业务术语 | [`product/`](product/) | 产品语义与验收口径；业务术语先归入相关产品文档 |
| 当前架构与未来设计 | [`architecture/`](architecture/) | 已实现架构事实与明确标为 DRAFT 的目标设计 |
| 长期、高成本架构决策 | [`architecture/decisions/`](architecture/decisions/) | 按 `ADR-NNNN-*.md` 记录多方案决策及其权衡 |
| 已实现 API | [OpenAPI](api/openapi.yaml) | 已发布接口的唯一契约 |
| 开发说明 | [`development/`](development/) | 本地开发与测试方法 |
| 运维治理 | [`operations/`](operations/) | 可观测性、数据保留等运维规则 |
| 历史材料 | [`archive/`](archive/) | 仅供历史追溯，不作为当前实施依据 |

不要默认在根目录创建 `CONTEXT.md`、`DOMAIN.md`、`DECISIONS.md`、`PLAN.md`、`SPEC.md` 或 `TODO.md`。新建 ACTIVE 文档或重要 DRAFT 文档时，使用 `Status`、`Owner`、`Last reviewed`、`Source of truth for` 轻量头信息。

## 需求访谈 Skill

访谈记录应区分 `CONFIRMED`、`ASSUMPTION`、`OPEN QUESTION`、`EXTERNAL QUESTION`，分别表示用户已确认、可撤销假设、需项目成员确认、需外部责任方确认。访谈只做澄清与记录，不自动越级生成实施计划或编码；用户直接下达明确实现任务时，可按该任务授权直接实施。假设和未决问题不得写成当前事实，需要长期跟踪的问题归入 [待确认问题](project/open-questions.md)。
