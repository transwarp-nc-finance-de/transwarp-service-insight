# Issue tracker: GitHub

- Status: `ACTIVE`
- Owner: Transwarp Service Insight Maintainers
- Last reviewed: 2026-07-14
- Source of truth for: 工程 Skill 使用的 Issue Tracker 与发布约定

本仓库的 Spec、实施 Ticket 和工作项发布为 GitHub Issues，使用 `gh` CLI 操作。

## Repository

- GitHub repository: `transwarp-nc-finance-de/transwarp-service-insight`
- Issue tracker: GitHub Issues
- Pull requests as a triage request surface: no

## Conventions

- 创建 Issue：`gh issue create`
- 查看 Issue：`gh issue view <number> --comments`
- 列出 Issue：`gh issue list`
- 评论 Issue：`gh issue comment <number>`
- 修改标签：`gh issue edit <number> --add-label/--remove-label`
- 关闭 Issue：`gh issue close <number>`

当 Skill 要求“发布到 Issue Tracker”时，创建 GitHub Issue。
当 Skill 要求获取 Ticket 时，读取对应 Issue 正文、标签和评论。

GitHub Issues 与 Pull Requests 共用编号空间；遇到裸编号时，先判断其资源类型。

## Wayfinding

如使用 Wayfinder：

- Map 使用单个 GitHub Issue，并标记 `wayfinder:map`
- 子任务优先使用 GitHub Sub-issues；不可用时使用任务列表并注明父 Issue
- 阻塞关系优先使用 GitHub 原生 Issue Dependencies；不可用时使用 `Blocked by: #<number>`
- Claim 时将 Issue 分配给当前执行者
- Resolve 时记录结论、关闭 Issue，并把上下文链接回写父级 Map
