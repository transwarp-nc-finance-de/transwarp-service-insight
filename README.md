# Transwarp Service Insight

## 目的

说明项目定位、当前技术 MVP、工程入口和运行方式。

## 适用范围

适用于项目介绍、M1 开发与演示、方案评审和后续任务拆解。

## 当前阶段

`Transwarp Service Insight` 面向交付与服务效率提升，当前能力为 `SLA 智能预诊助手`。第一阶段纯前端 Demo 已于 2026/07/10 关闭；当前进入技术 MVP 开发，M1 已实现 Vue 前端和 Spring Boot 确定性 Mock API。

M1 仅使用 `模拟数据`，无数据库、鉴权、真实 RAG、模型、AIOps、知识库、ITSM 或生产数据。预诊是辅助建议，不是最终根因或处理结论；无论成功或失败，SLA 是否继续提交均由人工确认。

## 工程入口

- `backend/`：Java 21 + Spring Boot 3.x Mock API，默认端口 `8080`。
- `frontend/`：Vue 3 + TypeScript + Vite，默认端口 `5173`。
- `docs/CURRENT_SCOPE.md`：当前范围唯一短版入口。
- `docs/DEVELOPMENT.md`：安装、启动、测试和联调。
- `docs/BACKLOG.md`、`docs/DECISIONS.md`：里程碑与技术决策。
- `prototypes/sla-precheck-demo.html`：第一阶段静态 Demo，仅作视觉与交互参考，不调用后端。

## 快速开始

安装 JDK 21、Node.js 20+ 后，分别运行：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

```powershell
cd frontend
npm ci
npm run dev
```

打开 `http://localhost:5173`。完整构建和测试命令见 `docs/DEVELOPMENT.md`。

## 文档导航

现有 `docs/00-project/` 至 `docs/10-testing/` 中的 PRD、架构、API、RAG、LLM、集成、安全与测试材料继续作为历史基线或后续目标设计参考，不代表全部能力已实现。真实试点仍需完成人工门禁和数据授权。
