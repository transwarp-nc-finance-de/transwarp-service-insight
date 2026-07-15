# 固定模拟评估集维护指南

Status: ACTIVE

Owner: Service Insight Maintainers

Last reviewed: 2026-07-15

Source of truth for: `mock-eval-v1` 固定评估数据资产、校验命令与版本升级规则

## 范围与边界

`mock-eval-v1` 是 Issue #16 交付的固定 `模拟数据` 资产，用于后续 EvaluationRun 的工程评估输入。它不执行检索、不调用模型、不连接外部服务，也不实现 EvaluationRun、失败案例 Controller、指标聚合或管理员页面。API v1 和 API v2 operation 状态均不由本数据资产改变。

数据目录为 `backend/src/main/resources/evaluation/mock-eval-v1/`：

- `dataset.json`：30 条按 `caseId` 升序排列的固定案例，包含完整宿主无关 PrecheckContext、多轮输入、语言/场景标签、执行身份、允许产品线、期望/禁止 Evidence、预期检索模式、降级和待补充字段。
- `evidence-fixture-manifest.json`：稳定的模拟执行身份与 Evidence fixture；Evidence 使用稳定模拟 UUID 绑定 Document、KnowledgeVersion、Chunk、最小受控摘录及其 SHA-256。
- `dataset.schema.json` 与 `evidence-fixture-manifest.schema.json`：资产结构、枚举、必填字段、三轮上限和模拟标记的 JSON Schema。
- `coverage-matrix.json`：语言、场景和降级维度到稳定 `caseId` 的机器可校验映射。
- `dataset.sha256`：`dataset.json` 的规范化 SHA-256。

## 自动校验

Windows：

```powershell
cd backend
.\mvnw.cmd -Dtest=EvaluationDatasetValidationTest test
```

macOS/Linux：

```bash
cd backend
./mvnw -Dtest=EvaluationDatasetValidationTest test
```

该命令是 CI 后端 job 的独立门禁，失败后不得通过跳过、禁用、忽略测试或空断言绕过。常规 `spotless:check test` 仍会再次执行该校验并完成全量回归。

校验会以 JSON Schema 实际验证两份资产，并验证样例数、稳定唯一 ID、完整 PrecheckContext、版本和模拟标记、身份/产品线/Evidence 引用、不可变引用绑定与摘录哈希、期望与禁止 Evidence 互斥、多轮上限、必需覆盖、权限拒绝目标、FTS_ONLY/UNAVAILABLE、安全内容、覆盖矩阵、排序、禁用测试和 checksum。它验证固定资产本身，不代表真实检索效果或生产 SLA。

## Checksum 规则

checksum 使用 UTF-8 JSON 规范化结果：递归按字典序排列对象键，保留数组顺序，再计算 SHA-256，格式为 `sha256:<64 位小写十六进制>`。因此平台换行和缩进不会改变结果，但案例顺序、轮次、字段值或数组顺序的语义变化会改变结果。

修改数据后先运行校验。仅当失败信息只包含预期的新 checksum，且覆盖矩阵、权限边界和评审结果均已确认时，才更新 `dataset.sha256`；不得为了消除未知差异直接复制 checksum。

## 维护与升级

`mock-eval-v1` 发布后保持不可变。仅修正明确的数据错误时允许在同一版本修改，并须在 PR 中说明错误、影响案例、覆盖矩阵变化和 checksum 变化。增加场景、改变字段语义、身份权限或 Evidence 期望时创建新版本目录（例如 `mock-eval-v2`），不得静默改写 v1 语义。

升级版本时必须同步：

1. 复制并更新 dataset、fixture manifest、两份 Schema、覆盖矩阵和 checksum；
2. 更新 classpath catalog 的显式版本白名单/资源映射，不得让未知版本回退到默认集；
3. 增加针对新版本的真实行为校验，并保留旧版本回归；
4. 更新本指南、测试策略和变更记录；若影响未来 API v2 DRAFT 契约，另行按契约治理评审；
5. 只使用公开、脱敏或模拟内容，所有输入、标题和摘录继续标注 `模拟数据`；
6. 由人工确认 PR，不自动合并。评估输出仍须提供依据来源、置信度、人工介入建议和待补充信息，且不得成为最终根因、最终方案或正式复盘结论。
