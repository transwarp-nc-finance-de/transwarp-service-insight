# 变更记录

Status: ACTIVE

Owner: 研发负责人

Last reviewed: 2026-07-15

Source of truth for: 当前技术 MVP 的用户可见实施增量

## 2026-07-15

- Issue #21：新增本地模拟知识首次上传、不可变 Compose 文件 volume、PostgreSQL/Flyway 元数据、异步 ParseTask、Markdown/TXT/文本型 PDF 解析，以及解析摘要、Block、Chunk 分页预览。
- API v2 仍为 `DRAFT / PARTIALLY_IMPLEMENTED / APPROVED_FOR_IMPLEMENTATION`；仅真实交付的 operation 标记为 `IMPLEMENTED`，未开始修订、审核、发布、检索或 Issue #23。
- API v1 契约与 Mock 行为保持不变；未接入真实身份、生产数据库、真实知识源或外部系统。

### 交接时尚待完成

- 完整 Compose 镜像构建因 Docker 基础镜像拉取长时间无响应而超时；本机 PostgreSQL 已健康启动，`knowledge-files` 命名卷已创建，但尚未完成真实 HTTP 上传后的 backend 重启与数据库/文件恢复验证。
- 尚待执行对照 `main`、Issue #21、父 Spec #12 与 Issue #15 的最终 `code-review`，修正审查结果后再创建小型 PR；不得自动合并。
- 当前快照还携带开始 Issue #21 前已存在的 Issue #16 文档工作区改动，集中在 `docs/api/openapi-v2.yaml` 的评估失败案例契约和 `docs/project/scope.md` 的固定评估集说明；开 PR 前需拆分或确认归属。
