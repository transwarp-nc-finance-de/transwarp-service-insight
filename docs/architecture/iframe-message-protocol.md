# iframe 消息协议

状态：ACTIVE（v1.0 协议骨架，尚未接真实 AIOps）。

信封字段为 `type`、`version`、`requestId`、`timestamp`、`payload`。消息包括 `TSI_INIT`、`TSI_READY`、`TSI_PRECHECK_START`、`TSI_PRECHECK_UPDATE`、`TSI_PRECHECK_RESULT`、`TSI_REQUEST_SUPPLEMENT`、`TSI_ADOPT_CHANGES`、`TSI_CONTINUE_SUBMISSION`、`TSI_REFERENCE_OPENED`、`TSI_CLOSE`、`TSI_ERROR`。

接收端校验来源白名单、协议版本、requestId 和 100KB 消息上限，并忽略重复初始化消息；初始化等待 10 秒后安全失败。业务正文不进入 query 参数，Embed 不读取宿主 DOM。错误消息只返回安全描述，任何协议或服务失败均不得阻断 AIOps 原有提交。
