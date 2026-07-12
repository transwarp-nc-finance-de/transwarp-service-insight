# ADR-0001：模块化单体优先

Status: ACCEPTED  
Owner: 架构负责人  
Last reviewed: 2026-07-12

## 决策

当前保持一个 Spring Boot 应用和一个 Vue 应用，按业务模块组织。模块内部使用 application、domain、port、infrastructure 边界，不因未来可能拆分而提前引入微服务。

## 原因与影响

业务和容量尚未证明微服务收益，模块化单体更利于事务、测试、调试和本地交付。未来仅在团队发布、权限、容量或故障隔离有明确证据时拆分；在线与离线链路即使同进程也必须保持模块边界。
