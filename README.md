# Transwarp Service Insight

## 目的

本文档说明 `Transwarp Service Insight` 当前项目定位、阶段边界、目录结构、文档导航和 Demo 查看方式。

## 适用范围

适用于项目介绍、Demo 查看、方案评审、AI Agent 协作和后续任务拆解。当前仓库仅包含文档、纯前端静态 Demo、mock 数据、PRD 和架构设计草案，不包含后端代码、真实接口接入、真实 RAG、真实大模型、真实 AIOps、真实 Wiki、真实历史 SLA 或数据库接入。

## 项目简介

`Transwarp Service Insight` 是面向交付与服务流程的 AI 服务洞察与效率提升项目。当前对外能力名称为 `SLA 智能预诊助手`。

`第一阶段：纯前端 SLA 智能预诊 Demo` 已于 2026/07/10 完成人工授权确认并关闭。本阶段验证了在 SLA 提交前通过模拟检索资料、生成辅助建议和提示补充信息来改善提交质量；当前仅准备下一阶段决策材料，尚未进入一期可试点系统设计评审、真实系统开发或试点实施。所有输出均需人工审核，不代表最终根因、最终处理方案或正式复盘结论。

## 当前边界

- 当前已完成：纯前端 Demo、mock 数据说明、PRD、架构设计基线、实施计划、任务拆分和各专题设计草案。
- 当前未完成：真实 ITSM、AIOps、RAG、LLM、Wiki、历史 SLA、数据库、后端服务和生产部署。
- 当前禁止：自动提交 SLA、自动闭单、自动判责、自动变更生产配置、使用未脱敏真实客户数据。
- 所有 Demo 数据和示例均必须标注为 `模拟数据`。

## 文档导航

- 项目范围与路线：`docs/00-project/`
- 产品需求：`docs/01-requirements/`
- 系统架构：`docs/02-architecture/`
- API 草案：`docs/03-api/`
- RAG 设计：`docs/04-rag/`
- LLM 生成设计：`docs/05-llm/`
- 知识库设计：`docs/06-knowledge/`
- 系统集成：`docs/07-integration/`
- 安全与幻觉防护：`docs/08-security/`
- 观测与指标：`docs/09-observability/`
- 测试验收：`docs/10-testing/`
- 提示词归档：`prompts/`
- 纯前端 Demo：`prototypes/sla-precheck-demo.html`

## 关键文档

- `docs/00-project/mvp-scope.md`：第一阶段 MVP 范围。
- `docs/00-project/implementation-plan.md`：从 Demo 到可试点系统设计的实施计划。
- `docs/00-project/demo-acceptance-review-2026-07-10.md`：Demo 浏览器技术验收、人工确认与第一阶段关闭结论。
- `docs/00-project/next-stage-decision-gate.md`：下一阶段范围、指标、授权与人工责任门禁。
- `docs/00-project/task-breakdown.md`：后续任务拆分。
- `docs/01-requirements/prd-sla-precheck-assistant.md`：一期 PRD。
- `docs/02-architecture/architecture-design-v1.0.md`：由架构设计 docx 整合形成的 Markdown 架构基线。
- `docs/03-api/api-contract-draft.md`：后续接口草案，不代表真实接口。

## 如何查看 Demo

直接用浏览器打开：

```text
prototypes/sla-precheck-demo.html
```

该文件是纯前端静态页面，不需要启动后端服务，不安装依赖，也不会访问真实接口。

## 后续演进方向

后续可在授权、脱敏、权限和审计评审通过后，逐步评估真实知识库检索、RAG 与大模型总结、AIOps 辅助分析、SLA 表单智能补全、人工确认后的 ITSM / AIOps 对接和复盘草稿生成。这些均为后续目标，不属于当前第一阶段已实现能力。
