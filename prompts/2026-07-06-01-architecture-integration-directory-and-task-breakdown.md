# Codex 提示词：Transwarp Service Insight 架构文档整合、目录调整与任务拆分

## 使用时间

2026/07/06

## 使用目的

将《Transwarp Service Insight 系统架构设计方案v1.0.docx》整合到当前 `transwarp-service-insight` 项目中，形成清晰的项目文档结构、架构设计基线、实施计划、任务拆分、接口草案、RAG 设计、安全防护设计、指标闭环设计和后续开发依据。

## 当前项目阶段判断

当前仓库仍处于：

- 纯前端 SLA 智能预诊 Demo；
- PRD / MVP 范围说明；
- mock 数据展示；
- 架构评审准备；
- 提示词归档；
- 后续工程化方案设计阶段。

当前仓库不是生产系统，不代表已经接入真实 ITSM、真实 AIOps、真实 SLA、真实知识库、真实 RAG、真实大模型、真实数据库或真实客户数据。

本次任务只做：

- 目录结构整理；
- 文档整合；
- 架构设计沉淀；
- 实施路线规划；
- 任务拆分；
- 接口草案；
- 后续开发边界说明；
- CHANGELOG 更新；
- 提示词归档。

本次任务不做：

- 不开发真实后端服务；
- 不接入真实 RAG；
- 不接入真实大模型；
- 不接入真实 AIOps；
- 不接入真实 ITSM；
- 不接入真实数据库；
- 不调用真实接口；
- 不使用真实客户数据；
- 不写生产部署脚本；
- 不创建 K8s / Helm / GPU 推理服务代码；
- 不自动提交 SLA；
- 不自动闭单；
- 不把 AI 预诊结果描述为最终根因或最终处理结论。

---

# 一、任务总目标

你现在在 `transwarp-service-insight` 仓库中工作。

请将《Transwarp Service Insight 系统架构设计方案v1.0.docx》的内容整合到当前项目中，形成可维护的项目架构文档、实施计划、任务拆分和后续开发依据。

请先阅读并理解以下现有文件：

- `README.md`
- `AGENTS.md`
- `CHANGELOG.md`
- `docs/mvp-scope.md`
- `docs/prd-sla-precheck-assistant.md`
- `docs/roadmap.md`
- `docs/progress.md`
- `docs/mock-knowledge.md`
- `prompts/README.md`
- `prototypes/sla-precheck-demo.html`

如果部分文件不存在，请不要报错中断，应根据实际仓库情况继续处理，并在变更说明中注明。

---

# 二、严格边界

请严格遵守以下边界：

1. 不生成真实后端业务代码。
2. 不调用真实接口。
3. 不接入真实 Wiki、真实历史 SLA、真实 AIOps、真实知识库或真实数据库。
4. 不使用真实客户、真实 SLA、真实日志、真实内部链接或未脱敏数据。
5. 不自动提交 SLA。
6. 不自动闭单。
7. 不把 AI 预诊结果描述为最终根因、最终处理结论或正式复盘结论。
8. 所有涉及示例的内容必须标注为“模拟数据”。
9. 所有后续工程能力必须描述为“后续阶段”“待评审”“待接入”“目标架构”或“设计目标”，不得描述为已完成。
10. 完成有意义变更后，必须更新 `CHANGELOG.md`。
11. 不要提前创建真实生产代码目录，例如 `backend/`、`rag-service/`、`llm-service/`、`k8s/`、`helm/` 等，除非仓库已有且需要维护。
12. 所有文档必须区分：
    - 当前已完成；
    - 当前 Demo 能力；
    - 一期试点目标；
    - 后续目标架构；
    - 明确不在当前阶段内的能力。

---

# 三、仓库清理

请检查仓库中是否存在误打包的本地系统目录，例如：

- `%SystemDrive%/`
- `ProgramData/`
- `SogouInput/`

如果存在，请删除这些无关目录。

同时检查 `.gitignore` 是否包含对此类目录的忽略规则。如果没有，请补充类似规则，避免再次提交本地系统文件。

示例规则：

```gitignore
%SystemDrive%/
ProgramData/
SogouInput/
*.tmp
*.log
.DS_Store
Thumbs.db
```

注意：不要删除项目正常文件。

---

# 四、调整文档目录结构

