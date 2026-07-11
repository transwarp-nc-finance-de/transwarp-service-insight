# 本地开发指南

## 目的

说明 M1 的安装、启动、测试、构建和本地联调方式。

## 适用范围

适用于 `backend/` Spring Boot Mock 服务和 `frontend/` Vue 应用。

## 正文

依赖 JDK 21（设置 `JAVA_HOME`）、Node.js 20+ 和 npm 10+。Maven 无需全局安装。当前已知本机有 Node.js 24、npm 11 和 Docker，但无 Java/Maven；M1 不使用容器。

后端：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
.\mvnw.cmd package
```

macOS/Linux 使用 `./mvnw`。首次运行会下载 Maven 3.9.9。服务地址为 `http://localhost:8080`。

前端：

```powershell
cd frontend
npm ci
npm run dev
npm test
npm run build
```

访问 `http://localhost:5173`，Vite 将 `/api` 代理到后端。正常预诊、缺少可选信息、关闭后端三种情况下，“继续提交 SLA”都应可点击；该操作仅显示人工确认提示，不调用提交接口。

常见问题：`java` 未找到时安装 JDK 21 并重开终端；Wrapper 下载失败时检查网络；前端 502 时确认后端 8080 已启动。不要提交个人代理或凭据。

所有输出均为 `模拟数据`，未接真实 RAG、模型、AIOps、知识库、ITSM 或生产数据。
