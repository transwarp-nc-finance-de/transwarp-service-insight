# Transwarp Service Insight

`Transwarp Service Insight` 当前聚焦 **SLA 智能预诊助手**技术 MVP：通过 Vue 页面和 Spring Boot Mock API，在 SLA 人工提交前提供结构化辅助建议。

> 当前全部业务内容均为 `模拟数据`。系统不接入真实客户数据、ITSM、AIOps、RAG、LLM、数据库或生产环境；预诊结果不是最终根因或处理结论，是否提交 SLA 始终由人工确认。

## 快速开始

- 当前范围：[docs/project/scope.md](docs/project/scope.md)
- 文档导航：[docs/README.md](docs/README.md)
- API 契约：[docs/api/openapi.yaml](docs/api/openapi.yaml)
- 本地开发：[docs/development/local-development.md](docs/development/local-development.md)

M2 提供一键本地交付模式（需 Docker Desktop）：

```powershell
docker compose up -d --build --wait
```

访问 `http://localhost:5173`；后端不向宿主机暴露端口。也可继续使用 Java 21 与 Node.js 24 LTS 分别启动前后端，详见本地开发文档。

## 仓库结构

- `backend/`：Spring Boot Mock API
- `frontend/`：Vue 技术 MVP
- `docs/`：当前文档、设计草案和历史归档
- `prompts/`：仅保留可长期复用提示词的治理说明

## 人工审核边界

智能输出必须包含依据来源、置信度、人工介入建议和待补充信息。失败、低置信度或信息不完整均不得阻断人工继续提交。