请先调整项目文档目录结构，将当前平铺在 `docs/` 下的文档整理为分层目录，便于后续架构评审、任务拆分、接口设计、RAG 设计、LLM 设计、安全设计和测试验收。

请创建以下目录：

```text
docs/00-project/
docs/01-requirements/
docs/02-architecture/
docs/03-api/
docs/04-rag/
docs/05-llm/
docs/06-knowledge/
docs/07-integration/
docs/08-security/
docs/09-observability/
docs/10-testing/
```

请将现有文档按以下规则移动：

```text
docs/mvp-scope.md → docs/00-project/mvp-scope.md
docs/roadmap.md → docs/00-project/roadmap.md
docs/progress.md → docs/00-project/progress.md
docs/prd-sla-precheck-assistant.md → docs/01-requirements/prd-sla-precheck-assistant.md
docs/mock-knowledge.md → docs/06-knowledge/mock-knowledge.md
```

如果某些文件不存在，请跳过并在变更说明中记录。

移动后请更新 `README.md`、`AGENTS.md` 和相关文档中的链接，避免出现失效链接。

---

# 五、建议最终目录结构

最终建议形成如下结构：

```text
transwarp-service-insight/
├── README.md
├── AGENTS.md
├── CHANGELOG.md
├── LICENSE
├── docs/
│   ├── 00-project/
│   │   ├── README.md
│   │   ├── mvp-scope.md
│   │   ├── roadmap.md
│   │   ├── progress.md
│   │   ├── implementation-plan.md
│   │   └── task-breakdown.md
│   │
│   ├── 01-requirements/
│   │   ├── prd-sla-precheck-assistant.md
│   │   ├── user-flow.md
│   │   └── acceptance-criteria.md
│   │
│   ├── 02-architecture/
│   │   ├── architecture-design-v1.0.md
│   │   ├── system-context.md
│   │   ├── module-boundary.md
│   │   ├── deployment-plan.md
│   │   └── evolution-plan.md
│   │
│   ├── 03-api/
│   │   ├── api-contract-draft.md
│   │   └── openapi-placeholder.md
│   │
│   ├── 04-rag/
│   │   ├── rag-design.md
│   │   ├── retrieval-design.md
│   │   ├── query-rewrite-design.md
│   │   └── evaluation-design.md
│   │
│   ├── 05-llm/
│   │   ├── prompt-design.md
│   │   ├── generation-design.md
│   │   ├── hallucination-guardrails.md
│   │   └── model-routing-design.md
│   │
│   ├── 06-knowledge/
│   │   ├── mock-knowledge.md
│   │   ├── knowledge-lifecycle.md
│   │   ├── knowledge-review-flow.md
│   │   └── metadata-schema.md
│   │
│   ├── 07-integration/
│   │   ├── itsm-integration.md
│   │   ├── aiops-integration.md
│   │   └── frontend-embed-design.md
│   │
│   ├── 08-security/
│   │   ├── security-and-guardrails.md
│   │   ├── data-masking.md
│   │   ├── prompt-injection-defense.md
│   │   └── audit-design.md
│   │
│   ├── 09-observability/
│   │   ├── observability-and-metrics.md
│   │   ├── event-tracking.md
│   │   └── dashboard-design.md
│   │
│   └── 10-testing/
│       ├── test-plan.md
│       ├── rag-evaluation-plan.md
│       ├── security-test-plan.md
│       └── acceptance-test-plan.md
│
├── prompts/
├── prototypes/
├── scripts/
├── data/
└── assets/
```

如果部分目录下只有一个文件，也可以先创建核心文件，不必强行补齐所有空文档。原则是：可以创建目录骨架，但不要创建大量没有内容的空文件。

---

# 六、新增项目总控文档

## 6.1 新增 `docs/00-project/README.md`

用于说明 `00-project` 目录的用途。

内容应包括：

- 本目录用于项目范围、路线、进度、实施计划、任务拆分。
- 本目录不代表系统已经进入生产开发。
- 当前项目仍处于 Demo / PRD / 架构设计阶段。
- 指向核心文档：
  - `mvp-scope.md`
  - `roadmap.md`
  - `progress.md`
  - `implementation-plan.md`
  - `task-breakdown.md`

---

## 6.2 新增 `docs/00-project/implementation-plan.md`

该文档要说明项目如何从当前 Demo 演进到可试点系统。

建议分为以下阶段：

### 第 0 阶段：仓库清理与文档基线

目标：

