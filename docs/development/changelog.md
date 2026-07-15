# 变更记录

Status: ACTIVE

Owner: 研发负责人

Last reviewed: 2026-07-15

Source of truth for: 当前技术 MVP 的用户可见实施增量

## 2026-07-15

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
