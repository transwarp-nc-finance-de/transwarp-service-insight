# 变更记录

## 目的

本文档用于记录 `Transwarp Service Insight` 的重要需求边界、文档规则、目录结构和 Demo 原型变更，便于后续追溯。

## 适用范围

适用于本仓库内由 AI Agent 或人工协作完成的重要项目修改。当前仅记录文档、纯前端 Demo、mock 数据、架构设计和规则层面的变更，不代表真实系统接入、生产上线或客户环境变更。

## 记录规则

1. 按日期倒序记录，最新日期放在最上方。
2. 日期格式统一为 `YYYY/MM/DD`。
3. 同一天的多条变更必须合并到同一个日期标题下。
4. 每条变更使用 `(1)`、`(2)`、`(3)` 编号。
5. 每个编号项代表一个功能主题或变更目标，不代表单个文件动作。
6. 纯前端 Demo 或 `模拟数据` 变更必须明确边界；未接真实系统、未接真实接口或需人工审核的边界必须保留。

## 变更记录

### 2026/07/06

(1) 架构文档整合与目录重组
摘要：将 `docs/` 从扁平目录重组为 00 到 10 的分层文档结构，并将 `Transwarp Service Insight 系统架构设计方案v1.0.docx` 整合为 Markdown 架构基线，用于后续架构评审和任务拆分。
- 新增 `docs/00-project/` 到 `docs/10-testing/` 分层目录。
- 迁移 MVP、路线、进度、PRD、mock 知识、Demo 评审材料和 PRD HTML 到对应目录。
- 新增 `docs/02-architecture/architecture-design-v1.0.md`，原 docx 作为源材料保存在架构目录。
- 当前仍为 Demo / PRD / 架构设计阶段，不代表已接真实 ITSM、AIOps、RAG、LLM、Wiki、历史 SLA 或数据库。

(2) 后续目标架构专题设计草案
摘要：补齐实施计划、任务拆分、API、RAG、LLM、知识库、集成、安全、观测和测试验收文档，用于后续评审，不作为当前已实现能力。
- 新增 `implementation-plan.md`、`task-breakdown.md`、`api-contract-draft.md`、`rag-design.md`、`prompt-design.md`、`knowledge-lifecycle.md`、`itsm-integration.md`、`security-and-guardrails.md`、`observability-and-metrics.md` 和测试计划等文档。
- 所有接口、模型、检索、集成和部署内容均为草案或目标架构说明，不写真实接口地址、鉴权实现、后端代码或生产部署脚本。
- 保留人工审核边界：不自动提交 SLA，不自动闭单，不输出最终根因或最终处理结论。

(3) 协作规则、导航、进度和提示词归档同步
摘要：更新 README、AGENTS、roadmap、progress 和提示词归档，使项目导航、允许事项、进度记录和变更记录与架构整合后的目录保持一致。
- `README.md` 更新文档导航和当前阶段说明。
- `AGENTS.md` 增加当前阶段允许架构文档整合、目录调整和设计草案编写，同时保留全部禁止事项。
- `docs/00-project/roadmap.md` 和 `docs/00-project/progress.md` 补充架构整合后的阶段状态。
- 归档本次任务提示词到 `prompts/2026-07-06-01-architecture-integration-directory-and-task-breakdown.md`。
- 清理误打包本地目录 `%SystemDrive%/`；`.gitignore` 已保留相关忽略规则。

### 2026/07/03

(1) 项目协作规则与提示词归档规范
摘要：完善 AI Agent 协作边界、提示词归档和 CHANGELOG 主题化记录规则，用于保证后续协作可追溯且避免文件级流水账。

(2) 第一阶段 `SLA 智能预诊助手` Demo 范围收敛
摘要：将项目当前阶段收敛为纯前端 SLA 智能预诊 Demo，用于演示提交前预诊价值，同时明确不代表真实系统接入。

(3) 一期 `SLA 预诊助手` PRD 与评审材料
摘要：完成 PRD 评审稿和静态 HTML 展示页，用于明确试点范围、人工审核边界和 AIOps 产品预留要求。

(4) 仓库基础配置维护
摘要：完善仓库忽略规则，用于降低本地缓存、敏感配置和构建产物误提交风险。