- 清理误提交文件。
- 整合架构文档。
- 调整文档目录。
- 更新 README、roadmap、progress、CHANGELOG。
- 统一项目边界。

交付：

- 架构文档 Markdown。
- 实施计划。
- 任务拆分。
- 接口草案。
- RAG 设计。
- 安全设计。
- 指标设计。
- 提示词归档。

边界：

- 不接真实系统。
- 不开发真实后端。
- 不使用真实数据。

### 第 1 阶段：纯前端 Demo 增强

目标：

- 优化 SLA 预诊入口。
- 优化预诊结果面板。
- 增强 mock 数据结构。
- 明确人工审核和模拟数据提示。
- 补充演示脚本。

边界：

- 不接真实 ITSM。
- 不接真实 AIOps。
- 不接真实 RAG。
- 不接真实大模型。
- 不接真实数据。

### 第 2 阶段：一期可试点系统设计

目标：

- 设计预诊服务接口。
- 设计检索服务接口。
- 设计反馈埋点接口。
- 设计知识审核接口。
- 设计 ITSM / AIOps 对接字段。
- 设计权限、脱敏、审计要求。

边界：

- 仅做接口草案和评审文档。
- 不写真实接口地址。
- 不接生产系统。

### 第 3 阶段：受控知识库与检索原型

目标：

- 文档解析。
- 文档切片。
- 元数据标注。
- 向量化。
- ES / 向量库混合检索。
- 重排序。
- 缓存策略。
- 检索评估。

边界：

- 只使用授权、脱敏、可审计的数据。
- 检索结果必须展示来源和适用范围。
- 不输出未经审核的正式结论。

### 第 4 阶段：RAG 与大模型预诊

目标：

- Prompt 模板。
- 结构化 JSON 输出。
- 引用来源。
- 置信度。
- 待补充信息。
- 幻觉拦截。
- 敏感信息过滤。
- 提示注入防御。
- 降级策略。

边界：

- 不输出最终根因。
- 不输出最终处理结论。
- 不自动闭单。
- 不自动提交 SLA。
- 不绕过人工确认。

### 第 5 阶段：ITSM / AIOps 集成与反馈闭环

目标：

- 与工单创建流程集成。
- 保存预诊结果字段。
- 记录采纳状态。
- 建立反馈埋点。
- 建立 ClickHouse / Grafana 指标看板。
- 建立知识复审流程。
- 建立闭环评估体系。

边界：

- AI 能力不可用时不能阻塞工单提交。
- 所有提交动作必须人工确认。
- 所有预诊结果必须保留审计记录。

---

## 6.3 新增 `docs/00-project/task-breakdown.md`

内容包括：

1. 模块拆分。
2. 任务列表。
3. 每个任务的输入。
4. 每个任务的输出。
5. 负责人角色。
6. 依赖关系。
7. 验收标准。
8. 风险边界。

建议角色包括：

- 项目负责人 / 产品经理。
- 前端工程师。
- 后端工程师。
- RAG / 检索工程师。
- 大模型 / 算法工程师。
- 知识库负责人。
- 平台 / DevOps 工程师。
- 安全合规负责人。
- 测试 / QA。

任务拆分必须覆盖：

- 文档整合。
- Demo 优化。
- PRD 补充。
- API 草案。
- RAG 检索设计。
- LLM 生成设计。
- 幻觉拦截。
- 知识库生命周期。
- ITSM / AIOps 集成。
- 埋点与指标。
- 降级与容灾。
- 安全与审计。
- 测试验收。

建议任务表示例：

```markdown
| 编号 | 模块 | 任务 | 输入 | 输出 | 负责人角色 | 依赖 | 验收标准 |
|---|---|---|---|---|---|---|---|
| T-001 | 项目文档 | 整理 docs 目录结构 | 现有 docs 文档 | 分层文档目录 | 项目负责人 | 无 | README 链接无失效 |
| T-002 | 架构设计 | 整合架构设计文档 | 架构设计方案 docx | architecture-design-v1.0.md | 架构负责人 | T-001 | 明确当前能力和目标能力 |
| T-003 | API | 编写接口草案 | PRD、架构文档 | api-contract-draft.md | 后端工程师 | T-002 | 接口字段清晰，无真实地址 |
```

---

# 七、新增需求文档

## 7.1 新增 `docs/01-requirements/user-flow.md`

内容包括：

