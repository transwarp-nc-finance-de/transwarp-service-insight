# Issue #29 Compose 完整模式与 FTS 降级验收报告

- Status: ACTIVE
- Owner: Service Insight 工程团队
- Last reviewed: 2026-07-22
- Source of truth for: Issue #29 本地 Compose 与浏览器端到端验收结果

## 结论

Issue #29 的本地技术验收通过。完整模式使用 Issue #19 已批准的五文件离线模型制品；FTS-only 与运行时故障均未下载或接入外部模型。所有业务输入、知识、身份、评估与审计内容均为 `模拟数据`。

本结论只证明当前提交在本地技术环境中的行为，不是最终根因、最终方案、正式复盘、真实试点效果或生产上线批准。SLA 是否提交及提交内容仍由人工确认。

## 可重复执行入口

在仓库根目录执行：

```powershell
.\tools\compose-acceptance\run.ps1 -ModelPath "<Issue #19 已批准模型目录的绝对路径>"
```

脚本使用隔离的 `service-insight-issue29` Compose project，逐文件核对 `tools/embedding-qualification/evidence/model.manifest` 中的大小与 SHA-256，不下载模型；随后执行完整四服务、运行时故障、恢复、重启持久化、FTS-only 与 v1 浏览器回归。默认在结束时删除该隔离项目的容器和 `模拟数据` volumes；诊断时可加 `-KeepEnvironment`。

## 验收证据

| 场景 | 用户可观察行为 | 结果 |
| --- | --- | --- |
| 完整四服务 | PostgreSQL、local-embedding、backend 健康，frontend 可访问 | PASS |
| 知识纵向闭环 | 浏览器上传/解析/修订/送审；编辑者不能自审；审核者批准并完成 FTS + 768 维向量原子发布 | PASS |
| 授权预诊 | 新 Session/多轮 Run 为 HYBRID，可查看不可变 Evidence，反馈与人工继续提交独立持久化 | PASS |
| Audit/Evaluation | ADMIN 可见脱敏结构化审计；固定 30 条 `模拟数据` 评估完成并展示指标 | PASS |
| 显式 FTS-only | 不启动 embedding，backend 保持健康，浏览器展示 FTS_ONLY、LOW、依据、人工介入建议、待补充信息和免责声明 | PASS |
| 运行时故障 | 停止 embedding 后 backend 保持健康；浏览器收到 FTS_ONLY，而不是网关 504 | PASS |
| 故障恢复 | embedding 预热且 healthy 后，新 Run 恢复 HYBRID，已有 FTS_ONLY Run 数量与内容不变 | PASS |
| 重启持久化 | PostgreSQL/backend 重启前后知识、版本、索引、Session/Run、反馈、人工继续、审计与评估计数一致，上传原文件路径与 SHA-256 一致 | PASS |
| v1 回归 | Sandbox 初诊、追问、反馈、失败降级与人工继续提交行为保持兼容 | PASS |

Playwright 用例位于 `frontend/e2e/`；FTS-only 与 v1 回归已加入 GitHub Actions 的 `compose-smoke` job。完整模型模式不在托管 runner 下载制品，必须使用已批准的本地模型目录执行上述脚本。

## 验收中修复的问题

1. 运行时 embedding 不可达时，后端已生成 FTS_ONLY，但 Docker 网络失败窗口约四秒，超过 Nginx 原三秒读取超时，浏览器先收到 504。网关读取超时已调整为八秒，并由真实浏览器故障用例覆盖。
2. 模型容器重启后，健康端点可能先于第一次推理预热完成，导致恢复后的首个 Run 再次超时降级。服务现在启动时先执行一条标记为 `模拟数据` 的查询预热，完成后才开放 HTTP/health。
3. 知识治理页面原先没有用户可见的稳定 version ID，浏览器无法在职责分离的身份切换后继续审核同一对象；页面现已显示该标识。

## 依据、置信度与人工介入

- 依据来源：Compose health、浏览器页面与 API 公共行为、PostgreSQL 持久化计数、模型 manifest 大小/SHA-256、现有 v1/v2 行为测试。
- 置信度：`HIGH`（本地技术验收范围）；不外推到真实数据、生产负载或外部系统。
- 人工介入建议：PR 合并前人工检查 GitHub Actions、确认完整模式验收使用的仍是 Issue #19 批准制品，并审阅本报告与 Issue #29 范围是否一致。
- 待补充信息：分支 PR 上的远程 CI 结果；真实试点、外部系统和生产准入仍需独立授权与评审。
