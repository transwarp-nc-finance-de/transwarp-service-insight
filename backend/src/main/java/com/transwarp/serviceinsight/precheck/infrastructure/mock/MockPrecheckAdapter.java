package com.transwarp.serviceinsight.precheck.infrastructure.mock;

import com.transwarp.serviceinsight.precheck.application.ContinuePrecheckCommand;
import com.transwarp.serviceinsight.precheck.application.CreatePrecheckCommand;
import com.transwarp.serviceinsight.precheck.domain.AllowedAction;
import com.transwarp.serviceinsight.precheck.domain.ConfidenceLevel;
import com.transwarp.serviceinsight.precheck.domain.Evidence;
import com.transwarp.serviceinsight.precheck.domain.FollowUpResult;
import com.transwarp.serviceinsight.precheck.domain.NextAction;
import com.transwarp.serviceinsight.precheck.domain.PrecheckResult;
import com.transwarp.serviceinsight.precheck.domain.PrecheckStatus;
import com.transwarp.serviceinsight.precheck.dto.Confidence;
import com.transwarp.serviceinsight.precheck.dto.FollowUpRequest;
import com.transwarp.serviceinsight.precheck.dto.FollowUpResponse;
import com.transwarp.serviceinsight.precheck.dto.PrecheckRequest;
import com.transwarp.serviceinsight.precheck.dto.PrecheckResponse;
import com.transwarp.serviceinsight.precheck.dto.ReferenceItem;
import com.transwarp.serviceinsight.precheck.dto.ReferenceSourceType;
import com.transwarp.serviceinsight.precheck.port.PrecheckExecutionPort;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MockPrecheckAdapter implements PrecheckExecutionPort {
  private static final String FOLLOW_UP_FALLBACK = "模拟数据：使用无状态确定性 Mock 规则，未调用真实数据库、RAG、大模型或外部服务。";
  private static final String MODEL_VERSION = "not-applicable-deterministic-mock";
  private static final String PROMPT_VERSION = "mock-rule-v1";
  private static final String INDEX_VERSION = "not-applicable-no-index";

  public PrecheckResponse precheck(PrecheckRequest request) {
    var missing = missingInformation(request);
    var module = hasText(request.module()) ? request.module().trim() : "未明确模块";
    var references =
        List.of(
            new ReferenceItem(
                ReferenceSourceType.PRODUCT_MANUAL,
                "产品手册排查章节（模拟数据）",
                "建议核对版本、模块状态和近期变更。",
                "https://example.com/mock/product-manual",
                true),
            new ReferenceItem(
                ReferenceSourceType.HISTORICAL_SLA,
                "相似历史 SLA 摘要（模拟数据）",
                "建议先补齐时间范围与影响对象。",
                "https://example.com/mock/historical-sla",
                true));
    return new PrecheckResponse(
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        "模拟预诊：已整理 " + module + " 的辅助排查方向；这不是最终根因或处理结论。",
        List.of("请人工核对发生时间、影响范围和复现步骤。", "确认适用性后再执行只读检查或补充脱敏信息。", "由提交人确认内容后决定是否继续提交 SLA。"),
        references,
        missing.size() >= 4 ? Confidence.LOW : Confidence.MEDIUM,
        missing.isEmpty() ? "模拟字段较完整，但仍无真实证据或模型判断。" : "仍缺少 " + missing.size() + " 项可核验信息。",
        true,
        missing,
        "M1 使用确定性 Mock 规则，未调用真实 RAG、大模型或外部服务。",
        missing.isEmpty() ? "MANUAL_REVIEW_REQUIRED" : "NEED_MORE_INFORMATION",
        missing.isEmpty() ? "模拟预诊已生成，仍需人工核对后决定后续动作。" : "仍有信息需要人工补充。",
        List.of("SUPPLEMENT_INFORMATION", "CONTINUE_SUBMISSION"),
        missing.isEmpty() ? "COMPLETED" : "NEED_MORE_INFORMATION",
        "mock-policy-v1",
        MODEL_VERSION,
        PROMPT_VERSION,
        INDEX_VERSION);
  }

  @Override
  public PrecheckResult create(CreatePrecheckCommand command) {
    var response =
        precheck(
            new PrecheckRequest(
                command.title(),
                command.description(),
                command.product(),
                command.module(),
                command.version(),
                command.severity(),
                command.impactScope(),
                command.attachmentsSummary()));
    return new PrecheckResult(
        UUID.fromString(response.precheckId()),
        UUID.fromString(response.sessionId()),
        response.summary(),
        response.recommendations(),
        response.references().stream().map(this::toEvidence).toList(),
        ConfidenceLevel.valueOf(response.confidence().name()),
        response.confidenceReason(),
        response.humanReviewRequired(),
        response.missingInformation(),
        response.fallbackReason(),
        NextAction.valueOf(response.nextAction()),
        response.nextActionReason(),
        response.allowedActions().stream().map(AllowedAction::valueOf).toList(),
        PrecheckStatus.valueOf(response.status()),
        null,
        response.modelVersion(),
        response.promptVersion(),
        response.indexVersion());
  }

  public FollowUpResponse followUp(FollowUpRequest request) {
    var message = request.message().trim().toLowerCase();
    if (containsAny(message, "日志", "报错", "异常", "error", "exception")) {
      return followUpResponse(
          request,
          "模拟数据：已记录日志或报错线索，可辅助缩小排查范围，但不能据此确认最终根因。",
          "请人工核对错误发生时间、完整错误码和上下文日志。",
          Confidence.MEDIUM,
          List.of("脱敏日志时间范围", "完整错误码", "复现步骤"),
          "日志排查提示（模拟数据）");
    }
    if (containsAny(message, "权限", "授权", "认证", "账号", "用户")) {
      return followUpResponse(
          request,
          "模拟数据：已识别权限相关线索，建议人工比对主体、资源和操作范围，不代表权限即为最终根因。",
          "请由有权限的人员只读核对授权关系，避免在预诊阶段直接变更权限。",
          Confidence.MEDIUM,
          List.of("脱敏账号或角色", "目标资源", "失败操作", "近期授权变更"),
          "权限核对提示（模拟数据）");
    }
    if (containsAny(message, "影响", "范围", "全部", "部分", "节点", "任务")) {
      return followUpResponse(
          request,
          "模拟数据：已记录影响范围线索，可用于人工判断优先级和排查边界，不构成故障结论。",
          "请人工确认受影响对象数量、开始时间及是否仍在持续。",
          Confidence.MEDIUM,
          List.of("受影响对象清单", "开始时间", "当前是否持续", "未受影响对照组"),
          "影响范围分析提示（模拟数据）");
    }
    if (containsAny(message, "变更", "升级", "发布", "配置", "重启", "补丁")) {
      return followUpResponse(
          request,
          "模拟数据：已记录近期变更线索，时间相关性仅供辅助核对，不能直接认定因果关系。",
          "请人工核对变更窗口、审批记录和变更前后差异，回退操作需另行确认。",
          Confidence.MEDIUM,
          List.of("变更时间", "变更内容", "执行人及审批记录", "变更前后现象"),
          "近期变更核对提示（模拟数据）");
    }
    return followUpResponse(
        request,
        "模拟数据：当前补充信息不足，尚不能形成可靠的辅助排查方向，请继续补充可核验事实。",
        "建议优先补充脱敏日志、影响范围、权限信息或近期变更，并由人工审核。",
        Confidence.LOW,
        List.of("脱敏日志或错误码", "影响范围", "权限信息", "近期变更"),
        "信息不足提示（模拟数据）");
  }

  @Override
  public FollowUpResult continuePrecheck(ContinuePrecheckCommand command) {
    var response = followUp(new FollowUpRequest(command.precheckId(), command.message()));
    return new FollowUpResult(
        UUID.fromString(response.followUpId()),
        UUID.fromString(response.precheckId()),
        response.reply(),
        response.recommendations(),
        response.references().stream().map(this::toEvidence).toList(),
        ConfidenceLevel.valueOf(response.confidence().name()),
        response.confidenceReason(),
        response.humanReviewRequired(),
        response.missingInformation(),
        response.fallbackReason(),
        NextAction.valueOf(response.nextAction()),
        response.nextActionReason(),
        response.allowedActions().stream().map(AllowedAction::valueOf).toList(),
        PrecheckStatus.valueOf(response.status()),
        null,
        response.modelVersion(),
        response.promptVersion(),
        response.indexVersion());
  }

  private FollowUpResponse followUpResponse(
      FollowUpRequest request,
      String reply,
      String recommendation,
      Confidence confidence,
      List<String> missingInformation,
      String referenceTitle) {
    return new FollowUpResponse(
        UUID.randomUUID().toString(),
        request.precheckId().toString(),
        reply,
        List.of(recommendation),
        List.of(
            new ReferenceItem(
                ReferenceSourceType.PRODUCT_MANUAL,
                referenceTitle,
                "由本地关键词规则生成的模拟依据，仅用于验证多轮预诊交互。",
                "https://example.com/mock/follow-up",
                true)),
        confidence,
        confidence == Confidence.LOW ? "补充内容未命中可核验的模拟分类规则。" : "仅命中确定性关键词规则，未经过真实证据核验。",
        true,
        missingInformation,
        FOLLOW_UP_FALLBACK,
        confidence == Confidence.LOW ? "NEED_MORE_INFORMATION" : "MANUAL_REVIEW_REQUIRED",
        confidence == Confidence.LOW ? "当前事实不足，建议继续补充，同时始终允许人工提交。" : "已形成模拟辅助方向，仍需人工审核。",
        List.of("SUPPLEMENT_INFORMATION", "CONTINUE_SUBMISSION"),
        confidence == Confidence.LOW ? "NEED_MORE_INFORMATION" : "COMPLETED",
        "mock-policy-v1",
        MODEL_VERSION,
        PROMPT_VERSION,
        INDEX_VERSION);
  }

  private Evidence toEvidence(ReferenceItem reference) {
    return new Evidence(
        Evidence.SourceType.valueOf(reference.sourceType().name()),
        reference.title(),
        reference.excerpt(),
        reference.url(),
        reference.mockData());
  }

  private boolean containsAny(String message, String... keywords) {
    for (var keyword : keywords) {
      if (message.contains(keyword)) return true;
    }
    return false;
  }

  List<String> missingInformation(PrecheckRequest request) {
    var missing = new ArrayList<String>();
    addIfMissing(missing, request.product(), "产品");
    addIfMissing(missing, request.module(), "模块");
    addIfMissing(missing, request.version(), "版本");
    addIfMissing(missing, request.severity(), "紧急程度");
    addIfMissing(missing, request.impactScope(), "影响范围");
    addIfMissing(missing, request.attachmentsSummary(), "附件摘要（仅限模拟或脱敏内容）");
    return missing;
  }

  private void addIfMissing(List<String> missing, String value, String label) {
    if (!hasText(value)) {
      missing.add(label);
    }
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
