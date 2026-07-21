# Transwarp Service Insight

`Transwarp Service Insight` 当前聚焦外挂在 AIOps SLA 流程中的智能预诊能力。AIOps 是正式表单、枚举、校验和最终提交的宿主；本仓库提供独立预诊后端、嵌入式面板以及本地 Mock AIOps Sandbox。

能力状态：Engineering Baseline `DONE`；Architecture Skeleton `IN PROGRESS`；本地身份与 PostgreSQL 基础闭环 `IMPLEMENTED`；Knowledge Ingestion、审核、双索引原子发布与废弃 `IMPLEMENTED`；持久化预诊 Session/Run、三轮补充、恢复、显式自助结束、授权在线 Retrieval 与 Evidence、独立 Feedback、SubmissionContinuation、结构化 AuditEvent、EvaluationRun、Metrics 与最小管理员评估页 `IMPLEMENTED`；AIOps Host Integration `PROTOTYPE`；LLM Generation 与 Agent Orchestration 仍为 `NOT STARTED`。

> 当前全部业务内容均为 `模拟数据`。系统使用本地 Compose PostgreSQL 保存模拟身份与目录、AuthSession、知识治理、预诊 Session/Run、Evidence、Feedback、SubmissionContinuation、结构化 AuditEvent、EvaluationRun 与聚合指标所需事件等本地工程数据，不接入真实客户数据、ITSM、AIOps、生成式 RAG/LLM、企业共享/生产数据库或生产环境；预诊建议仅供人工参考，不是最终根因、最终方案或正式复盘结论。失败、低置信度或信息不完整不得阻断人工继续提交，SLA 是否提交及提交内容始终由人工确认。

## 1. 适用范围

本文面向首次接触仓库的开发和评审人员，用于完成本地工程验收。它不是生产部署方案，不包含真实 SSO、HTTPS、域名、业务数据持久化、高可用、监控告警、真实外部服务或生产配置。

当前事实以以下文档为准：

- [当前范围](docs/project/scope.md)
- [已实现 v1 OpenAPI 唯一契约](docs/api/openapi.yaml)
- [v2 DRAFT 实施契约与 operation 级实现状态](docs/api/openapi-v2.yaml)
- [文档导航](docs/README.md)
- [本地开发](docs/development/local-development.md)
- [测试策略](docs/development/test-strategy.md)

## 2. 系统拓扑

```text
浏览器 / 本机命令
       |
       | http://127.0.0.1:5173
       v
+---------------------------+
| frontend 容器             |
| Nginx :80 + Vue 静态文件  |
+---------------------------+
       |
       | /api/* 反向代理（Compose 内部网络 app）
       v
+---------------------------+
| backend 容器              |
| Spring Boot :8080         |
+---------------------------+
       |
       v
+---------------------------+
| postgres/pgvector 容器    |
| 模拟身份/会话/双索引       |
+---------------------------+
       ^
       | index-internal（无互联网出口）
+---------------------------+
| local-embedding 容器      |
| 固定 E5 / CPU / 768 维    |
+---------------------------+
```

业务入口仅绑定 `127.0.0.1:5173`，后端 8080 与 local-embedding 8090 不向宿主机开放；PostgreSQL 的 5432 仅供本地开发。页面和 API 都通过 `http://127.0.0.1:5173` 访问。`GET /api/v1/health` 返回 `UP` 只表示后端进程可以响应，不代表在线 Retrieval 或任何外部系统可用。

## 3. 前置条件

### 3.1 一键容器模式（推荐）

安装并启动 Docker Desktop，确保包含 Docker Compose v2。Windows 建议使用 PowerShell；macOS/Linux 可使用任意常用终端。

```powershell
docker --version
docker compose version
docker info
git --version
```

所有命令都应成功。`docker info` 若提示无法连接 daemon，先启动 Docker Desktop 并等待引擎就绪。

容器模式不要求宿主机安装 JDK、Node.js、npm 或 Maven：镜像构建分别使用 JDK 21/Maven 3.9.9 和 Node.js 24。

### 3.2 非容器开发模式

仅在需要修改和调试源码时额外安装：

- JDK 21，并正确设置 `JAVA_HOME`
- Node.js 24 和随附的 npm
- Git

```powershell
java -version
node --version
npm --version
git --version
```

`java -version` 应显示 21，`node --version` 应显示 24.x。后端使用仓库内 Maven Wrapper，无需全局安装 Maven。

## 4. 获取仓库

首次获取：

```powershell
git clone <仓库地址>
cd transwarp-service-insight
```

已有工作副本：

```powershell
git status
git pull --ff-only
```

若工作区有未提交修改，请先人工确认和妥善保存，不要直接覆盖。后续命令均在仓库根目录执行，除非章节另有说明。

## 5. Quick Start

### 5.1 构建并启动

