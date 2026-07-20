# 本地开发指南

## 目的

说明 M2 的容器化交付以及保留的非容器开发方式。

## 适用范围

适用于 `backend/` Spring Boot Mock 服务、`frontend/` Vue 应用、本地 Compose PostgreSQL/pgvector 与固定离线 `local-embedding`。

## 正文

完整的首次安装、版本检查、日常运维、故障排查、升级与回滚步骤以根目录 [README](../../README.md) 为准。本文只保留源码开发时的补充说明，避免与安装手册形成两套口径。

### 一键容器化启动

依赖已启动的 Docker Desktop。根目录执行：

```powershell
$env:LOCAL_EMBEDDING_MODEL_PATH='D:\approved-artifacts\multilingual-e5-base'
docker compose up -d --build --wait
Invoke-RestMethod http://localhost:5173/api/v1/health
docker compose down --remove-orphans
```

`LOCAL_EMBEDDING_MODEL_PATH` 必须指向 Issue #19 批准的五文件只读制品目录；启动时会校验白名单、逐文件字节/SHA-256 和 manifest SHA-256，不匹配即失败。模型不进入 Git 或镜像，服务仅连接 `index-internal` 内部网络。显式 FTS-only 模式使用 `docker compose -f compose.yaml -f compose.fts-only.yaml up -d --wait`，此模式不启动 embedding 服务。

浏览器访问 `http://127.0.0.1:5173`。前端入口绑定宿主机 `127.0.0.1:5173`，本地 PostgreSQL 仅绑定 `127.0.0.1:5432`，后端只在 Compose 网络内提供 8080，local-embedding 不发布宿主端口；数据库保存版本化模拟目录与身份、AuthSession、知识治理与双索引、预诊 Session/Run、Evidence、Feedback、SubmissionContinuation 和结构化 AuditEvent，`knowledge-files` volume 只保存本地模拟原始知识文件，不得写入真实业务数据。

### 非容器开发

依赖 JDK 21（设置 `JAVA_HOME`）、Node.js 24 LTS 和 npm。Maven 无需全局安装。

后端：

```powershell
docker compose up -d --wait postgres
cd backend
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
.\mvnw.cmd spotless:check
.\mvnw.cmd package
```

macOS/Linux 使用 `./mvnw`。首次运行会下载 Maven 3.9.9。服务地址为 `http://localhost:8080`。

前端：

```powershell
cd frontend
npm ci
npm run dev
npm run lint
npm run format:check
npm run openapi:check
npm test
npm run build
```

访问 `http://localhost:5173`，Vite 将 `/api` 代理到后端。页面顶部可以使用四个预置身份完成本地模拟登录、切换和退出。正常预诊、缺少可选信息、关闭后端三种情况下，“继续提交 SLA”都应可点击；该操作仅显示人工确认提示，不调用提交接口。

容器模式下关闭后端后，前端页面仍可访问，预诊会以网关错误安全失败且“继续提交 SLA”仍可人工操作（仅模拟，不调用提交接口）。常见故障及安全清理方式统一见根目录 README。不要提交个人代理、凭据或包含真实客户信息的日志。

所有输出均为 `模拟数据`，未接真实 RAG、模型、AIOps、知识库、ITSM、企业共享数据库或生产数据。
