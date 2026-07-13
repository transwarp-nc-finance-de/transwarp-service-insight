# 当前范围：AIOps 嵌入式预诊技术 MVP

状态：ACTIVE，2026-07-12。

当前已有 Vue 前端、Spring Boot API、确定性 Mock Workflow、Docker Compose、OpenAPI 和 CI。AIOps 是 SLA 表单、枚举、原有校验和最终提交的权威宿主；Service Insight 只负责完整度分析、辅助建议、引用、反馈、审计与安全降级。

前端 `/sandbox` 是 Mock AIOps，仅用于本地开发、联调、演示和自动化测试；`/embed` 是不复制 SLA 表单的嵌入式预诊面板。用户始终可忽略建议并由 AIOps 继续原有提交，反馈失败不影响提交。

后端当前使用应用层工作流、细粒度 Port/Mock Adapter 和进程内 Session Repository。初次预诊创建 Run 1，追问在同一 Session 中递增 Run。所有输入只允许模拟、公开或脱敏信息，不保存附件内容或真实敏感正文。

当前未接真实 AIOps、身份传递、数据库、知识导入、RAG、LLM、多 Agent、真实 Wiki/历史 SLA 或真实 SLA 创建接口。智能输出是 `模拟数据`，包含依据、置信度、人工介入建议和待补充信息，不是最终根因、最终方案或正式复盘结论。

能力矩阵：Engineering Baseline `DONE`；Architecture Skeleton `IN PROGRESS`；AIOps Host Integration `PROTOTYPE`；Persistence、Knowledge Ingestion、Retrieval、LLM Generation、Agent Orchestration 均为 `NOT STARTED`。
