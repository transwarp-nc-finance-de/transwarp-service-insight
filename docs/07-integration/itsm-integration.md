# ITSM 集成设计

## 目的

本文档描述后续目标架构中 SLA 智能预诊与 ITSM / 工单创建流程的集成方式。

## 适用范围

适用于 ITSM 集成评审和产品接口讨论。当前 Demo 不接真实 ITSM。

## 正文

目标集成方式可包括工单创建页嵌入预诊面板、展示预诊结果、保存人工确认后的预诊摘要和采纳状态。

字段建议：`pre_diagnosis_id`、`adoption_status`、`referenced_knowledge_ids`、`precheck_summary`、`ai_assist_trace_id`。

AI 能力不可用时不得影响人工提交。预诊结果只作为辅助建议，不得自动提交 SLA、自动闭单或自动判责。

