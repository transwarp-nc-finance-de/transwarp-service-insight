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

置信度由版本化确定性策略计算并返回理由，不接受生成内容自行判断：无有效证据、权限过滤后无证据、关键信息缺失或 Embedding 降级为 `LOW`；有效证据存在但单一、两路检索不一致或仍需补充信息为 `MEDIUM`；只有完整性通过、证据充分、FTS 与向量结果相互印证且无降级才可为 `HIGH`。该值是辅助判断，不是最终根因可信度。

## 一期已确认目标边界

每个 `PrecheckSession` 最多包含 3 个 Run：初诊 Run 1 加最多两次补充。用户可在任意 Run 跳过补充并继续提交；达到 Run 3 后不得创建下一轮，只展示剩余待补充信息与人工介入建议。该上限尚未在当前 API 与内存工作流中实现。

Run 的执行完成与 Session 的业务终止是不同概念。信息完整、Run 完成或达到上限均不自动终止 Session；只有用户明确确认模拟继续提交或采纳建议并结束预诊时，Session 才分别以 `CONTINUED_SUBMISSION` 或 `SELF_SERVICE_CONFIRMED` 原因终止。反馈动作本身不终止 Session。

用户可恢复自己尚未终止且仍可补充的 Session，终态 Session 只能读取。恢复时重新校验当前身份和产品线权限；管理员不得接管或恢复其他用户的 Session。

产品线、产品或组件变化代表问题与权限边界变化，必须创建新 Session。版本、级别、描述等变化可在原 Session 创建下一 Run，但必须重新执行过滤与检索。每个 Run 保存完整规范化上下文快照；历史 Run 输入快照和证据不可变。

反馈与模拟继续提交是独立命令与事件，分别处理失败、重试和幂等。反馈失败不影响继续提交；继续提交只记录本地模拟确认，不创建真实 SLA。当前 v1 API 仍耦合两个字段且保持兼容；独立命令已由 `APPROVED_FOR_IMPLEMENTATION` 的 API v2 DRAFT 表达。

完整度评估使用版本化确定性策略，由通用必需项和问题类型专属清单组成。每个 Run 保存策略版本、缺失项与命中说明。用户可以跳过任意补充项；策略结果不得阻断模拟继续提交。

策略来自受评审的版本化配置文件，并随 `模拟数据` 初始化进入数据库。管理员只读查看，不在 UI 编辑；策略更新创建新版本，既有 Run 不追溯重算。

一期问题类型为 `FUNCTIONAL_FAILURE`、`PERFORMANCE_DEGRADATION`、`INSTALLATION_CONFIGURATION`、`DATA_CORRECTNESS`。本地编码稳定并进入策略快照，但与未来 AIOps 枚举的映射尚未确认。

通用完整度项为产品、组件、版本、问题级别、影响范围和 `OCCURRED_AT`。类型专属项：功能故障检查 `ERROR_MESSAGE / REPRODUCTION_STEPS / RECENT_CHANGES`；性能下降检查 `TIME_WINDOW / BASELINE_METRIC / CURRENT_METRIC / WORKLOAD_IDENTIFIER`；安装配置检查 `OPERATION_GOAL / EXECUTED_STEPS / ENVIRONMENT_SUMMARY / ERROR_MESSAGE`；数据正确性检查 `EXPECTED_RESULT / ACTUAL_RESULT / AFFECTED_DATA_SCOPE / QUERY_OR_JOB_ID`。所有缺失项都只能触发建议补充。
