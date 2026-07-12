# ADR-0004：OpenAPI 是已实现接口唯一契约

Status: ACCEPTED  
Owner: 后端负责人  
Last reviewed: 2026-07-12

## 决策

`docs/api/openapi.yaml` 是已实现 HTTP API 的唯一契约。前后端、测试、示例和错误结构必须与其一致；未来接口只可进入明确标记的 DRAFT 文档。

## 影响

新增兼容字段需同步契约与消费者测试。删除、改名、类型变化或语义收紧属于破坏性变化，必须人工确认、版本化并提供迁移计划。
