# 待确认问题（ACTIVE）

1. `EXTERNAL QUESTION`：MIT License 的对外开源意图待负责人确认。
2. `EXTERNAL QUESTION`：Copyright 版权主体待负责人/法务确认。
3. `EXTERNAL QUESTION`：公司内部使用、修改和分发授权边界待负责人/法务确认。
4. `EXTERNAL QUESTION`：一期本地模拟/公开/脱敏数据已确认保留至受控重置；真实数据的分类、保留删除期限、迁移和恢复责任仍待数据/安全负责人确认，该结论不得直接沿用到二期。
5. `EXTERNAL QUESTION`：真实知识源的授权白名单、脱敏、版权、文档及 Chunk 权限继承待知识/安全负责人确认。
6. `EXTERNAL QUESTION`：外部或内部模型的调用边界、数据出域、成本预算和模型审计待安全/架构负责人确认。
7. `EXTERNAL QUESTION`：AIOps/ITSM 的只读范围、人工提交责任、失败回滚与审计契约待业务/系统负责人确认。
8. `OPEN QUESTION`：一期固定评估集已确认不少于 30 条，并已确认权限、引用、降级和 Recall@5 工程门槛；具体样例内容、业务指标基线、统计周期及非门禁指标目标值仍待产品与 SLA 处理人确认。
9. `CONFIRMED`：用户于 2026-07-16 以项目唯一开发者及全部责任角色身份确认一期默认 Embedding 为 `intfloat/multilingual-e5-base@d13f1b27baf31030b7fd040960d60d909913633f`，仅限内部非商用使用且不对外分发；接受模型与依赖许可证及训练数据权利链剩余风险，确认固定安全边界、代表性测试机、资源门槛和 `mock-eval-v1` 评估门槛，并允许后续独立 Ticket 进行一次受控下载。本确认不允许 Issue #19 本身下载模型，也不代表模型门禁通过。
10. `CONFIRMED`：保留当前 v1、通过 API v2 表达新语义，并冻结一期 v2 资源组；方法、请求/响应 Schema、错误码、幂等语义、分页、异步任务状态及 v1→v2 映射的 DRAFT 已由用户以 API、产品、安全负责人身份于 2026-07-14 明确批准进入实施。需求范围为 `CONFIRMED`，API v2 为 `APPROVED_FOR_IMPLEMENTATION`，一期实施为 `READY_FOR_IMPLEMENTATION`；当前已实现 AuthSession、知识上传解析预览，以及不可变草稿修订、送审、退回和批准，发布、检索、评估等其余 v2 operation 仍未实现。
11. `CONFIRMED`：`PrecheckContext` 领域语义、一期最小必填字段、可选字段约束、稳定本地枚举形状及附件元数据 Schema 已随 API v2 DRAFT 于 2026-07-14 获人工批准；`local-identity-v1` 与 `local-catalog-v1` 的稳定本地种子值已确认并在 AuthSession/PostgreSQL 基础切片中实现。
12. `EXTERNAL QUESTION`：`PrecheckContext` 与真实 AIOps 表单、枚举及附件标识的映射待 AIOps、产品与 API 负责人确认；不影响一期 Sandbox 的本地模拟闭环。
13. `OPEN QUESTION`：默认 Embedding 的小型配置文件 SHA-256、最终依赖锁/SBOM/NOTICE、4 GB 内存限制下的资源与 P95 实测、批量索引基线及 `mock-eval-v1` 真实资格结果尚未产生；上述证据由 [Issue #39](https://github.com/transwarp-nc-finance-de/transwarp-service-insight/issues/39) 跟踪。证据齐备前 Issue #19 保持 `BLOCKED`，本 Issue 不下载模型，也不得打包或默认启用模型制品。

结论形成前不修改仓库 `LICENSE`，也不宣称项目对外开源。第 4–7 项仅阻止对应真实数据或外部系统接入，不阻止已确认的一期本地模拟闭环；第 9 项只授权后续独立 Ticket 受控取件，第 13 项证据齐备前不得将模型门禁标为 `PASS`，也不得在 Issue #19 中下载、打包或默认启用模型制品。v2 可按已批准契约继续实施。
