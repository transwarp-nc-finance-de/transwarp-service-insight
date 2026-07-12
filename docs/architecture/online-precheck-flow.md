# 在线预诊流程

Status: ACTIVE  
Owner: 后端负责人  
Last reviewed: 2026-07-12  
Source of truth for: 在线预诊顺序、状态和降级边界

## 当前 Mock 流程

```text
HTTP 校验 → 模拟策略快照 → 确定性关键词 Adapter → 结构化结果
          ↘ 脱敏业务事件（不含正文）
```

初始预诊与追问均同步、无状态、无持久化。结果状态使用 `NEED_MORE_INFORMATION` 或 `COMPLETED`，始终返回 `CONTINUE_SUBMISSION`。页面内会话刷新即清空。

## 目标工作流（DRAFT）

```text
RECEIVED → VALIDATED → POLICY_RESOLVED → CLASSIFIED
→ 信息充分性判断 → 权限前置检索 → 重排序 → 结构化生成
→ 引用核验 → 安全策略 → COMPLETED
```

允许分支为 `NEED_MORE_INFORMATION`、`DEGRADED`、`FAILED` 和 `CANCELLED`。任何分支都不得禁止人工继续提交。在线链路只读、低延迟、可超时降级，禁止同步执行知识导入、索引重建或外部写操作。