- 工单创建前用户输入问题；
- 触发智能预诊；
- 展示模拟预诊结果；
- 用户选择采纳、部分采纳或忽略；
- 若采纳，则用户可自行处理并不提交工单；
- 若部分采纳，则继续提交工单并附带预诊摘要；
- 若忽略，则直接提交工单；
- 所有动作均为用户人工确认；
- 当前 Demo 不自动提交、不自动闭单。

请使用流程图或文本状态机表达。

示例：

```text
填写问题 → 触发预诊 → 展示建议
→ 采纳并自行处理
→ 部分采纳并继续提交
→ 忽略并直接提交
```

---

## 7.2 新增 `docs/01-requirements/acceptance-criteria.md`

内容包括：

- Demo 阶段验收标准；
- 文档阶段验收标准；
- 接口设计阶段验收标准；
- RAG 原型阶段验收标准；
- LLM 预诊阶段验收标准；
- ITSM / AIOps 集成阶段验收标准；
- 安全验收标准；
- 测试验收标准。

必须明确：

- 当前阶段只验收 Demo、PRD、架构文档和 mock 数据；
- 不验收真实生产功能；
- 不要求真实知识库命中率；
- 不要求真实模型生成质量；
- 不要求真实工单系统联调。

---

# 八、新增架构文档

## 8.1 新增 `docs/02-architecture/architecture-design-v1.0.md`

基于《Transwarp Service Insight 系统架构设计方案v1.0.docx》整理。

该文档用于承接上传的系统架构方案，要求包含：

1. 文档目的。
2. 适用范围。
3. 当前仓库状态说明。
4. 业务目标。
5. 核心设计理念。
6. 架构目标与设计原则。
7. 系统总体架构。
8. 逻辑分层架构。
9. 关键集成关系。
10. 查询改写与意图识别。
11. 混合检索服务。
12. 大模型生成与幻觉拦截。
13. 知识库建设与管理。
14. 大模型推理服务工程化。
15. 反馈闭环与效果度量。
16. ITSM / AIOps 集成方案。
17. 降级与容灾策略。
18. 提示注入防御与 AI 安全。
19. 技术选型。
20. 部署与资源规划。
21. 分阶段演进路线。
22. 当前仓库与目标架构之间的差距说明。
23. 明确说明：本文是目标架构设计，不代表当前 Demo 已经实现这些能力。

文档中必须体现以下核心思想：

- RAG 为主导；
- 无侵入集成；
- 私有化部署优先；
- 全链路安全可信；
- 数据闭环自演进；
- AI 能力不可用时不能阻塞工单主流程；
- 检索接口必须只检索已审核知识；
- 预诊结果必须可追溯、可解释、可降级。

---

## 8.2 新增 `docs/02-architecture/system-context.md`

内容包括：

- 系统与用户的关系；
- 系统与 ITSM 的关系；
- 系统与 AIOps 的关系；
- 系统与知识库的关系；
- 系统与大模型服务的关系；
- 系统与埋点 / 指标系统的关系。

必须明确：

- 当前 Demo 只模拟这些关系；
- 真实对接需要后续评审和接口确认。

---

## 8.3 新增 `docs/02-architecture/module-boundary.md`

内容包括：

- 前端预诊面板；
- 预诊业务服务；
- 查询改写服务；
- 混合检索服务；
- LLM 生成服务；
- 幻觉拦截服务；
- 知识管理服务；
- 反馈埋点服务；
- 审计与安全服务；
- 监控与指标服务。

每个模块都要写清楚：

- 职责；
- 输入；
- 输出；
- 不负责的事情；
- 当前状态；
- 后续阶段。

---

## 8.4 新增 `docs/02-architecture/deployment-plan.md`

内容包括：

- 一期 MVP 资源规划；
- K8s 部署目标；
- GPU 节点规划；
- ES / Redis / Milvus / PostgreSQL / Kafka / ClickHouse 的目标角色；
- 说明当前阶段不部署这些生产组件。

---

## 8.5 新增 `docs/02-architecture/evolution-plan.md`

内容包括：

- 一阶段：内部预诊助手；
- 二阶段：扩展全部产品线；
- 三阶段：客户 / 伙伴智能工单助手；
- 四阶段：主动预警与智能 SLA 生命周期管理。

必须明确：

- 当前只处于 Demo / 架构设计阶段；
- 后续阶段需要评审。

---

