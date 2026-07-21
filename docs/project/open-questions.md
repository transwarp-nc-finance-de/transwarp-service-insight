# 待确认问题（ACTIVE）

1. `EXTERNAL QUESTION`：MIT License 的对外开源意图待负责人确认。
2. `EXTERNAL QUESTION`：Copyright 版权主体待负责人/法务确认。
3. `EXTERNAL QUESTION`：公司内部使用、修改和分发授权边界待负责人/法务确认。
4. `EXTERNAL QUESTION`：一期本地模拟/公开/脱敏数据已确认保留至受控重置；真实数据的分类、保留删除期限、迁移和恢复责任仍待数据/安全负责人确认，该结论不得直接沿用到二期。
5. `EXTERNAL QUESTION`：真实知识源的授权白名单、脱敏、版权、文档及 Chunk 权限继承待知识/安全负责人确认。
6. `EXTERNAL QUESTION`：外部或内部模型的调用边界、数据出域、成本预算和模型审计待安全/架构负责人确认。
7. `EXTERNAL QUESTION`：AIOps/ITSM 的只读范围、人工提交责任、失败回滚与审计契约待业务/系统负责人确认。
8. `CONFIRMED`：`mock-eval-v1` 已交付 30 条版本化 `模拟数据` 样例、Evidence fixture、Schema、覆盖矩阵和 checksum 校验；权限、引用、降级和 Recall@5 工程门槛也已确认。业务指标基线、统计周期及非门禁指标目标值仍为后续观察项，不构成当前开发阻塞。
9. `CONFIRMED`：用户于 2026-07-16 以项目唯一开发者及全部责任角色身份确认一期默认 Embedding 为 `intfloat/multilingual-e5-base@d13f1b27baf31030b7fd040960d60d909913633f`，仅限内部非商用使用且不对外分发；接受模型与依赖许可证及训练数据权利链剩余风险，确认固定安全边界、代表性测试机、资源门槛和 `mock-eval-v1` 评估门槛，并允许后续独立 Ticket 进行一次受控下载。该确认在当时不代表门禁通过；后续最终结论见第 13 项。
10. `CONFIRMED`：保留当前 v1、通过 API v2 表达新语义，并冻结一期 v2 资源组；方法、请求/响应 Schema、错误码、幂等语义、分页、异步任务状态及 v1→v2 映射的 DRAFT 已由用户以 API、产品、安全负责人身份于 2026-07-14 明确批准进入实施。需求范围为 `CONFIRMED`，API v2 为 `APPROVED_FOR_IMPLEMENTATION`，一期实施为 `READY_FOR_IMPLEMENTATION`；当前已实现 AuthSession、知识上传/治理/发布、持久化 Precheck Session/Run、授权混合 Retrieval 与 Evidence、独立 Feedback、SubmissionContinuation、结构化 AuditEvent、EvaluationRun、Metrics 与仅限本地环境的受控 Admin Reset。其他未标记 `IMPLEMENTED` 的 v2 operation 仍未实现。
11. `CONFIRMED`：`PrecheckContext` 领域语义、一期最小必填字段、可选字段约束、稳定本地枚举形状及附件元数据 Schema 已随 API v2 DRAFT 于 2026-07-14 获人工批准；`local-identity-v1` 与 `local-catalog-v1` 的稳定本地种子值已确认并在 AuthSession/PostgreSQL 基础切片中实现。
12. `EXTERNAL QUESTION`：`PrecheckContext` 与真实 AIOps 表单、枚举及附件标识的映射待 AIOps、产品与 API 负责人确认；不影响一期 Sandbox 的本地模拟闭环。
13. `CONFIRMED`：Issue #39 已产生固定 revision 的完整 SHA-256 manifest、依赖锁/SBOM/NOTICE、4 GiB 资源与三轮 P95、36 组批量基线及 `mock-eval-v1` 资格结果；工程门禁总体建议为 `PASS`，详见 [资格实测报告](../development/embedding-model-qualification-report.md)。用户于 2026-07-19 人工复核原始 checksum、供应链材料和门禁表后，将 Issue #19 最终确认为 `PASS` 并关闭；Issue #25 已在该批准边界内完成固定制品接入与双索引原子发布闭环。
14. `CONFIRMED`：用户于 2026-07-20 明确要求解决文档状态冲突；v1 继续以 `docs/api/openapi.yaml` 为唯一 v1 契约，v2 以 `docs/api/openapi-v2.yaml` 中 operation 级 `IMPLEMENTED` 为唯一 v2 运行时契约，未标记 `IMPLEMENTED` 的设计保持 `NOT_IMPLEMENTED / DRAFT`。根 `AGENTS.md`、文档导航与契约说明已同步，不再保留“openapi.yaml 是全部版本唯一契约”的冲突表述。
15. `OPEN QUESTION`：Issue #27 的 `mock-eval-v1` 固定集在真实 v2 路径运行时，30 条样例的 `expectedMissingFieldCodes` 均与当前 `mock-completeness-v1` 完整性策略产生非硬性差异（当前策略额外要求 `OCCURRED_AT` 及问题类型相关字段）；四项工程门槛仍全部通过，差异已按失败样例安全持久化且不得通过改写固定集或特殊判断掩盖。后续需由产品与评测负责人决定是在新版本固定集中对齐期望，还是调整完整性策略；在结论形成前保留 `mock-eval-v1` 的版本和 checksum。

在第 1–3 项结论形成前不修改仓库 `LICENSE`，也不宣称项目对外开源。第 4–7 项仅阻止对应真实数据或外部系统接入，不阻止已确认的一期本地模拟闭环；第 9 项的一次受控取件授权已由 Issue #39 使用完毕，第 13 项的 Issue #19 人工门禁已通过。模型文件不得进入 Git 或镜像；Issue #25 已在校验批准制品、只读挂载和离线运行边界下完成默认 Compose 与产品运行时接入。v2 可按已批准契约继续实施。
