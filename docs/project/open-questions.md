# 待确认问题（ACTIVE）

1. `EXTERNAL QUESTION`：MIT License 的对外开源意图待负责人确认。
2. `EXTERNAL QUESTION`：Copyright 版权主体待负责人/法务确认。
3. `EXTERNAL QUESTION`：公司内部使用、修改和分发授权边界待负责人/法务确认。
4. `EXTERNAL QUESTION`：一期本地模拟/公开/脱敏数据已确认保留至受控重置；真实数据的分类、保留删除期限、迁移和恢复责任仍待数据/安全负责人确认，该结论不得直接沿用到二期。
5. `EXTERNAL QUESTION`：真实知识源的授权白名单、脱敏、版权、文档及 Chunk 权限继承待知识/安全负责人确认。
6. `EXTERNAL QUESTION`：外部或内部模型的调用边界、数据出域、成本预算和模型审计待安全/架构负责人确认。
7. `EXTERNAL QUESTION`：AIOps/ITSM 的只读范围、人工提交责任、失败回滚与审计契约待业务/系统负责人确认。
8. `OPEN QUESTION`：一期固定评估集已确认不少于 30 条，并已确认权限、引用、降级和 Recall@5 工程门槛；具体样例内容、业务指标基线、统计周期及非门禁指标目标值仍待产品与 SLA 处理人确认。
9. `EXTERNAL QUESTION`：一期默认 Embedding 已选定 `intfloat/multilingual-e5-base@d13f1b27baf31030b7fd040960d60d909913633f`；MIT 模型卡与依赖许可证兼容性、本机资源实测及固定评估集表现仍待架构、安全与法务负责人复核，未通过前不得下载或引入模型制品。
10. `OPEN QUESTION`：已确认保留当前 v1、通过 API v2 表达新语义，并冻结一期 v2 资源组；进入实现前仍须由 API 负责人完整定义并人工确认方法、请求/响应 Schema、错误码、幂等语义、分页、异步任务状态及 v1→v2 映射。此项是当前 `NOT_READY` 的直接阻塞项。
11. `OPEN QUESTION`：`PrecheckContext` 领域语义及一期最小必填字段已冻结，并确认由 API v2 承载；可选字段约束、稳定本地枚举及附件元数据 Schema 仍须在实施前由产品与 API 负责人确认。
12. `EXTERNAL QUESTION`：`PrecheckContext` 与真实 AIOps 表单、枚举及附件标识的映射待 AIOps、产品与 API 负责人确认；不影响一期 Sandbox 的本地模拟闭环。

结论形成前不修改 `LICENSE`，也不宣称明确的开源或内部授权状态。第 4–7 项仅阻止对应真实数据或外部系统接入，不阻止已确认的一期本地模拟闭环；第 9 项未通过前不得下载或启用默认模型制品；第 10–11 项完成前不得开始依赖 v2 契约的实现。