Windows PowerShell、macOS 和 Linux 命令相同：

```powershell
docker compose up -d --build --wait
```

首次启动需要下载基础镜像和依赖，耗时取决于网络。`--wait` 会等待后端健康检查通过；命令成功返回后，打开：

<http://127.0.0.1:5173>

### 5.2 首次启动验证

Windows PowerShell：

```powershell
docker compose ps
Invoke-RestMethod http://127.0.0.1:5173/api/v1/health
```

macOS/Linux：

```bash
docker compose ps
curl --fail --silent http://127.0.0.1:5173/api/v1/health
```

预期结果：`frontend`、`backend` 与 `postgres` 均处于运行状态，后端与 PostgreSQL 为 `healthy`，健康接口返回包含 `"status": "UP"` 的 JSON。

页面顶部提供四个预置本地身份。选择身份后登录，确认页面显示 `模拟数据`、唯一角色及授权产品线；切换身份会轮换 Session 与 CSRF Token，退出会同时使二者失效。该能力不是 SSO 或生产鉴权。

使用 `mock-knowledge-editor` 登录后可访问 `/knowledge`，上传标注为模拟数据的 Markdown、TXT 或文本型 PDF，查看 ParseTask、解析摘要、Block 和 Chunk；审核人可批准后创建双索引发布任务、查看 FTS/向量分支状态并废弃已发布版本。扫描 PDF/OCR、真实知识源和在线检索仍不在该页面范围内。

使用 `mock-precheck-tdh` 登录后可访问 `/precheck-v2`，创建持久化 Session 与 Run 1、补充完整 Context 形成最多三轮 Run、刷新恢复本人活动 Session、跳过建议继续人工提交，以及显式确认自助结束。每个 Run 都会在当前 Context 产品线授权范围内查询当前 `PUBLISHED` 知识：FTS 与本地 Embedding 均可用时为 `HYBRID`，Embedding 故障时降级为 `FTS_ONLY`，FTS 故障时为无 Evidence 的 `UNAVAILABLE`；降级均限制为 `LOW` 且不阻断人工继续提交。Evidence 保存不可变快照，并在每次读取时按当前身份重新鉴权。

### 5.3 `模拟数据` 预诊验收

1. 打开 <http://127.0.0.1:5173/sandbox>。该页面不是正式 SLA 入口；`/embed` 仅用于验证嵌入式预诊面板。
2. 在必填的“问题标题”和“问题描述”中填写不含真实客户信息的模拟内容，例如“`模拟数据：查询响应变慢`”。
3. 点击“智能预诊”，确认页面展示摘要、建议、依据来源、置信度、人工介入建议和待补充信息。
4. 确认输出明确为 `模拟数据`，且没有将建议表述为最终根因或最终处理结论。
5. 确认“继续提交 SLA”始终可操作；它只展示人工确认提示，不调用真实提交接口。

## 6. 日常运维命令

以下命令均在仓库根目录执行。

查看服务状态：

```powershell
docker compose ps
```

查看全部日志或单个服务日志：

```powershell
docker compose logs --tail 200
docker compose logs --tail 200 backend
docker compose logs --tail 200 frontend
docker compose logs --tail 200 postgres
docker compose logs -f
```

`docker compose logs -f` 使用 `Ctrl+C` 退出只会停止跟踪，不会停止服务。

重启服务（不重新构建镜像）：

```powershell
docker compose restart
docker compose restart backend
```

源码或依赖变化后重新构建并启动：

```powershell
docker compose up -d --build --wait
```

停止并删除本项目容器和网络：

```powershell
docker compose down --remove-orphans
```

仅停止、保留容器：

```powershell
docker compose stop
```

恢复已停止的容器：

```powershell
docker compose start
```

清理本项目容器、网络和本地构建镜像（会导致下次完整重建，执行前人工确认）：

```powershell
docker compose down --remove-orphans --rmi local
```

`docker compose down` 默认保留 `postgres-data` 卷，以验证迁移和会话重启持久化；只有人工明确需要清空本地模拟数据时才可增加 `--volumes`。仓库和数据卷都不得存放真实业务数据。

## 7. 常见故障排查

