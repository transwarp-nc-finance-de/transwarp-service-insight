package com.transwarp.serviceinsight.precheck.v2.application;

import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalModels.RetrievalOutcome;
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

  public PrecheckResult evaluate(
      PrecheckContext context,
      CompletenessPolicy policy,
      int sequence,
      RetrievalOutcome retrieval) {
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
    var confidence = confidence(missing, retrieval);
    return new PrecheckResult(
        summary(missing, retrieval),
        recommendations(retrieval),
        completeness,
        confidence.level(),
        confidence.reasons(),
        retrieval.evidence().stream()
            .map(
                evidence ->
                    java.util.Map.<String, Object>of(
                        "evidenceId", evidence.evidenceId(),
                        "title", evidence.title(),
                        "excerpt", evidence.excerpt(),
                        "mockData", true))
            .toList(),
        List.of("建议人工核对完整错误码、复现步骤和近期变更；用户仍可继续提交。"),
        missing,
        actions,
        DISCLAIMER,
        new RetrievalDegradation(
            retrieval.mode(),
            new CapabilityStatus(
                retrieval.fts().available(), retrieval.fts().code(), retrieval.fts().message()),
            new CapabilityStatus(
                retrieval.embedding().available(),
                retrieval.embedding().code(),
                retrieval.embedding().message()),
            retrieval.degraded()),
        true);
  }

  private Confidence confidence(List<String> missing, RetrievalOutcome retrieval) {
    if (!missing.isEmpty()) {
      return new Confidence("LOW", List.of("仍有待补充信息，确定性规则将置信度限制为 LOW。"));
    }
    if ("UNAVAILABLE".equals(retrieval.mode()) || retrieval.evidence().isEmpty()) {
      return new Confidence("LOW", List.of("无可用 Evidence，确定性规则将置信度限制为 LOW。"));
    }
    if ("FTS_ONLY".equals(retrieval.mode())) {
      return new Confidence("LOW", List.of("向量召回不可用，FTS_ONLY 降级强制使用 LOW。"));
    }
    var crossConfirmed =
        retrieval.fusion().selected().stream()
            .filter(item -> item.ftsRank() != null && item.vectorRank() != null)
            .count();
    if (retrieval.evidence().size() >= 2 && crossConfirmed >= 2) {
      return new Confidence("HIGH", List.of("信息完整且至少两条 Evidence 由 FTS 与向量召回共同印证。"));
    }
    return new Confidence("MEDIUM", List.of("存在可用 Evidence，但双路共同印证仍有限。"));
  }

  private String summary(List<String> missing, RetrievalOutcome retrieval) {
    if ("UNAVAILABLE".equals(retrieval.mode())) {
      return "模拟数据：检索暂时不可用，请人工核对上下文后继续处理。";
    }
    if (retrieval.evidence().isEmpty()) {
      return "模拟数据：未找到当前权限范围内的可用依据，请人工继续核对。";
    }
    return missing.isEmpty()
        ? "模拟数据：已基于当前授权范围内的 Evidence 生成确定性预诊摘要。"
        : "模拟数据：已找到授权 Evidence，但当前信息仍建议补充。";
  }

  private List<String> recommendations(RetrievalOutcome retrieval) {
    if (retrieval.evidence().isEmpty()) {
      return List.of("请由 SLA 处理人核对完整错误码、复现步骤和近期变更后决定下一步。");
    }
    return List.of("请人工打开并核验列出的 Evidence，再决定排查方向；建议不构成最终方案。");
  }

  private record Confidence(String level, List<String> reasons) {}

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
