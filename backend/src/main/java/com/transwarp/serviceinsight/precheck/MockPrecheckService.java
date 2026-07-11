package com.transwarp.serviceinsight.precheck;

import com.transwarp.serviceinsight.precheck.dto.Confidence;
import com.transwarp.serviceinsight.precheck.dto.PrecheckRequest;
import com.transwarp.serviceinsight.precheck.dto.PrecheckResponse;
import com.transwarp.serviceinsight.precheck.dto.ReferenceItem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MockPrecheckService {
  public PrecheckResponse precheck(PrecheckRequest request) {
    var missing = missingInformation(request);
    var module = hasText(request.module()) ? request.module().trim() : "未明确模块";
    var references =
        List.of(
            new ReferenceItem(
                "PRODUCT_MANUAL",
                "产品手册排查章节（模拟数据）",
                "建议核对版本、模块状态和近期变更。",
                "https://example.com/mock/product-manual",
                true),
            new ReferenceItem(
                "HISTORICAL_SLA",
                "相似历史 SLA 摘要（模拟数据）",
                "建议先补齐时间范围与影响对象。",
                "https://example.com/mock/historical-sla",
                true));
    return new PrecheckResponse(
        UUID.randomUUID().toString(),
        "模拟预诊：已整理 " + module + " 的辅助排查方向；这不是最终根因或处理结论。",
        List.of(
            "请人工核对发生时间、影响范围和复现步骤。",
            "确认适用性后再执行只读检查或补充脱敏信息。",
            "由提交人确认内容后决定是否继续提交 SLA。"),
        references,
        missing.size() >= 4 ? Confidence.LOW : Confidence.MEDIUM,
        true,
        missing,
        "M1 使用确定性 Mock 规则，未调用真实 RAG、大模型或外部服务。");
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