# 九、新增 API 草案文档

## 9.1 新增 `docs/03-api/api-contract-draft.md`

只写接口草案，不写真实接口地址，不写真实鉴权实现，不写生产连接信息。

至少包含以下接口草案：

### 1. `POST /api/v1/precheck/analyze`

用于 SLA 提交前预诊。

输入字段建议：

- title
- description
- product
- module
- version
- severity
- impact_scope
- attachment_summary
- user_context
- source_channel

输出字段建议：

- precheck_id
- diagnosis_summary
- possible_causes
- recommended_actions
- references
- confidence
- missing_information
- risk_level
- manual_review_required
- latency_ms

必须说明：

- 返回结果是辅助建议；
- 不代表最终根因；
- 不代表正式处理结论；
- 不能自动闭单。

### 2. `POST /api/v1/retrieval/search`

用于知识检索。

输入字段建议：

- query
- rewrite
- top_k
- filters
- search_type
- rerank
- rerank_top_n
- include_highlights

输出字段建议：

- chunk_id
- text
- score
- metadata
- highlights
- rewritten_query
- total_hits
- latency_ms

必须说明：

- 默认只检索 `status = APPROVED` 的知识；
- 当前文档只是接口草案。

### 3. `POST /api/v1/feedback/event`

用于记录用户采纳、忽略、继续提交等行为。

输入字段建议：

- event_name
- user
- ticket_id
- precheck_id
- adoption_status
- referenced_knowledge_ids
- latency_ms
- rating
- comment

输出字段建议：

- event_id
- status
- received_at

### 4. `POST /api/v1/knowledge/review`

用于知识审核状态流转。

输入字段建议：

- chunk_id
- action
- reason
- operator
- review_comment

输出字段建议：

- chunk_id
- previous_status
- current_status
- operated_at

文档中必须说明：这些接口为后续设计草案，不代表当前 Demo 已经实现。

---

## 9.2 新增 `docs/03-api/openapi-placeholder.md`

说明：

- 后续可以基于 `api-contract-draft.md` 生成 OpenAPI 规范；
- 当前不维护真实 OpenAPI；
- 当前不提供真实服务地址；
- 当前不提供真实鉴权方式。

---

# 十、新增 RAG 设计文档

## 10.1 新增 `docs/04-rag/rag-design.md`

内容包括：

1. 查询改写。
2. 意图识别。
3. 实体抽取。
4. 文档解析。
5. 文档切片。
6. 元数据标注。
7. Embedding。
8. ES 关键词检索。
9. 向量检索。
10. 多路召回。
11. 去重合并。
12. 重排序。
13. L1 / L2 缓存。
14. 缓存失效。
15. 检索质量评估。
16. Top5 命中率指标。
17. 当前阶段不接真实数据的边界说明。

必须说明：

- 当前 Demo 不具备真实 RAG 能力；
- RAG 是后续目标架构；
- 真实数据必须授权、脱敏、审核；
- 仅 `APPROVED` 状态知识可进入线上检索。

---

## 10.2 新增 `docs/04-rag/retrieval-design.md`

内容包括：

- BM25 检索；
- 向量检索；
- 混合检索；
- 分数归一化；
- 去重合并；
- Cross-Encoder 重排序；
- 检索过滤条件；
- 检索结果来源展示；
- 检索超时降级。

---

## 10.3 新增 `docs/04-rag/query-rewrite-design.md`

内容包括：

- 术语纠错；
- 产品名规范化；
- 错误码识别；
- 同义词扩展；
- IP / 日期 / 版本实体识别；
- 查询意图分类；
- 风险：过度改写可能导致误召回；
- 要求保留原始输入用于审计。

---

## 10.4 新增 `docs/04-rag/evaluation-design.md`

内容包括：

- Top1 / Top3 / Top5 命中率；
- MRR；
- NDCG；
- 采纳率；
- 人工标注评估集；
- 高频未命中 query 分析；
- 低分反馈触发知识复审。

---

# 十一、新增 LLM 设计文档

## 11.1 新增 `docs/05-llm/prompt-design.md`

内容包括：

- Prompt 总体原则；
- 系统指令；
- 用户输入隔离；
- 参考资料引用格式；
- 不确定时如何回答；
- JSON 输出格式；
- 故障排查类模板；
- 操作指导类模板；
- 咨询类模板；
- 禁止事项。

必须强调：

