# API 契约草案

## 目的

本文档定义后续可试点系统可能需要的 API 契约草案，便于产品、前端、后端、RAG、LLM 和安全团队评审。

## 适用范围

适用于后续接口设计讨论。本文不提供真实服务地址、真实鉴权实现、数据库 Schema 或后端代码，不代表当前 Demo 已经实现接口。

## 正文

### `POST /api/v1/precheck`

用途：提交人工填写的 SLA 草稿字段，返回预诊建议草案。

输入字段建议：`title`、`description`、`product`、`module`、`version`、`severity`、`impact_scope`、`attachments_summary`。

输出字段建议：`precheck_id`、`summary`、`recommendations`、`references`、`confidence`、`human_review_required`、`missing_information`、`fallback_reason`。

### `POST /api/v1/retrieval/search`

用途：基于查询和过滤条件进行后续目标架构中的受控知识检索。

输入字段建议：`query`、`rewrite`、`top_k`、`filters.product`、`filters.module`、`filters.doc_type`、`filters.status`、`search_type`、`rerank`。

输出字段建议：`items.chunk_id`、`items.text`、`items.score`、`items.metadata`、`items.highlights`、`rewritten_query`、`total`、`latency_ms`。

检索必须强制限定 `status=APPROVED`，并按权限过滤资料范围。

### `POST /api/v1/feedback`

用途：记录用户对预诊结果的采纳、忽略、评分和继续提交行为。

输入字段建议：`precheck_id`、`adoption_status`、`referenced_knowledge_ids`、`latency_ms`、`rating`、`comment`。

输出字段建议：`event_id`、`status`、`received_at`。

### `POST /api/v1/knowledge/review`

用途：在后续知识管理系统中流转知识审核状态。

输入字段建议：`chunk_id`、`action`、`reason`、`operator`、`review_comment`。

输出字段建议：`chunk_id`、`previous_status`、`current_status`、`operated_at`。

### 边界

这些接口均为后续设计草案，不代表当前 Demo 已经实现。所有提交、采纳、工单创建和知识发布动作必须保留人工确认与审计。

### 知识策略与预诊响应增补草案

管理员策略对象建议包含：

```text
rag_enabled
enabled_sources.wiki
enabled_sources.historical_sla
enabled_sources.product_manual
enabled_sources.open_source
general_knowledge_fallback_enabled
policy_version
```

`POST /api/v1/precheck` 的来源范围由服务端策略和用户权限计算，客户端只能缩小查询范围，不能扩大管理员策略。响应建议增加：`answer_mode`、`configured_sources`、`effective_sources`、`matched_sources`、`policy_version` 和 `fallback_reason`。

`configured_sources` 表示管理员已开启来源，`effective_sources` 表示叠加权限、状态和查询过滤后的范围，`matched_sources` 仅表示本次真实命中的来源。附件请求只传经脱敏和授权的附件引用或摘要；审计不得保存未脱敏附件正文。

以上仅为后续 API 设计草案，当前纯前端 Demo 未实现或调用任何接口。

