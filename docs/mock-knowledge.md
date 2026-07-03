# Mock 知识数据说明

## 目的

本文档说明第一阶段纯前端 Demo 使用的 mock 知识数据目的、来源、结构、字段、示例、使用限制和脱敏要求，确保所有示例数据均保持演示边界。

## 适用范围

适用于 `prototypes/sla-precheck-demo.html` 中的前端 mock 数据维护，以及 Demo 讲解、评审和后续样例扩展。

## 正文

### Mock 数据目的

Mock 数据用于模拟 `智能预诊` 点击后的参考资料展示效果，帮助评审者理解 SLA 提交前如何展示资料来源、前置诊断结论和建议补充信息。

Mock 数据不用于证明真实知识库质量，不代表真实 Wiki、真实 SLA、真实历史问题或真实客户现场。

### Mock 数据来源说明

当前所有 mock 数据均由项目文档人工构造，统一标注为 `模拟数据`。数据内容不来自真实客户、真实历史 SLA、真实 Wiki、真实内部系统或真实 AIOps 平台。

### Mock 数据结构

```js
const mockKnowledgeResults = [
  {
    type: "Wiki",
    title: "Foundation 审计日志归档功能说明",
    summary: "模拟数据：介绍 Foundation 审计日志归档相关配置、使用限制和常见问题。",
    url: "https://example.com/wiki/foundation-audit-archive"
  }
]
```

### Mock 数据字段说明

- `type`：资料类型，例如 Wiki、历史问题、历史 SLA、操作手册、产品文档。
- `title`：资料标题，必须为模拟标题，不得使用真实内部标题。
- `summary`：资料摘要，必须以 `模拟数据` 标注。
- `url`：示例链接，必须使用 `example.com` 示例域，不得使用真实内部链接。

### Mock 数据示例

```js
const mockKnowledgeResults = [
  {
    type: "Wiki",
    title: "Foundation 审计日志归档功能说明",
    summary: "模拟数据：介绍 Foundation 审计日志归档相关配置、使用限制和常见问题。",
    url: "https://example.com/wiki/foundation-audit-archive"
  },
  {
    type: "历史问题",
    title: "Foundation 审计日志归档按钮不可用问题处理记录",
    summary: "模拟数据：历史问题样例显示，该类现象可能与版本、权限配置或功能开关有关，不能作为当前问题最终结论。",
    url: "https://example.com/question/foundation-audit-archive-button"
  },
  {
    type: "历史 SLA",
    title: "TDS-4.0.1 Foundation 组件归档功能异常",
    summary: "模拟数据：该历史 SLA 样例记录归档功能异常的人工排查路径，实际是否适用需要处理人确认。",
    url: "https://example.com/sla/foundation-archive-case"
  },
  {
    type: "操作手册",
    title: "Foundation 组件常见问题排查手册",
    summary: "模拟数据：包含 Foundation 组件常见问题、日志路径、权限检查和配置检查方法。",
    url: "https://example.com/manual/foundation-troubleshooting"
  }
]
```

### 使用限制

- 只能用于纯前端 Demo。
- 不得被描述为真实命中结果。
- 不得包含真实客户名称。
- 不得包含真实 SLA 编号。
- 不得包含真实日志片段。
- 不得包含真实内部系统地址。
- 不得调用真实 Wiki、AIOps、SLA 或数据库接口。

### 脱敏要求

如后续需要基于真实材料制作演示数据，必须先完成脱敏和授权。脱敏至少包括：

- 替换客户名称、项目名称和集群名称。
- 移除真实 SLA 编号、工单编号和内部链接。
- 删除 IP、账号、路径、密钥、日志详情和业务敏感字段。
- 用明确的 `模拟数据` 标识替换真实来源。

### 建议输出要求

当 mock 数据用于生成建议时，必须说明：

- 依据来源：页面表单字段与前端模拟知识库数据。
- 置信度：中。
- 是否需要人工介入：需要。
- 待补充信息：真实环境日志、复现步骤、影响范围、权限与配置核查结果。