| 现象 | 检查 | 处理 |
| --- | --- | --- |
| Docker 未启动或无法连接 daemon | `docker info` | 启动/重启 Docker Desktop，等待引擎就绪后重试 |
| 5173 端口已被占用 | Windows：`Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue`；macOS/Linux：`lsof -i :5173` | 关闭占用程序或先停止本仓库的旧 Compose；不要擅自改已约定入口后提交 |
| 镜像或依赖下载失败 | 查看构建输出，确认代理、DNS 和镜像仓库访问 | 网络恢复后重试 `docker compose up -d --build --wait`；不要提交个人代理、镜像凭据或证书 |
| `backend` 不健康 | `docker compose ps`、`docker compose logs --tail 200 backend` | 根据启动异常修复；必要时 `docker compose up -d --build --wait` 完整重建 |
| 页面 API 返回 502/504 | `docker compose ps` 并检查 backend 日志 | 502 通常表示后端不可连接，504 通常表示后端响应超时；恢复后端后重试，人工提交仍不得被阻断 |
| Java 版本错误或找不到 Java | `java -version`、`$env:JAVA_HOME`（Windows） | 安装 JDK 21，修正 `JAVA_HOME`，重开终端；容器模式无需宿主机 Java |
| Node/npm 版本错误 | `node --version`、`npm --version` | 安装 Node.js 24，重开终端并重新执行 `npm ci`；容器模式无需宿主机 Node.js |
| 页面无法打开 | `docker compose ps`、`docker compose logs --tail 200 frontend` | 确认使用 `http://127.0.0.1:5173`，前端容器正在运行且端口未被占用 |

若问题仍未解决，请保留已脱敏的命令输出、服务状态和相关日志，再交由人工排查。日志中不得包含真实客户数据、访问令牌或凭据。

## 8. 非容器开发

本模式用于本地热更新，前后端分别占用宿主机 8080 和 5173。

### 8.1 启动后端

Windows PowerShell：

```powershell
docker compose up -d --wait postgres
cd backend
.\mvnw.cmd spring-boot:run
```

macOS/Linux：

```bash
cd backend
./mvnw spring-boot:run
```

后端地址为 <http://127.0.0.1:8080>。首次运行 Wrapper 会下载 Maven 3.9.9 和项目依赖。

### 8.2 启动前端

另开终端：

```powershell
cd frontend
npm ci
npm run dev
```

访问 <http://127.0.0.1:5173>。Vite 将 `/api` 代理到 `http://localhost:8080`。退出开发服务使用 `Ctrl+C`。

## 9. 测试与构建

后端（Windows PowerShell）：

```powershell
cd backend
.\mvnw.cmd spotless:check test
.\mvnw.cmd package
```

macOS/Linux 将 `.\mvnw.cmd` 替换为 `./mvnw`。

前端和 OpenAPI 契约校验（`openapi:check` 同时校验已实现的 v1 和整体仍为 DRAFT / PARTIALLY_IMPLEMENTED 的 v2；当前 v2 已实现 AuthSession、知识上传/解析预览/审核/索引发布、持久化预诊、授权 Retrieval 与 Evidence、独立 Feedback、SubmissionContinuation 和结构化 AuditEvent 等 operation 级 `IMPLEMENTED` 切片）：

```powershell
cd frontend
npm ci
npm run lint
npm run format:check
npm run openapi:check
npm test
npm run build
```

Compose 配置与关键链路：

```powershell
docker compose config --quiet
docker compose up -d --build --wait
docker compose ps
Invoke-RestMethod http://127.0.0.1:5173/api/v1/health
docker compose down --remove-orphans
```

macOS/Linux 将 HTTP 验证命令中的 `Invoke-RestMethod` 替换为 `curl --fail --silent`。详细行为与降级验收范围见[测试策略](docs/development/test-strategy.md)。

## 10. 升级与回滚

升级前先确认工作区干净、目标分支或版本已获人工批准：

```powershell
git status
git pull --ff-only
docker compose up -d --build --wait
Invoke-RestMethod http://127.0.0.1:5173/api/v1/health
```

若新版本本地验收失败，应由人工选择已知可用的标签或提交，在干净工作区切换到该版本后重新构建：

```powershell
git switch --detach <已批准的标签或提交>
docker compose down --remove-orphans
docker compose up -d --build --wait
```

回滚会替换当前本地运行版本；不得在含未提交修改的工作区执行，也不得将 detached HEAD 上的修改直接推送。恢复开发时切回原主题分支。本地 PostgreSQL 使用 Flyway 迁移并默认保留 `postgres-data` 卷，但不提供生产数据迁移或回滚能力。

## 11. 仓库结构

- `backend/`：Spring Boot Mock API、Maven Wrapper 和后端镜像
- `frontend/`：Vue 应用、Nginx 代理配置和前端镜像
- `compose.yaml`：前端、后端与本地 PostgreSQL 一键交付拓扑
- `docs/api/openapi.yaml`：已实现 v1 API 的唯一契约
- `docs/api/openapi-v2.yaml`：v2 DRAFT 实施契约与 operation 级实现状态的唯一来源
- `docs/`：ACTIVE 文档、DRAFT 目标设计和 ARCHIVED 历史材料
- `prompts/`：可长期复用提示词的治理说明

`docs/architecture/drafts/` 中内容均为 `DRAFT`，不代表已实现；`docs/archive/` 仅供历史追溯，不作为当前工程依据。
