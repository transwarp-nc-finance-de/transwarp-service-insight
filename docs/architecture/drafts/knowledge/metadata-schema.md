# 知识元数据草案

## 目的

本文档定义后续知识 Chunk 的元数据草案，便于检索过滤、权限控制和审计追溯。

## 适用范围

适用于知识库 Schema 评审和 RAG 检索设计。当前阶段不创建真实数据库 Schema。

## 正文

字段草案：

| 字段 | 说明 |
| --- | --- |
| `chunk_id` | 知识片段标识 |
| `doc_id` | 源文档标识 |
| `title` | 标题 |
| `product` | 产品线 |
| `module` | 组件模块 |
| `doc_type` | 文档类型 |
| `source` | 来源类型 |
| `version` | 文档版本 |
| `status` | 知识状态 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |
| `expired_at` | 过期时间 |
| `owner` | 负责人 |
| `review_status` | 审核状态 |
| `sensitivity_level` | 敏感级别 |
| `permission_scope` | 权限范围 |

真实 Schema 需后端、数据和安全团队评审后确定。

