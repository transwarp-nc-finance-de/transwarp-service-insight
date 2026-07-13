# 嵌入式预诊 UI

状态：ACTIVE。

- Sandbox：模拟 AIOps 表单、失败和宿主事件，仅供开发、联调、演示和自动化测试。
- Embed：只接收宿主快照并展示预诊过程与结果，不复制正式 SLA 表单。
- PrecheckPanel：复用摘要、完整度、置信度原因、待补充信息、建议、引用、降级和继续提交入口。
- HostBridge：核心组件与宿主通信的唯一抽象，当前有 Sandbox 和 postMessage 两种实现。

普通模式隐藏 policy/prompt/model/index/trace，Sandbox 调试模式可显示技术详情。反馈记录与继续提交是两个独立动作；反馈失败不影响宿主继续提交。
