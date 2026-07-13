# AIOps 宿主集成

状态：ACTIVE（协议为本地 PROTOTYPE）。第一期推荐 iframe + postMessage，以隔离未知的 AIOps 前端技术栈；Native API 和 Web Component 作为后续候选。

当前 `/sandbox` 模拟宿主，`/embed` 是不包含 SLA 表单的嵌入面板。真实来源通过 `VITE_AIOPS_ALLOWED_ORIGINS` 配置，不硬编码生产域名。Embed 不读取宿主 DOM，不通过 URL 传表单内容，附件只接收元数据或受控引用。

接入前需由 AIOps 团队确认：Vue/React 及版本、是否允许改前端和 iframe、CSP/X-Frame-Options、稳定字段编码、富文本转纯文本方式、附件引用、登录鉴权、测试环境、API Gateway、浏览器范围和发布流程。