- 只基于参考资料回答；
- 不得编造来源；
- 不得输出最终根因；
- 不得输出正式处理结论；
- 信息不足时要提示补充信息。

---

## 11.2 新增 `docs/05-llm/generation-design.md`

内容包括：

- 生成管理器；
- 统一推理接口；
- 模型路由；
- 简单问题模型；
- 复杂问题模型；
- 结构化输出；
- JSON 解析失败降级；
- 推理超时降级。

---

## 11.3 新增 `docs/05-llm/hallucination-guardrails.md`

内容包括：

- 生成前拦截；
- 检索分数阈值；
- 知识完整性检查；
- 生成中约束；
- 引用块限制；
- 生成后引用对齐检测；
- 实体一致性检查；
- 高风险结果降级；
- 人工抽检。

---

## 11.4 新增 `docs/05-llm/model-routing-design.md`

内容包括：

- 轻量模型路由；
- 复杂诊断模型路由；
- 多模型版本标记；
- A/B 测试；
- 回滚策略；
- 不同模型输出的一致性评估。

---

# 十二、新增知识库文档

## 12.1 新增 `docs/06-knowledge/knowledge-lifecycle.md`

内容包括：

- 多源接入；
- 文档解析；
- 切片；
- 元数据标注；
- 向量化；
- 审核；
- 发布；
- 过期；
- 废弃；
- 审计。

状态机：

```text
DRAFT → REVIEW → APPROVED / REJECTED → EXPIRED / DEPRECATED
```

必须说明：

- 只有 `APPROVED` 知识可被线上检索；
- `DEPRECATED` 只用于审计和历史追踪；
- 所有状态变更需要记录操作人、时间、原因。

---

## 12.2 新增 `docs/06-knowledge/knowledge-review-flow.md`

内容包括：

- 审核队列；
- SME 审核；
- 批量通过；
- 驳回原因；
- 版本对比；
- 负反馈触发复审；
- 长期未审核提醒；
- 审核效率指标。

---

## 12.3 新增 `docs/06-knowledge/metadata-schema.md`

内容包括知识 Chunk 的元数据草案：

- chunk_id
- doc_id
- title
- product
- module
- doc_type
- source
- version
- status
- created_at
- updated_at
- expired_at
- owner
- review_status
- sensitivity_level
- permission_scope

必须说明：

- 当前只是设计草案；
- 真实 Schema 需后端和数据团队评审。

---

# 十三、新增集成文档

## 13.1 新增 `docs/07-integration/itsm-integration.md`

内容包括：

- 工单创建页前端集成；
- 预诊结果展示；
- 工单模型扩展字段；
- 采纳状态；
- 预诊结果保存；
- 用户人工确认；
- AI 不可用时不影响提交。

字段建议：

- pre_diagnosis_id
- adoption_status
- referenced_knowledge_ids
- precheck_summary
- ai_assist_trace_id

必须说明：

- 当前 Demo 不接真实 ITSM；
- 后续需 ITSM 团队评审接口。

---

## 13.2 新增 `docs/07-integration/aiops-integration.md`

内容包括：

- AIOps 复盘库作为知识来源；
- 告警、指标、日志摘要作为预诊上下文；
- 不直接执行修复动作；
- 不直接关闭告警；
- 不自动改生产配置；
- 后续接入需要安全评审。

---

## 13.3 新增 `docs/07-integration/frontend-embed-design.md`

内容包括：

- iframe 方式；
- JS SDK 方式；
- 工单创建页嵌入；
- 防抖触发；
- 手动触发；
- 结果面板；
- 降级隐藏；
- 当前 Demo 的差异。

---

# 十四、新增安全与幻觉防护文档

## 14.1 新增 `docs/08-security/security-and-guardrails.md`

内容包括：

1. 数据不出域。
2. 权限过滤。
3. 脱敏。
4. 传输与存储加密。
5. Prompt 注入防御。
6. 指令与用户输入隔离。
7. 生成前拦截。
8. 生成中约束。
9. 生成后引用对齐检测。
10. 实体一致性检查。
11. 敏感信息过滤。
12. 高风险结果降级。
13. 审计日志。
14. 人工审核要求。
15. 红队测试计划。

必须强调：

- 模型输出只作为辅助建议。
- 不得作为最终根因。
- 不得绕过人工审核。
- 不得输出未脱敏敏感信息。
- 不得根据用户注入指令改变系统规则。

