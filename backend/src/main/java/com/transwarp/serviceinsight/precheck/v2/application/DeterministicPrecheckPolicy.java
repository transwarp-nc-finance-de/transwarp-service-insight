package com.transwarp.serviceinsight.precheck.v2.application;

import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.CapabilityStatus;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.CompletenessAssessment;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.CompletenessPolicy;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckContext;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckResult;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.RetrievalDegradation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class DeterministicPrecheckPolicy {
  private static final int MAX_RUNS = 3;
  private static final String DISCLAIMER = "本结果不是最终根因、最终方案或正式复盘结论，须由人工确认。";

  public PrecheckResult evaluate(PrecheckContext context, CompletenessPolicy policy, int sequence) {
    var missing = missing(context, policy);
    var completeness =
        new CompletenessAssessment(
            missing.isEmpty(),
            policy.policyVersion(),
            missing,
            missing.isEmpty()
                ? List.of("模拟数据：当前版本完整度策略未发现缺失项。")
                : List.of("模拟数据：当前版本完整度策略建议补充列出的字段。"));
    var actions = new ArrayList<String>();
    if (!missing.isEmpty() && sequence < MAX_RUNS) actions.add("SUPPLEMENT_INFORMATION");
    actions.add("CONTINUE_SUBMISSION");
    actions.add("CONFIRM_SELF_SERVICE");
    return new PrecheckResult(
        missing.isEmpty() ? "模拟数据：信息完整度检查通过，但检索能力尚未实现。" : "模拟数据：当前信息仍可补充，以下内容仅供人工参考。",
        List.of("请由 SLA 处理人核对上下文和脱敏信息后决定下一步。"),
        completeness,
        "LOW",
        List.of("当前未实现 FTS、向量检索或真实 Evidence，置信度按确定性规则固定为 LOW。"),
        List.of(),
        List.of("建议人工核对完整错误码、复现步骤和近期变更；用户仍可继续提交。"),
        missing,
        actions,
        DISCLAIMER,
        new RetrievalDegradation(
            "UNAVAILABLE",
            new CapabilityStatus(false, "FTS_NOT_IMPLEMENTED", "模拟数据：FTS 尚未实现"),
            new CapabilityStatus(false, "EMBEDDING_NOT_IMPLEMENTED", "模拟数据：local-embedding 尚未接入"),
            true),
        true);
  }

  private List<String> missing(PrecheckContext context, CompletenessPolicy policy) {
    var values = new LinkedHashSet<String>();
    var supplied = new LinkedHashSet<String>();
    context.additionalInformation().forEach(item -> supplied.add(item.fieldCode()));
    policy.generalFieldCodes().stream()
        .filter(code -> !present(context, supplied, code))
        .forEach(values::add);
    policy.issueSpecificFieldCodes().stream()
        .filter(code -> !present(context, supplied, code))
        .forEach(values::add);
    return List.copyOf(values);
  }

  private boolean present(PrecheckContext context, LinkedHashSet<String> supplied, String code) {
    return switch (code) {
      case "ISSUE_TYPE" -> context.issueType() != null;
      case "PRODUCT_LINE" -> context.productLine() != null;
      case "PRODUCT" -> context.product() != null;
      case "COMPONENT" -> context.component() != null;
      case "VERSION" -> !blank(context.version());
      case "SEVERITY" -> context.severity() != null;
      case "SERVICE_TYPE" -> context.serviceType() != null;
      case "TITLE" -> !blank(context.title());
      case "DESCRIPTION" -> !blank(context.descriptionPlainText());
      case "IMPACT_SCOPE" -> !blank(context.impactScope());
      default -> supplied.contains(code);
    };
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
