# 在线预诊流程

状态：ACTIVE（本地 Mock 实现）。

```text
AIOps 表单快照 / Sandbox 模拟快照
→ 创建 Session 与 Run 1 → 策略快照、完整度评估
→ 确定性 Mock 检索、建议和 Evidence 核验
→ 更新 Run 与 Session并记录不含正文的内存审计
→ 用户补充时在同一 Session 创建 Run N
→ 用户人工确认后由 AIOps 原流程提交
```

信息不足返回 `NEED_MORE_INFORMATION`；能力和证据不足通过结构化降级说明表达。任何状态、低置信度和服务失败均保留 `CONTINUE_SUBMISSION`。旧 follow-up API 按 precheckId 定位 Session，新会话 API 使用 sessionId；所有会话仅在当前进程内，重启清空。