---

## 14.2 新增 `docs/08-security/data-masking.md`

内容包括：

- IP 脱敏；
- 手机号脱敏；
- 邮箱脱敏；
- 密钥脱敏；
- Token 脱敏；
- 密码脱敏；
- 日志路径和客户名称处理；
- 脱敏前后示例，示例必须标注“模拟数据”。

---

## 14.3 新增 `docs/08-security/prompt-injection-defense.md`

内容包括：

- 常见注入模式；
- “忽略以上指令”类攻击；
- 角色扮演类攻击；
- 泄露系统提示词类攻击；
- 指令和数据隔离；
- 输入过滤；
- 输出检测；
- 红队样例。

---

## 14.4 新增 `docs/08-security/audit-design.md`

内容包括：

- 用户请求审计；
- 检索审计；
- 生成审计；
- 降级审计；
- 采纳行为审计；
- 知识状态变更审计；
- 审计字段草案；
- 审计日志保存周期待安全团队评审。

---

# 十五、新增指标与反馈闭环文档

## 15.1 新增 `docs/09-observability/observability-and-metrics.md`

内容包括：

1. 埋点事件。
2. 预诊触发事件。
3. 预诊结果事件。
4. 用户采纳事件。
5. 工单结果事件。
6. 用户评分事件。
7. 降量指标。
8. 效率指标。
9. 质量指标。
10. 幻觉拦截率。
11. 检索命中率。
12. 采纳率。
13. 看板设计。
14. 低分知识复审流程。
15. 高频未命中 query 的知识缺口发现流程。

---

## 15.2 新增 `docs/09-observability/event-tracking.md`

事件建议包括：

```text
pre_check_trigger
pre_check_result
pre_check_adopt
ticket_outcome
feedback_rating
knowledge_review
guardrail_block
fallback_triggered
```

每个事件写清楚：

- 事件说明；
- 触发时机；
- 字段；
- 是否包含敏感信息；
- 是否需要脱敏；
- 当前阶段是否实现。

---

## 15.3 新增 `docs/09-observability/dashboard-design.md`

内容包括：

- 预诊触发量；
- 采纳率；
- 忽略率；
- 继续提交率；
- 检索命中率；
- 幻觉拦截率；
- 降级次数；
- 平均延迟；
- 知识复审数量；
- 高频未命中 query；
- SME 审核效率。

必须说明：

- 当前阶段只是指标设计；
- 真实看板后续基于 ClickHouse / Grafana 或公司现有平台实现。

---

# 十六、新增测试文档

## 16.1 新增 `docs/10-testing/test-plan.md`

内容包括：

- 当前 Demo 测试；
- 文档完整性测试；
- 链接检查；
- mock 数据一致性测试；
- 后续接口测试；
- 后续 RAG 评估；
- 后续 LLM 安全测试；
- 后续集成测试；
- 后续降级测试。

---

## 16.2 新增 `docs/10-testing/rag-evaluation-plan.md`

内容包括：

- 评估集构建；
- 人工标注；
- TopK 命中；
- MRR；
- NDCG；
- 召回错误分析；
- 知识缺口分析；
- 检索结果可解释性检查。

---

## 16.3 新增 `docs/10-testing/security-test-plan.md`

内容包括：

- Prompt 注入测试；
- 敏感信息泄露测试；
- 权限绕过测试；
- 引用编造测试；
- 高风险内容输出测试；
- 日志脱敏测试；
- 红队样例库。

---

## 16.4 新增 `docs/10-testing/acceptance-test-plan.md`

内容包括：

- Demo 阶段验收；
- 文档阶段验收；
- 接口草案验收；
- RAG 原型验收；
- LLM 生成验收；
- 安全集成验收；
- ITSM 集成验收。

---

# 十七、更新已有文档

## 17.1 更新 `README.md`

补充：

- 当前项目定位；
- 当前仍是 Demo / PRD / 架构设计阶段；
- 已新增架构设计文档；
- 已新增实施计划；
- 已新增任务拆分；
- 已新增接口草案；
- 已新增 RAG、LLM、安全、指标、测试文档；
- 明确生产化能力是后续目标，不代表当前已经实现；
- 新增文档导航。

建议文档导航：

```markdown
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
```

---

## 17.2 更新 `docs/00-project/roadmap.md`

补充：

