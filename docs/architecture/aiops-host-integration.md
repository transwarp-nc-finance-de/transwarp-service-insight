# AIOps 宿主集成

状态：ACTIVE（协议为本地 PROTOTYPE）。第一期推荐 iframe + postMessage，以隔离未知的 AIOps 前端技术栈；Native API 和 Web Component 作为后续候选。

当前 `/sandbox` 模拟宿主，`/embed` 是不包含 SLA 表单的嵌入面板。真实来源通过 `VITE_AIOPS_ALLOWED_ORIGINS` 配置，不硬编码生产域名。Embed 不读取宿主 DOM，不通过 URL 传表单内容，附件只接收元数据或受控引用。

接入前需由 AIOps 团队确认：Vue/React 及版本、是否允许改前端和 iframe、CSP/X-Frame-Options、稳定字段编码、富文本转纯文本方式、附件引用、登录鉴权、测试环境、API Gateway、浏览器范围和发布流程。

## 一期已确认预留边界

一期冻结 `PrecheckContext` 的宿主无关领域语义：`sourceSystem`、`hostRequestId`、`formSchemaVersion`、`issueType`、`productLine`、`product`、`component`、`version`、`issueLevel`、`serviceType`、`title`、`descriptionPlainText`、`additionalInformation`、`impactScope` 与附件元数据。领域对象与 HTTP DTO 通过 Mapper 隔离；当前 v1 OpenAPI 无法完整表达该语义，本轮不修改已实现契约。

一期创建 Session 的必需字段为 `sourceSystem`、`hostRequestId`、`formSchemaVersion`、`issueType`、`productLine`、`title`、`descriptionPlainText`。缺失时预诊请求失败，但宿主继续提交流程仍可用。当前 v1 只强制标题和描述，目标兼容方案尚未确认。

`additionalInformation` 是稳定编码、显示名和纯文本值组成的条目集合。未知宿主编码保留用于追溯，但只有当前策略认识的编码参与完整度判断；未知值不得扩大权限或改变规则。

一期 Sandbox 只验证 `模拟数据` 问题附件 ID 与文件名、媒体类型、大小元数据，不访问附件内容。真实 AIOps 附件授权与 `AttachmentAccessPort` 推迟到二期。

一期在 Sandbox 中真实验证 `sourceSystem + hostRequestId` 幂等：相同规范化上下文的重试复用原 Session/Run，同键不同上下文返回冲突；新 ID 允许主动重新预诊。当前 v1 契约仅把 `hostRequestId` 定义为可选元数据，尚未表达完整幂等行为。

一期模拟继续提交不生成 ticketId。未来 `hostTicketId` 只作为二期 DRAFT 关联字段，由真实 AIOps 提交结果提供；Service Insight 不自行生成或冒充宿主工单标识。
