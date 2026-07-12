# 当前架构

Status: ACTIVE  
Owner: 技术负责人  
Last reviewed: 2026-07-12  
Source of truth for: 当前技术 MVP 拓扑、调用链和实现边界

## 当前拓扑

```text
浏览器 → Nginx/Vue → /api 反向代理 → Spring Boot
                                      ↓
Controller → Use Case → Application Service → PrecheckExecutionPort → 确定性 Mock Adapter
```

Compose 仅向宿主机暴露前端 `5173`，后端在 Compose 网络内监听 `8080`。非容器开发仍支持 Vite 与 Spring Boot 分别启动。

## 已实现

- SLA 表单、一次预诊和页面内多轮追问；
- Bean Validation、结构化错误、进程健康接口；
- 摘要、建议、模拟依据、置信度、人工审核、待补充信息和下一步动作；
- `CONTINUE_SUBMISSION` 始终允许，提交内容与是否提交由人工确认；
- 无状态关键词 Mock；刷新清空会话；失败不清空历史且不阻断人工提交；
- OpenAPI、前后端测试、容器构建和 Compose。
- 模拟反馈接口、策略版本快照及进程内反馈/脱敏审计事件；后端重启即清空。

## 当前边界

未实现数据库、真实 RAG、真实模型、真实 SSO/权限、外部审计存储、反馈持久化、AIOps/ITSM、知识治理、生产部署或自动操作。后端不校验或持久化追问会话。所有智能结果和引用均标注 `模拟数据`，不得作为最终根因、最终方案或正式复盘结论。

在线请求仅执行同步、只读、可降级 Mock。离线知识导入链路尚未实现，也不得混入在线请求。
