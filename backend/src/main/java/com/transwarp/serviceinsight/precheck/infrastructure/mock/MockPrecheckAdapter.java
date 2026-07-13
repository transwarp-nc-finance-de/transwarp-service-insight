package com.transwarp.serviceinsight.precheck.infrastructure.mock;

import com.transwarp.serviceinsight.precheck.application.CreatePrecheckCommand;
import com.transwarp.serviceinsight.precheck.domain.Evidence;
import com.transwarp.serviceinsight.precheck.port.CompletenessAssessmentPort;
import com.transwarp.serviceinsight.precheck.port.EvidenceVerificationPort;
import com.transwarp.serviceinsight.precheck.port.KnowledgeSearchPort;
import com.transwarp.serviceinsight.precheck.port.SuggestionGenerationPort;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MockPrecheckAdapter
    implements CompletenessAssessmentPort,
        KnowledgeSearchPort,
        SuggestionGenerationPort,
        EvidenceVerificationPort {
  @Override
  public List<String> missingInformation(CreatePrecheckCommand command) {
    var missing = new ArrayList<String>();
    add(missing, command.product(), "产品");
    add(missing, command.module(), "模块");
    add(missing, command.version(), "版本");
    add(missing, command.severity(), "紧急程度");
    add(missing, command.impactScope(), "影响范围");
    add(missing, command.attachmentsSummary(), "附件摘要（仅限模拟或脱敏内容）");
    return missing;
  }

  @Override
  public List<Evidence> search(String query) {
    return List.of(
        new Evidence(
            Evidence.SourceType.PRODUCT_MANUAL,
            "产品手册排查章节（模拟数据）",
            "建议核对版本、模块状态和近期变更。",
            "https://example.com/mock/product-manual",
            true),
        new Evidence(
            Evidence.SourceType.HISTORICAL_SLA,
            "相似历史 SLA 摘要（模拟数据）",
            "建议先补齐时间范围与影响对象。",
            "https://example.com/mock/historical-sla",
            true));
  }

  @Override
  public List<String> generate(CreatePrecheckCommand command, List<String> missing) {
    return List.of("请人工核对发生时间、影响范围和复现步骤。", "确认适用性后再执行只读检查或补充脱敏信息。", "由提交人确认内容后决定是否继续提交 SLA。");
  }

  @Override
  public FollowUpSuggestion followUp(String message) {
    var text = message.trim().toLowerCase();
    if (contains(text, "日志", "报错", "异常", "error", "exception"))
      return suggestion(
          "已记录日志或报错线索，可辅助缩小排查范围，但不能据此确认最终根因。",
          "请人工核对错误发生时间、完整错误码和上下文日志。",
          List.of("脱敏日志时间范围", "完整错误码", "复现步骤"),
          "日志排查提示（模拟数据）");
    if (contains(text, "权限", "授权", "认证", "账号", "用户"))
      return suggestion(
          "已识别权限相关线索，建议人工比对主体、资源和操作范围，不代表权限即为最终根因。",
          "请由有权限的人员只读核对授权关系。",
          List.of("脱敏账号或角色", "目标资源", "失败操作", "近期授权变更"),
          "权限核对提示（模拟数据）");
    if (contains(text, "影响", "范围", "全部", "部分", "节点", "任务"))
      return suggestion(
          "已记录影响范围线索，可用于人工判断优先级和排查边界，不构成故障结论。",
          "请人工确认受影响对象数量、开始时间及是否仍在持续。",
          List.of("受影响对象清单", "开始时间", "当前是否持续", "未受影响对照组"),
          "影响范围分析提示（模拟数据）");
    if (contains(text, "变更", "升级", "发布", "配置", "重启", "补丁"))
      return suggestion(
          "已记录近期变更线索，时间相关性仅供辅助核对，不能直接认定因果关系。",
          "请人工核对变更窗口、审批记录和变更前后差异。",
          List.of("变更时间", "变更内容", "执行人及审批记录", "变更前后现象"),
          "近期变更核对提示（模拟数据）");
    return new FollowUpSuggestion(
        "模拟数据：当前补充信息不足，尚不能形成可靠的辅助排查方向。",
        List.of("建议优先补充脱敏日志、影响范围、权限信息或近期变更，并由人工审核。"),
        List.of("脱敏日志或错误码", "影响范围", "权限信息", "近期变更"),
        false,
        "信息不足提示（模拟数据）");
  }

  @Override
  public List<Evidence> verify(List<Evidence> evidence) {
    return evidence.stream().filter(Evidence::mockData).toList();
  }

  private FollowUpSuggestion suggestion(
      String reply, String recommendation, List<String> missing, String title) {
    return new FollowUpSuggestion("模拟数据：" + reply, List.of(recommendation), missing, true, title);
  }

  private void add(List<String> missing, String value, String label) {
    if (value == null || value.isBlank()) missing.add(label);
  }

  private boolean contains(String value, String... words) {
    for (var word : words) if (value.contains(word)) return true;
    return false;
  }
}
