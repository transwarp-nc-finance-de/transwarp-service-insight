# 审计设计

## 目的

本文档定义后续预诊系统的审计对象、字段草案和保留边界。

## 适用范围

适用于安全合规评审、ITSM / AIOps 集成评审和知识管理评审。当前阶段不产生日志或审计数据。

## 正文

审计对象包括用户请求、查询改写、检索结果、生成输出、降级事件、采纳行为、人工确认、知识状态变更和安全拦截。

字段草案：`trace_id`、`user_id`、`action`、`input_summary`、`source_ids`、`confidence`、`guardrail_status`、`fallback_reason`、`operator`、`created_at`。

审计日志保存周期、访问权限和脱敏规则需安全团队后续评审。审计数据不得用于自动判责。

