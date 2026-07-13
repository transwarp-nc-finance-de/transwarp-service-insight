# 当前验收标准

状态：ACTIVE。

- AIOps 是 SLA 表单、枚举、原有校验和最终提交主体；Service Insight 不创建 SLA。
- `/sandbox` 是本地 Mock AIOps；`/embed` 不显示完整 SLA 表单；PrecheckPanel 可复用。
- HostBridge 和 postMessage v1.0 校验 origin、version、requestId、大小与重复初始化。
- 应用层拥有预诊工作流；Infrastructure 不依赖 HTTP DTO；ArchUnit 规则真实执行。
- 初次预诊创建 Session/Run 1，追问创建递增 Run；无效 Session 返回结构化错误。
- 所有结果包含模拟依据、置信度原因、人工介入建议、待补充信息、结构化降级和 `CONTINUE_SUBMISSION`。
- 反馈失败、预诊失败和用户未采纳均不阻断 AIOps 原流程人工提交。
- OpenAPI、Java DTO、TypeScript 类型和行为测试一致；后端、前端、Compose 与浏览器冒烟通过后方可交付。
- 不接真实 AIOps、数据库、RAG、LLM、多 Agent、真实数据或真实 SLA 提交。