- 架构设计整合阶段；
- 可试点系统设计阶段；
- RAG 原型阶段；
- LLM 预诊阶段；
- ITSM / AIOps 集成阶段；
- 反馈闭环阶段。

如果原 `roadmap.md` 已移动，请修改移动后的文件。

---

## 17.3 更新 `docs/00-project/progress.md`

更新：

- 当前进度可以从 35% 调整为 40% 或合适比例；
- 已完成新增“架构设计方案整合与实施拆分”；
- 待办中增加：
  - 接口草案评审；
  - 知识源权限评审；
  - 安全评审；
  - RAG 原型评审；
  - LLM Prompt 评审；
  - 指标看板评审；
  - ITSM / AIOps 集成评审。

如果原 `progress.md` 已移动，请修改移动后的文件。

---

## 17.4 更新 `AGENTS.md`

补充：

- 当前阶段允许进行架构文档整合和任务拆分；
- 当前阶段允许调整 docs 文档结构；
- 当前阶段允许新增接口草案、RAG 设计、安全设计、指标设计、测试设计；
- 仍然禁止声称已接入真实系统；
- 仍然禁止使用真实客户数据；
- 仍然禁止自动提交 SLA；
- 仍然禁止自动闭单；
- 后续如果要进入工程开发，必须先调整阶段定义并经过人工确认。

---

## 17.5 更新 `CHANGELOG.md`

按现有格式追加今天日期的变更记录，记录：

- 新增分层文档目录结构；
- 移动原有文档到新的分层目录；
- 新增架构设计 Markdown；
- 新增实施计划；
- 新增任务拆分；
- 新增接口草案；
- 新增 RAG 设计；
- 新增 LLM 设计；
- 新增知识库生命周期设计；
- 新增 ITSM / AIOps 集成设计；
- 新增安全与幻觉防护设计；
- 新增指标与反馈闭环设计；
- 新增测试验收文档；
- 清理误打包目录；
- 更新 README、roadmap、progress、AGENTS；
- 保留 Demo / 模拟数据 / 人工审核边界。

---

# 十八、归档本次提示词

新增提示词文件：

```text
prompts/2026-07-06-01-architecture-integration-directory-and-task-breakdown.md
```

内容包括：

1. 使用时间：2026/07/06。
2. 使用目的：将系统架构设计方案整合到项目中，并形成目录结构、实施计划、任务拆分、接口草案和后续开发依据。
3. 适用范围：项目文档整合、架构评审、任务分工、后续阶段规划。
4. 提示词正文：保存本次完整提示词。
5. 约束说明：
   - 不接真实系统；
   - 不生成后端代码；
   - 不使用真实数据；
   - 不自动提交 SLA；
   - 不自动闭单；
   - 不输出最终根因；
   - 不声称当前 Demo 具备生产能力。

注意：归档时不要无限递归复制自身。如果自动化处理提示词归档不方便，可以将本提示词原文保存到该文件即可。

---

# 十九、质量检查要求

完成后请检查：

1. 所有新增文档标题、目的、适用范围、正文完整。
2. 所有示例都明确标注“模拟数据”。
3. 所有后续能力都明确是“目标架构”或“后续阶段”。
4. 没有真实接口地址。
5. 没有真实客户、真实 SLA、真实日志、真实内部链接。
6. 没有声称当前 Demo 已经接入真实 RAG、大模型、AIOps 或 ITSM。
7. `CHANGELOG.md` 已更新。
8. `README.md`、`docs/00-project/roadmap.md`、`docs/00-project/progress.md` 已同步。
9. 原 docs 路径移动后，README 中链接无失效。
10. 删除误打包目录。
11. 保持项目中文档风格一致，中文表达清晰，可直接用于评审。
12. 不创建真实生产服务目录。
13. 不提交空洞无内容文档。
14. 不把目标架构描述为已完成能力。
15. 不把模拟数据描述为真实数据。

---

# 二十、最终输出要求

完成任务后，请在回复中总结：

1. 创建了哪些目录。
2. 移动了哪些文档。
3. 新增了哪些文档。
4. 更新了哪些文档。
5. 删除了哪些误打包文件或目录。
6. 是否更新了 CHANGELOG。
7. 当前项目边界是否仍然保持为 Demo / PRD / 架构设计阶段。
8. 是否存在未完成项或需要人工确认的事项。

请不要只回复“完成”。请给出清晰的变更摘要。
