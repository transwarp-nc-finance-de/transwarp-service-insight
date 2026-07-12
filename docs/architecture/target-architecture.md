# 目标架构

Status: DRAFT  
Owner: 架构负责人  
Last reviewed: 2026-07-12  
Source of truth for: 未批准的长期架构方向

目标采用模块化单体与 Workflow-first，不把微服务或多 Agent 作为基础依赖。

```text
预诊助手 / 知识治理 / 运营评估
              ↓
API 与应用用例
              ↓
确定性工作流、状态机与策略
              ↓
precheck / knowledge / retrieval / generation / feedback / policy / audit
              ↓
Port → 可替换 Adapter → 数据、模型及外部系统
```

在线预诊要求低延迟、只读、权限先于检索、可降级且永不阻断人工提交。离线知识处理负责导入、解析、审核、发布、索引、失败重试和版本管理，两条链路彻底分离。

未来可替换 Adapter 包括关键词/向量检索、生成网关、持久化、身份策略、审计与 AIOps/ITSM。任何真实数据、真实模型、外部服务、生产访问或自动提交均需另行人工批准。受控 Agent 仅在固定工具、轮次、预算、超时和完整回放下实验，且必须证明优于单工作流。
