# 评审记录

## 目的

本文档记录本次范围收敛、目录重构、HTML 原型迁移和旧文档删除的评审说明，便于后续追溯变更原因。

## 适用范围

适用于本次 `第一阶段：纯前端 SLA 智能预诊 Demo` 重构后的文档评审和 AI Agent 协作交接。

## 正文

### 本次评审结论

本次变更将项目从泛化的 SLA/RAG/AIOps/复盘方案收敛为 `第一阶段：纯前端 SLA 智能预诊 Demo`。当前阶段只允许交付文档、静态 HTML 原型、mock 数据和演示说明。

### 删除旧文档的原因

以下旧文档被删除：

- `docs/agent-design.md`
- `docs/architecture.md`
- `docs/project-overview.md`
- `docs/requirements.md`
- `docs/task-breakdown.md`
- `docs/templates.md`

删除原因是这些文档包含较多后续阶段能力描述，例如真实 RAG、AIOps 摘要、多 Agent、SLA 草稿生成和智能复盘，容易被误读为当前 MVP 功能。其仍有价值的边界信息已合并到 `README.md`、`AGENTS.md`、`docs/mvp-scope.md`、`docs/roadmap.md` 和本文档中。

### HTML 原型迁移原因

原文件：

```text
docs/frontend-demo.html
```

迁移为：

```text
prototypes/sla-precheck-demo.html
```

迁移原因是 `docs/` 目录只保留 Markdown 文档，静态原型统一放入 `prototypes/`，便于区分项目文档和可运行 Demo。

### 范围收敛原因

当前阶段只验证 SLA 提交前的 `智能预诊` 交互，包括按钮、loading、模拟检索结果、建议补充信息和继续提交提示。真实知识库检索、RAG 与大模型总结、AIOps 辅助分析、SLA 表单智能补全和智能复盘报告生成均移入后续演进方向。

### 保留的业务边界

- 所有 Demo 数据必须标注为 `模拟数据`。
- 所有建议必须说明依据来源、置信度、人工介入建议和待补充信息。
- 智能预诊结果不代表最终根因或最终处理结论。
- SLA 是否提交、工单内容是否准确、处理建议是否适用、复盘报告是否完整和知识库条目是否可沉淀，均必须人工审核。
- 不得接入真实接口、真实 RAG、真实大模型、真实 AIOps、真实 Wiki、真实历史 SLA 或真实工单系统。

### 待人工确认事项

- Foundation 审计日志归档场景是否符合第一阶段演示重点。
- 推荐参考资料类型是否覆盖评审方关注的资料来源。
- 后续是否需要增加更多模拟场景，但不改变当前主 Demo。
- 如后续接入真实数据，应先确认脱敏、授权、权限和审计机制。
