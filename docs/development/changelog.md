# 变更记录

Status: ACTIVE

Owner: 研发负责人

Last reviewed: 2026-07-20

Source of truth for: 当前技术 MVP 的用户可见实施增量

## 2026-07-20

- 文档治理：统一 Issue #24 在产品、项目、架构、API 与开发文档中的已实现状态，并明确 v1 `openapi.yaml` 与 v2 `openapi-v2.yaml` operation 级 `IMPLEMENTED` 的契约事实职责；Evaluation、Metrics、Admin Reset 等剩余目标继续保持未实现。
- Issue #24：实现独立持久化 Feedback 与 SubmissionContinuation、`CONTINUED_SUBMISSION` 终止语义，以及按 ADMIN 产品线授权查询的脱敏不可变 AuditEvent；前端新增独立反馈/继续提交和管理员审计页，且不创建 SLA、工单草稿、`ticketId` 或回执。
- Issue #26：实现授权优先的在线混合检索与不可变 Evidence 快照。FTS/向量召回均在排序前过滤当前身份产品线和当前 `PUBLISHED` 版本，固定采用两路各 Top 20、RRF `k=60`、最终 Top 5 与稳定 UUID 同分排序；每个 Run 重新检索并保存规则、候选、rank、模式和 Evidence 审计快照。
- `GET /api/v2/evidence/{evidenceId}` 每次按当前身份重新授权，不存在与无权限统一安全 `404`，不返回宿主路径或直接文件 URL。Embedding 故障降级为 `FTS_ONLY / LOW`，FTS 故障降级为 `UNAVAILABLE / LOW`，两者均保留人工继续提交；历史 Run 与 Evidence 不随服务恢复或权限变化改写。Sandbox 新增三种检索模式、降级原因、置信度和受控 Evidence 查看。

## 2026-07-19

- Issue #25：接入固定 `multilingual-e5-base` 离线服务、pgvector 768 维与 PostgreSQL FTS，新增可恢复且最多三次尝试的 `IndexTask`、原子发布/旧版废弃、显式废弃 API 和最小治理 UI；模型制品保持 Git 外置，在线 Retrieval/Evidence 仍未实现。

- Decision Gate #19 经人工复核 Issue #39、PR #42、资格报告、模型 manifest、依赖锁、SBOM/NOTICE、镜像绑定和仓库外原始结果后确认为 `PASS` 并关闭；Issue #25 已在固定制品、逐文件 SHA-256 校验、离线运行和受控要素变化后重新评估的边界下完成接入。模型文件仍不提交 Git 或写入镜像，由默认 Compose 以只读外置制品加载。

## 2026-07-17

- Issue #39：新增可整体删除的隔离 Embedding 资格 Harness，完成固定 revision 五文件受控取件、实际 SHA-256 manifest、依赖锁/SBOM/NOTICE、`network=none` CPU 真实推理、4 GiB 资源与查询实测、32/128/512 精确 Token 桶的 36 组批量基线及 `mock-eval-v1` 四项资格指标；当日工程总体建议为 `PASS`，Decision Gate #19 当时仍等待人工确认，模型未进入 Git、默认 Compose 或产品运行时。

## 2026-07-16

- Issue #23：新增 API v2 持久化预诊 Session/Run 基础闭环，覆盖业务幂等、完整 Context 与策略/结果快照、最多三轮补充、本人恢复、终态只读、显式自助结束、确定性 Mock 降级输出、最小前端 Sandbox，以及真实 PostgreSQL 并发与重启恢复门禁。
- Issue #22：实现不可变知识草稿修订、职责分离送审、审核退回和批准命令，并持久化不可变审核历史；知识发布、索引和检索仍未实现。
- Issue #19：记录固定默认 Embedding、内部非商用使用边界、供应链与安全约束、资源和 `mock-eval-v1` 资格协议；该日阶段状态为 `BLOCKED`，本增量不下载、运行、打包或默认启用模型制品。

## 2026-07-15

- Issue #16：交付 `mock-eval-v1` 的 30 条固定模拟案例、稳定身份/Evidence fixture manifest、JSON Schema、覆盖矩阵与规范化 SHA-256；新增独立 CI 校验和维护升级指南。本增量不实现 EvaluationRun 运行时、失败案例 Controller、指标或页面，也不变更 API v1。
- Issue #16：确认固定评估集 `mock-eval-v1` 的最小样例结构、语言与场景覆盖，并在 API v2 DRAFT 契约中增加仅 `ADMIN` 可读的评估失败案例分页安全摘要。
- 正常执行但未达到工程门禁的评估任务保持 `SUCCEEDED`，由 `gatePassed=false` 表示质量未通过；失败案例不得暴露输入正文、无权 Evidence 标识或摘录、宿主路径及内部推理。
- Issue #16 仅批准契约与范围说明，相关 v2 operation 仍标记为 `NOT_IMPLEMENTED`；API v1 契约与 Mock 行为保持不变。
- Issue #21：新增本地模拟知识首次上传、不可变 Compose 文件 volume、PostgreSQL/Flyway 元数据、异步 ParseTask、Markdown/TXT/文本型 PDF 解析，以及解析摘要、Block、Chunk 分页预览。
- API v2 仍为 `DRAFT / PARTIALLY_IMPLEMENTED / APPROVED_FOR_IMPLEMENTATION`；仅真实交付的 operation 标记为 `IMPLEMENTED`，未开始修订、审核、发布、检索或 Issue #23。
- API v1 契约与 Mock 行为保持不变；未接入真实身份、生产数据库、真实知识源或外部系统。

### Issue #21 完成交接

- 后端 Spotless、57 项测试和 JAR 打包均通过；前端 lint、格式、OpenAPI v1/v2、20 项测试和生产构建均通过，API v1 未发生变更。
- backend 与 frontend Compose 镜像分别完成真实构建；本次基础镜像元数据获取成功，不存在尚未记录的镜像拉取阻塞。
- 使用标注为 `模拟数据` 的 Markdown 完成真实 HTTP 登录、上传、ParseTask 轮询及摘要、Block、Chunk 三个预览端点验收；重启 backend 后 Session、PostgreSQL 记录、原始文件哈希与解析预览均保持一致。
- 对照 `main`、Issue #21、父 Spec #12 与 Issue #15 完成双轴 `code-review`；已修复真实 token 窗口、按退避截止时间启动恢复、指数退避、解析任务级结果哈希、PDF 风险告警、生命周期状态、重复 Cookie 解析、Compose CI 持久化覆盖及文档事实冲突。
- Issue #16 的评估失败案例契约与固定评估集说明不属于 Issue #21，已从当前分支移除；API v2 仅 3 个既有认证 operation 与 Issue #21 实际交付的 5 个 operation 标记为 `IMPLEMENTED`。
